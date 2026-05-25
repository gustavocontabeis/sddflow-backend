package com.example.springia.agent.loop;

import com.example.springia.agent.tool.Tool;
import com.example.springia.agent.tool.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementa o Agent Loop com padrão ReAct (Reasoning + Acting)
 *
 * Fluxo:
 * 1. Recebe input
 * 2. LLM pensa (Reasoning)
 * 3. LLM decide ação (Acting) - qual tool usar
 * 4. Executa tool e observa resultado
 * 5. Repete até chegar a Finalizar
 */
@Slf4j
public class AgentLoop {

    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final int maxSteps;
    private final ObjectMapper objectMapper;

    public AgentLoop(ChatClient chatClient, ToolRegistry toolRegistry, int maxSteps) {
        this.chatClient = chatClient;
        this.toolRegistry = toolRegistry;
        this.maxSteps = maxSteps;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Executa o agent loop com o input fornecido
     */
    public AgentExecution execute(String input) throws Exception {
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

            while (stepCount < maxSteps) {
                stepCount++;
                log.info("[AGENT] Passo {} de {}", stepCount, maxSteps);

                // Chamada ao LLM para decidir ação
                String llmResponse = callLLM(context);
                log.debug("[AGENT] LLM Response (passo {}): {}", stepCount, llmResponse);

                // Parse da resposta
                AgentStep step = parseAgentResponse(llmResponse, stepCount);
                execution.addStep(step);

                // Se é final, encerra
                if (step.isFinal()) {
                    log.info("[AGENT] Agent finalizou no passo {}", stepCount);
                    execution.setFinalAnswer(step.getFinalAnswer());
                    execution.setStatus("SUCCESS");
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
        1. ANTES de executar qualquer ação, PENSE sobre o que precisa ser feito
        2. Use UMA ferramenta (tool) para executar a ação
        3. AGUARDE a observação do resultado antes de prosseguir
        4. DECIDA se precisa de mais ações ou se pode FINALIZAR
        5. Quando terminar, responda com "Finalizar: [resposta_final]"
        
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
                "(?ms)^\\s*Ação:\\s*([^\\n]+?)\\s*$\\s*^\\s*Parâmetros:\\s*(\\{.*?})(?=\\s*^\\s*Ação:|\\s*^\\s*Finalizar:|$)"
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

    /**
     * Parse dos parâmetros JSON
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> parseParameters(String paramsStr) {
        try {
            if (paramsStr == null || paramsStr.isBlank()) {
                return new HashMap<>();
            }

            // Remove ```json e ``` se houver
            paramsStr = paramsStr.replace("```json", "").replace("```", "").trim();

            return (Map<String, String>) objectMapper.readValue(paramsStr, Map.class);
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
            String result = tool.execute(params);
            log.debug("[AGENT] Tool {} executada com sucesso", toolName);
            return result;
        } catch (Exception e) {
            String error = "Erro ao executar tool " + toolName + ": " + e.getMessage();
            log.error("[AGENT] {}", error, e);
            return "ERRO: " + error;
        }
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



