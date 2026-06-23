package com.example.springia.agent.loop;

import com.example.springia.agent.tool.Tool;
import com.example.springia.agent.tool.ToolRegistry;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.utils.LogUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementa o Agent Loop com padrão ReAct (Reasoning + Acting)
 * Fluxo:
 * 1. Recebe input
 * 2. LLM pensa (Reasoning)
 * 3. LLM decide ação (Acting) - qual tool usar
 * 4. Executa tool e observa resultado
 * 5. Repete até chegar a Finalizar
 *
 * <pre>
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.loop.AgentLoop" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 * </pre>
 */
@Slf4j
public class AgentLoop {

    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final int maxSteps;
    private final ObjectMapper objectMapper;
    private final AgentFinalizationGate finalizationGate;

    public AgentLoop(ChatClient chatClient, ToolRegistry toolRegistry, int maxSteps) {
        this(chatClient, toolRegistry, maxSteps, new AgentFinalizationGate());
    }

    public AgentLoop(ChatClient chatClient, ToolRegistry toolRegistry, int maxSteps, AgentFinalizationGate finalizationGate) {
        this.chatClient = chatClient;
        this.toolRegistry = toolRegistry;
        this.maxSteps = maxSteps;
        this.objectMapper = new ObjectMapper();
        this.finalizationGate = finalizationGate;
    }

    /**
     * Executa o agent loop com o input fornecido
     */
    public AgentExecution execute(String input) throws Exception {
        return execute(input, null);
    }

    /**
     * Executa o agent loop com contexto de projeto para validar build/test no gate final.
     */
    public AgentExecution execute(String input, Project project) throws Exception {
        String executionId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();

        AgentExecution execution = AgentExecution.builder()
                .executionId(executionId)
                .input(input)
                .steps(new ArrayList<>())
                .startTime(startTime)
                .status("RUNNING")
                .build();

        log.info("[AGENT] Iniciando execução id={} input_length={}", executionId, input.length());

        try {
            String context = buildInitialContext(input);
            int stepCount = 0;
            String lastActionSignature = null;
            int repeatedActionCount = 0;

            while (stepCount < maxSteps) {
                stepCount++;
                log.debug("[AGENT] Passo {} de {}", stepCount, maxSteps);
                //log.debug("[AGENT] Prompt: {}", context);

                // Chamada ao LLM para decidir ação
                String llmResponse = callLLM(context);
                log.debug("[AGENT] LLM Response (passo {}): {}", stepCount, llmResponse);

                // Parse da resposta
                AgentStep step = parseAgentResponse(llmResponse, stepCount);
                execution.addStep(step);

                // Se é final, encerra
                if (step.isFinal()) {
                    List<CodeRepo> repos = project.getRepos();
                    AgentFinalizationGate.GateResult gateResult = finalizationGate.validate(project);
                    log.info("[AGENT] Gate de finalizacao - finalizado a validação. Resultado: {}", gateResult.passed()?"Aprovado":"Reprovado");
                    if (!gateResult.passed()) {
                        String gateFeedback = "Gate de finalizacao reprovado. Corrija o codigo com base nos logs abaixo e so entao finalize novamente.\n\n" + gateResult.report();
                        step.setObservation("Finalizacao bloqueada por falha no gate de build/test via Docker.");
                        step.setToolResult(gateFeedback);
                        step.setFinal(false);
                        step.setFinalAnswer(null);
                        log.warn("[AGENT] Gate de finalizacao reprovado no passo {}", stepCount);

                        context = updateContext(context, step);

                        log.warn("[AGENT] Arquivo log: {}", LogUtils.saveLog(context));

                        continue;
                    }

                    step.setObservation("Finalizacao aprovada no gate de build/test via Docker.");
                    step.setToolResult(gateResult.report());
                    log.debug("[AGENT] Agent finalizou no passo {}", stepCount);
                    execution.setFinalAnswer(step.getFinalAnswer());
                    execution.setStatus("SUCCESS");
                    break;
                }

                String actionSignature = buildActionSignature(step);
                if (actionSignature.equals(lastActionSignature)) {
                    repeatedActionCount++;
                } else {
                    repeatedActionCount = 1;
                    lastActionSignature = actionSignature;
                }

                if (repeatedActionCount >= 4) {
                    String loopError = "Loop detectado: a mesma ação foi repetida " + repeatedActionCount
                            + " vezes seguidas. Use update_file para alterar apenas linhas específicas e então finalize.";
                    step.setToolResult("ERRO: " + loopError);
                    execution.setStatus("ERROR");
                    execution.setErrorMessage(loopError);
                    log.warn("[AGENT] {}", loopError);
                    break;
                }

                // Executa a ferramenta
                if (step.getToolName() != null && !step.getToolName().isBlank()) {
                    String toolResult = executeToolCalls(llmResponse, step);
                    step.setToolResult(toolResult);
                    log.debug("[AGENT] Tool result (passo {}): {}", stepCount, toolResult.length() > 500 ? toolResult.substring(0, 500) + "..." : toolResult);
                }

                // Atualiza contexto para próxima iteração
                context = updateContext(context, step);
            }

            if (stepCount >= maxSteps && execution.getStatus().equals("RUNNING")) {
                execution.setStatus("TIMEOUT");
                execution.setErrorMessage("Máximo de passos (" + maxSteps + ") atingido");
                log.warn("[AGENT] Execução atingiu limite de passos");
            }

        } catch (Exception e) {
            log.error("[AGENT] Erro durante execução", e);
            execution.setStatus("ERROR");
            execution.setErrorMessage(e.getMessage());
        }

        LocalDateTime endTime = LocalDateTime.now();
        execution.setEndTime(endTime);
        execution.setTotalExecutionTimeMs(
            java.time.temporal.ChronoUnit.MILLIS.between(startTime, endTime)
        );

        log.info("[AGENT] Execução finalizada id={} status={} passos={} tempo={}ms",
                executionId, execution.getStatus(), execution.getStepCount(),
                execution.getTotalExecutionTimeMs());

        return execution;
    }

    /**
     * Constrói o contexto inicial para o primeiro prompt ao LLM
     */
    private String buildInitialContext(String input) {
        return """
        Você é um especialista em engenharia de software que segue o padrão ReAct (Reasoning + Acting).
        
        TAREFA: Analisar e executar as tarefas descritas no código/especificação fornecida.
        
        TOOLS DISPONÍVEIS:
        %s
        
        INSTRUÇÕES:
        1. ANTES de executar qualquer ação, PENSE sobre o que precisa ser feito e verifique no código existente para antes de criar algo novo. Use ferramenta (tool) discovery_tool para isso.
        2. Use UMA ferramenta (tool) para executar a ação
        3. Se o arquivo já existe, use update_file para alterar somente as linhas necessárias.
        3.1. ANTES de usar update_file, use read_file no mesmo arquivo e copie o old_text literalmente da saída.
        3.2. Nunca invente comentários/trechos para old_text; use apenas texto que exista no arquivo.
        4. Use create_file APENAS para arquivo novo. Nunca use create_file para sobrescrever arquivo existente.
        5. AGUARDE a observação do resultado antes de prosseguir
        6. DECIDA se precisa de mais ações ou se pode FINALIZAR
        7. Quando terminar, responda com "Finalizar: [resposta_final]"
        
        !! CRÍTICO: Responda com APENAS UMA ação por vez.
        !! Nunca inclua múltiplas ações na mesma resposta.
        !! Nunca coloque "Finalizar:" na mesma resposta que uma "Ação:".
        !! Execute uma ferramenta, aguarde o resultado, depois decida o próximo passo.
        
        FORMATO DE RESPOSTA (para executar uma ação):
        Pensamento: [Seu raciocínio sobre o que fazer]
        Ação: [Nome da tool]
        Parâmetros: [JSON com os parâmetros da tool]
        
        FORMATO DE RESPOSTA (quando terminar todas as ações):
        Pensamento: [Análise final]
        Finalizar: [Resposta resumida do que foi feito]
        
        ============================================
        ENTRADA:
        %s
        """.formatted(
            toolRegistry.getToolsDescription(),
            input
        );
    }

    /**
     * Chama o LLM para decidir a próxima ação
     */
    private String callLLM(String context) {
        return chatClient.prompt()
                .user(context)
                .call()
                .content();
    }

    /**
     * Parse da resposta do LLM para extrair thinking, tool, params, etc
     */
    private AgentStep parseAgentResponse(String response, int stepNumber) {
        AgentStep.AgentStepBuilder builder = AgentStep.builder()
                .stepNumber(stepNumber);

        // Extrai Pensamento
        String thinking = extractSection(response, "Pensamento:");
        builder.thinking(thinking);

        // Tool calls têm prioridade: se o LLM incluiu ações na resposta,
        // execute-as primeiro. "Finalizar:" só é processado quando não há
        // nenhuma ação pendente, evitando que o agent finalize sem executar
        // as ferramentas que listou.
        List<ToolCall> toolCalls = extractToolCalls(response);
        if (!toolCalls.isEmpty()) {
            ToolCall first = toolCalls.getFirst();
            builder.toolName(first.name());
            builder.toolParams(first.params());
            return builder.build();
        }

        // Só considera finalização quando não há tool calls na resposta.
        String finalAnswer = extractFinalAnswer(response);
        if (finalAnswer != null && !finalAnswer.isBlank()) {
            builder.isFinal(true);
            builder.finalAnswer(finalAnswer);
            return builder.build();
        }

        // Fallback para manter compatibilidade com respostas fora do formato ideal.
        String action = extractSection(response, "Ação:");
        builder.toolName(action);
        String paramsStr = extractSection(response, "Parâmetros:");
        Map<String, String> params = parseParameters(paramsStr);
        builder.toolParams(params);

        return builder.build();
    }

    private String extractFinalAnswer(String response) {
        Pattern finalPattern = Pattern.compile("(?m)^\\s*Finalizar:\\s*(.+)$");
        Matcher matcher = finalPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private List<ToolCall> extractToolCalls(String response) {
        List<ToolCall> calls = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "(?ms)^\\s*Ação:\\s*([^\\n]+?)\\s*$\\s*^\\s*Parâmetros:\\s*(.*?)(?=^\\s*Ação:|^\\s*Finalizar:|\\z)"
        );
        Matcher matcher = pattern.matcher(response);
        while (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String paramsStr = matcher.group(2).trim();
            calls.add(new ToolCall(toolName, parseParameters(paramsStr)));
        }
        return calls;
    }

    private String executeToolCalls(String llmResponse, AgentStep parsedStep) {
        List<ToolCall> toolCalls = extractToolCalls(llmResponse);
        if (toolCalls.isEmpty()) {
            return executeSingleTool(parsedStep.getToolName(), parsedStep.getToolParams());
        }

        StringBuilder aggregated = new StringBuilder();
        for (int i = 0; i < toolCalls.size(); i++) {
            ToolCall call = toolCalls.get(i);
            String result = executeSingleTool(call.name(), call.params());
            if (i > 0) {
                aggregated.append("\n");
            }
            aggregated.append("[")
                    .append(i + 1)
                    .append("/")
                    .append(toolCalls.size())
                    .append("] ")
                    .append(call.name())
                    .append(": ")
                    .append(result);
        }

        if (toolCalls.size() > 1) {
            parsedStep.setToolName(toolCalls.getFirst().name() + " (+" + (toolCalls.size() - 1) + " ações)");
            parsedStep.setToolParams(toolCalls.getFirst().params());
        }

        return aggregated.toString();
    }

    /**
     * Extrai uma seção da resposta do LLM
     */
    private String extractSection(String response, String sectionName) {
        Pattern pattern = Pattern.compile(Pattern.quote(sectionName) + "\\s*([^\\n]*?)(?=\\n|$)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String normalizeParamsBlock(String paramsStr) {
        if (paramsStr == null) {
            return "";
        }

        String normalized = paramsStr.trim();

        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("(?s)^```(?:json)?\\s*", "");
            normalized = normalized.replaceFirst("(?s)\\s*```$", "");
        }

        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start >= 0 && end > start) {
            normalized = normalized.substring(start, end + 1);
        }

        // Alguns modelos retornam concatenação estilo Java/JS ("..." + "...")
        // dentro do JSON de parâmetros; remove o operador para recuperar JSON válido.
        normalized = normalized.replaceAll("\"\\s*\\+\\s*\"", "");

        return normalized.trim();
    }

    /**
     * Parse dos parâmetros JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> parseParameters(String paramsStr) {
        try {
            String normalizedParamsStr = normalizeParamsBlock(paramsStr);
            if (normalizedParamsStr.isBlank()) {
                return new HashMap<>();
            }

            Map<String, Object> rawParams = objectMapper.readValue(normalizedParamsStr, Map.class);
            Map<String, String> normalizedParams = new HashMap<>();

            // Tools recebem Map<String, String>; converte valores JSON primitivos para texto.
            for (Map.Entry<String, Object> entry : rawParams.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                normalizedParams.put(key, value == null ? "" : String.valueOf(value));
            }

            return normalizedParams;
        } catch (Exception e) {
            log.warn("[AGENT] Erro ao fazer parse de parâmetros: {}", paramsStr, e);
            return new HashMap<>();
        }
    }

    /**
     * Executa uma ferramenta
     */
    private String executeSingleTool(String toolName, Map<String, String> params) {
        Tool tool = toolRegistry.getTool(toolName);

        if (tool == null) {
            String error = "Tool não encontrada: " + toolName;
            log.error("[AGENT] {}", error);
            return "ERRO: " + error;
        }

        try {
            log.info("[AGENT] Executando tool: {} com {} parâmetros", toolName, params.size());
            Set<Map.Entry<String, String>> entries = params.entrySet();
            for (Map.Entry<String, String> entry : entries)
                log.info("        {} > {}", entry.getKey(), entry.getValue());

            String result = tool.execute(params);
            log.debug("[AGENT] Tool {} executada com sucesso", toolName);
            return result;
        } catch (Exception e) {
            String error = "Erro ao executar tool " + toolName + ": " + e.getMessage();
            log.error("[AGENT] {}", error, e);
            return "ERRO: " + error;
        }
    }

    private String buildActionSignature(AgentStep step) {
        if (step.getToolName() == null) {
            return "";
        }

        String params = step.getToolParams() == null ? "{}" : new TreeMap<>(step.getToolParams()).toString();
        return step.getToolName() + "|" + params;
    }

    /**
     * Atualiza o contexto para a próxima iteração incluindo resultado
     */
    private String updateContext(String baseContext, AgentStep step) {
        return baseContext + "\n\n" +
            "Pensamento do agent: " + step.getThinking() + "\n" +
            "Tool executada: " + step.getToolName() + "\n" +
            "Resultado:\n" + step.getToolResult() + "\n" +
            "---\n" +
            "Qual é o próximo passo? (Se tudo está pronto, responda com 'Finalizar: ...')";
    }

    private record ToolCall(String name, Map<String, String> params) {}
}



