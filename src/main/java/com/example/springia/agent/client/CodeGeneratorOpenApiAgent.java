package com.example.springia.agent.client;

import com.example.springia.agent.responseapi.request.RequestToolDefinition;
import com.example.springia.agent.responseapi.request.RequestToolProperty;
import com.example.springia.agent.tool.DockerBuildAndTestTool;
import com.example.springia.agent.tool.Tool;
import com.example.springia.agent.tool.files.*;
import com.example.springia.model.Project;
import com.example.springia.repository.ProjectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.responses.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * **Responsabilidade:** Gera código Java usando modelo gpt-5.3-codex com Responses API usando `com.openai.client.OpenAIClient`
 * <p>{@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.client.CodeGeneratorOpenApiAgent" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}</p>
 */
@Slf4j
@Component
public class CodeGeneratorOpenApiAgent {

    private static final String SYSTEM_PROMPT_RESOURCE_PATH = "prompts/system-prompt.md";
    private static final int MAX_TOOL_LOOPS = 10;

    private final ObjectMapper objectMapper;
    private final ProjectRepository projectRepository;
    private final DockerBuildAndTestTool dockerBuildAndTestTool;
    private final Map<String, Tool> supportedTools;

    public CodeGeneratorOpenApiAgent(ObjectMapper objectMapper, ProjectRepository projectRepository, DockerBuildAndTestTool dockerBuildAndTestTool) {
        this.objectMapper = objectMapper;
        this.projectRepository = projectRepository;
        this.supportedTools = new HashMap<>();
        CreateFileTool createFileTool = new CreateFileTool();
        this.supportedTools.put(createFileTool.getName(), createFileTool);
        CreateDirectoryTool createDirectoryTool = new CreateDirectoryTool();
        this.supportedTools.put(createDirectoryTool.getName(), createDirectoryTool);
        FindFilesTool findFilesTool = new FindFilesTool();
        this.supportedTools.put(findFilesTool.getName(), findFilesTool);
        GrepFilesTool grepFilesTool = new GrepFilesTool();
        this.supportedTools.put(grepFilesTool.getName(), grepFilesTool);
        ReadFileTool readFileTool = new ReadFileTool();
        this.supportedTools.put(readFileTool.getName(), readFileTool);
        UpdateFileTool updateFileTool = new UpdateFileTool();
        this.supportedTools.put(updateFileTool.getName(), updateFileTool);
        this.dockerBuildAndTestTool = dockerBuildAndTestTool;
        this.supportedTools.put(dockerBuildAndTestTool.getName(), dockerBuildAndTestTool);
    }

    public String executar(String userPrompt) {
        OpenAIClient client = getOpenAIClient();
        String deploymentName = System.getenv().getOrDefault("AZURE_OPENAI_DEPLOYMENT", "gpt-5.3-codex").trim();
        ResponseCreateParams.Builder createParamsBuilder = ResponseCreateParams.builder()
                .model(deploymentName)
                .input(userPrompt)
                .toolChoice(ToolChoiceOptions.NONE);
        ResponseCreateParams createParams = createParamsBuilder.build();
        Response response = client.responses().create(createParams);
        for (ResponseOutputItem responseOutputItem : response.output()) {
        Optional<ResponseOutputMessage> responseOutputMessageOptional = responseOutputItem.message();
            if(responseOutputMessageOptional.isPresent()) {
                ResponseOutputMessage responseOutputMessage = responseOutputMessageOptional.get();
                List<ResponseOutputMessage.Content> contentList = responseOutputMessage.content();
                for (ResponseOutputMessage.Content content : contentList) {
                    Optional<ResponseOutputText> responseOutputTextOptional = content.outputText();
                    if(responseOutputTextOptional.isPresent()) {
                        ResponseOutputText responseOutputText = responseOutputTextOptional.get();
                        String text = responseOutputText.text();
                        return  text;
                    }
                }
            }
        }
        return "";
    }

    public String executar(Project project, String userPrompt) {
        log.info("[EXECUTAR] Iniciando chamada da Responses API do projeto {}", project != null ? project.getSigla() : null);

        try {

            OpenAIClient client = getOpenAIClient();

            String deploymentName = System.getenv().getOrDefault("AZURE_OPENAI_DEPLOYMENT", "gpt-5.3-codex").trim();

            String systemPrompt = readResourceFile(SYSTEM_PROMPT_RESOURCE_PATH);

            List<FunctionTool> functionTools = montarTools();

            ResponseCreateParams.Builder createParamsBuilder = ResponseCreateParams.builder()
                    .model(deploymentName)
                    .instructions(systemPrompt)
                    .input(userPrompt)
                    .toolChoice(!functionTools.isEmpty() ? ToolChoiceOptions.AUTO : ToolChoiceOptions.NONE);

            for (FunctionTool functionTool : functionTools) {
                createParamsBuilder.addTool(functionTool);
            }

            ResponseCreateParams createParams = createParamsBuilder.build();

            Response response = client.responses().create(createParams);
            List<String> toolCalls = new ArrayList<>();
            List<String> toolCallOutputs = new ArrayList<>();
            List<String> textos = new ArrayList<>();

            coletarSaida(response.output(), toolCalls, toolCallOutputs, textos);

            int loop = 0;
            while (loop < MAX_TOOL_LOOPS) {
                List<ResponseInputItem> functionCallOutputs = executarFunctionCalls(response.output(), toolCallOutputs);
                if (!functionCallOutputs.isEmpty()) {
                    log.info("[EXECUTAR] Enviando function_call_output quantidade={} loop={}", functionCallOutputs.size(), loop + 1);

                    response = client.responses().create(ResponseCreateParams.builder()
                            .model(deploymentName)
                            .previousResponseId(response.id())
                            .inputOfResponse(functionCallOutputs)
                            .build());

                    coletarSaida(response.output(), toolCalls, toolCallOutputs, textos);
                    loop++;
                    continue;
                }

                if (project != null) {

                    String buildFinalOutput = validarBuildFinalComDocker(project);
                    toolCallOutputs.add("final_validation=docker_build_and_test status=completed output=" + buildFinalOutput);

                    if (isBuildValidationSuccessful(buildFinalOutput)) {
                        break;
                    }

                    String feedback = "GATE DE FINALIZAÇÃO FALHOU:\n"
                            + buildFinalOutput
                            + "\n\nCORRIJA OS ERROS ACIMA E TENTE NOVAMENTE."
                            + "\n\nOBRIGATÓRIO: responda usando ferramentas para aplicar a correção; não finalize com texto.";
                    log.warn("[EXECUTAR] Validação final falhou; realimentando LLM com feedback do build");

                    response = client.responses().create(ResponseCreateParams.builder()
                            .model(deploymentName)
                            .previousResponseId(response.id())
                            .input(feedback)
                            .toolChoice(functionTools.isEmpty() ? ToolChoiceOptions.NONE : ToolChoiceOptions.REQUIRED)
                            .build());

                    coletarSaida(response.output(), toolCalls, toolCallOutputs, textos);
                }

                loop++;
            }

            String resposta = String.join("\n", textos);
            if (!toolCalls.isEmpty() || !toolCallOutputs.isEmpty()) {
                resposta = "TOOL_CALLS:\n"
                        + String.join("\n", toolCalls)
                        + "\n\nTOOL_CALL_OUTPUTS:\n"
                        + String.join("\n", toolCallOutputs)
                        + (resposta.isBlank() ? "" : "\n\nTEXT:\n" + resposta);
            }

            log.info("[EXECUTAR] Chamada concluída resposta: {}", resposta);
            return resposta;
        } catch (Exception e) {
            log.error("[EXECUTAR] Erro ao chamar Responses API", e);
            throw e;
        }
    }

    private OpenAIClient getOpenAIClient() {
        String baseUrl = System.getenv().getOrDefault("SPRING_AI_OPENAI_BASE_URL", "").trim();
        String apiKey = System.getenv().getOrDefault("SPRING_AI_OPENAI_API_KEY", "").trim();
        String deploymentName = System.getenv().getOrDefault("AZURE_OPENAI_DEPLOYMENT", "gpt-5.3-codex").trim();

        validarConfiguracao(baseUrl, apiKey, deploymentName);

        return OpenAIOkHttpClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    private List<FunctionTool> montarTools() {
        log.debug("[MONTAR_TOOLS] Montando declaração de tools para Responses API");

        List<RequestToolDefinition> definitions = List.of(
                CreateFileTool.createTool(),
                CreateDirectoryTool.createTool(),
                FindFilesTool.createTool(),
                GrepFilesTool.createTool(),
                ReadFileTool.createTool(),
                criarUpdateFileToolDefinition(),
                DockerBuildAndTestTool.createTool()
        );

        List<FunctionTool> tools = new ArrayList<>();
        for (RequestToolDefinition definition : definitions) {
            tools.add(toFunctionTool(definition));
        }
        return tools;
    }

    private FunctionTool toFunctionTool(RequestToolDefinition definition) {
        log.debug("[TO_FUNCTION] Convertendo definição da tool: {}", definition.getName());

        if (definition.getParameters() == null || definition.getParameters().getType() == null) {
            throw new IllegalArgumentException("Definição de parâmetros inválida para tool: " + definition.getName());
        }

        Map<String, Object> properties = new HashMap<>();
        if (definition.getParameters() != null && definition.getParameters().getProperties() != null) {
            for (Map.Entry<String, RequestToolProperty> entry : definition.getParameters().getProperties().entrySet()) {
                RequestToolProperty value = entry.getValue();
                properties.put(entry.getKey(), Map.of(
                        "type", value.getType(),
                        "description", value.getDescription()
                ));
            }
        }

        FunctionTool.Parameters parameters = FunctionTool.Parameters.builder()
                .putAdditionalProperty("type", JsonValue.from(definition.getParameters().getType()))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required", JsonValue.from(definition.getParameters().getRequired()))
                .putAdditionalProperty("additionalProperties", JsonValue.from(definition.getParameters().getAdditionalProperties()))
                .build();

        return FunctionTool.builder()
                .name(definition.getName())
                .description(definition.getDescription())
                .parameters(parameters)
                .strict(Boolean.TRUE.equals(definition.getStrict()))
                .build();
    }

    private RequestToolDefinition criarUpdateFileToolDefinition() {
        return RequestToolDefinition.builder()
                .type("function")
                .name("update_file")
                .description("Atualiza arquivo existente substituindo trecho específico")
                .parameters(com.example.springia.agent.responseapi.request.RequestToolParameters.builder()
                        .type("object")
                        .properties(Map.of(
                                "file_path", RequestToolProperty.builder().type("string").description("Caminho do arquivo a alterar (absoluto)").build(),
                                "old_text", RequestToolProperty.builder().type("string").description("Trecho atual a ser substituído").build(),
                                "new_text", RequestToolProperty.builder().type("string").description("Novo trecho que substituirá old_text").build(),
                                "replace_all", RequestToolProperty.builder().type("boolean").description("Se true substitui todas as ocorrências").build()
                        ))
                        .required(List.of("file_path", "old_text", "new_text", "replace_all"))
                        .additionalProperties(false)
                        .build())
                .strict(true)
                .build();
    }

    private void coletarSaida(List<ResponseOutputItem> output,
                              List<String> toolCalls,
                              List<String> toolCallOutputs,
                              List<String> textos) {
        log.debug("[COLETAR_SAIDA] Processando itens de saída quantidade={}", output.size());

            for (ResponseOutputItem responseOutputItem : output) {
                responseOutputItem.functionCall().ifPresent(functionCall -> {
                    String callSummary = String.format("name=%s arguments=%s callId=%s",
                            functionCall.name(),
                            functionCall.arguments(),
                            functionCall.id());
                    toolCalls.add(callSummary);
                    log.trace("[COLETAR_SAIDA] [TOOL_CALL] {}", callSummary);
                });

                responseOutputItem.functionCallOutput().ifPresent(functionCallOutput -> {
                    String outputSummary = String.format("output=%s status=%s callId=%s",
                            functionCallOutput.output(),
                            functionCallOutput.status(),
                            functionCallOutput.callId());
                    toolCallOutputs.add(outputSummary);
                    log.trace("[COLETAR_SAIDA] [TOOL_OUT] {}", outputSummary);
                });

                Optional<ResponseOutputMessage> responseOutputMessageOptional = responseOutputItem.message();
                if(responseOutputMessageOptional.isPresent()) {
                    ResponseOutputMessage responseOutputMessage = responseOutputMessageOptional.get();
                    List<ResponseOutputMessage.Content> contentList = responseOutputMessage.content();
                    for (ResponseOutputMessage.Content content : contentList) {
                        Optional<ResponseOutputText> responseOutputTextOptional = content.outputText();
                        if(responseOutputTextOptional.isPresent()) {
                            ResponseOutputText responseOutputText = responseOutputTextOptional.get();
                            String text = responseOutputText.text();
                            textos.add(text);
                            log.trace("[COLETAR_SAIDA] [TEXTO] {}", text);
                        }
                    }
                }
            }
    }

    private List<ResponseInputItem> executarFunctionCalls(List<ResponseOutputItem> output,
                                                          List<String> toolCallOutputs) {
        log.debug("[EXEC_FUNC_CALL] Executando function calls quantidade={}", output.size());

        List<ResponseInputItem> results = new ArrayList<>();

        for (ResponseOutputItem responseOutputItem : output) {
            Optional<ResponseFunctionToolCall> functionCallOptional = responseOutputItem.functionCall();
            if (functionCallOptional.isEmpty()) {
                continue;
            }

            ResponseFunctionToolCall functionCall = functionCallOptional.get();

            try {
                Map<String, String> params = parseFunctionArguments(functionCall.arguments());
                String toolResult = executarTool(functionCall.name(), params);

                results.add(ResponseInputItem.ofFunctionCallOutput(
                        ResponseInputItem.FunctionCallOutput.builder()
                                .callId(functionCall.callId())
                                .output(toolResult)
                                .type(JsonValue.from("function_call_output"))
                                .build()
                ));

                String outputSummary = String.format("callId=%s status=completed output=%s",
                        functionCall.callId(),
                        toolResult);
                toolCallOutputs.add(outputSummary);
                log.info("[TOOL_OUT] {}", outputSummary);
            } catch (Exception e) {
                log.error("[EXEC_FUNC_CALL] Erro ao executar tool create_file", e);

                String errorOutput = "Erro ao executar " + functionCall.name() + ": " + e.getMessage();
                results.add(ResponseInputItem.ofFunctionCallOutput(
                        ResponseInputItem.FunctionCallOutput.builder()
                                .callId(functionCall.callId())
                                .output(errorOutput)
                                .type(JsonValue.from("function_call_output"))
                                .build()
                ));

                toolCallOutputs.add(String.format("callId=%s status=failed output=%s",
                        functionCall.callId(),
                        errorOutput));
            }
        }

        return results;
    }

    private String executarTool(String toolName, Map<String, String> params) throws Exception {
        log.debug("[EXEC_TOOL] Executando tool: {}", toolName);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            log.debug("[EXEC_TOOL] Executando tool - param: {}", entry.getKey());
            log.debug("[EXEC_TOOL] Executando tool:         {}", entry.getValue());
        }

        Tool tool = supportedTools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Tool não suportada: " + toolName);
        }
        return tool.execute(params);
    }

    private Map<String, String> parseFunctionArguments(String arguments) {
        log.debug("[PARSE_ARGS] Convertendo argumentos da tool");

        try {
            Map<String, Object> rawParams = objectMapper.readValue(arguments, new TypeReference<>() {
            });

            Map<String, String> parsedParams = new HashMap<>();
            rawParams.forEach((key, value) -> parsedParams.put(key, value == null ? null : String.valueOf(value)));
            for (Map.Entry<String, String> entry : parsedParams.entrySet()) {
                log.trace("{} = {}", entry.getKey(), entry.getValue());
            }
            return parsedParams;
        } catch (Exception e) {
            log.error("[PARSE_ARGS] Erro ao converter argumentos da tool", e);
            throw new IllegalArgumentException("Não foi possível converter argumentos da tool", e);
        }
    }

    private void validarConfiguracao(String baseUrl, String apiKey, String deploymentName) {
        log.debug("[VALIDAR_CONF] Validando variáveis de ambiente do OpenAI client");

        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("Defina SPRING_AI_OPENAI_BASE_URL (ex: https://<resource>.openai.azure.com/openai/v1)");
        }

        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("Defina SPRING_AI_OPENAI_API_KEY para autenticar via API Key");
        }

        if (deploymentName.isBlank()) {
            throw new IllegalArgumentException("Defina AZURE_OPENAI_DEPLOYMENT com o nome do deployment do modelo");
        }

        if (!baseUrl.contains("/openai/")) {
            throw new IllegalArgumentException("SPRING_AI_OPENAI_BASE_URL deve apontar para o endpoint OpenAI v1 (contendo /openai/v1)");
        }
    }

    private String validarBuildFinalComDocker(Project project) {
        log.debug("[VALIDAR_BUILD] Iniciando validação final obrigatória");

//        if (project == null || project.getId() == null) {
//            throw new IllegalArgumentException("Projeto com id é obrigatório para executar docker_build_and_test");
//        }

        Map<String, String> params = new HashMap<>();
        params.put("id_projeto", String.valueOf(project.getId()));
        params.put("validate_all_repos", "true");

        try {
            String buildResult = executarTool(dockerBuildAndTestTool.getName(), params);
            if (isBuildValidationSuccessful(buildResult)) {
                log.info("[VALIDAR_BUILD] Validação final concluída com sucesso");
            } else {
                log.warn("[VALIDAR_BUILD] Validação final falhou: {}", buildResult);
            }
            return buildResult;
        } catch (Exception e) {
            log.error("[VALIDAR_BUILD] Erro na validação final com docker_build_and_test", e);
            return "ERRO ao executar docker_build_and_test: " + e.getMessage();
        }
    }

    private boolean isBuildValidationSuccessful(String buildResult) {
        return "Passou!".equalsIgnoreCase(buildResult != null ? buildResult.trim() : "");
    }

    private String readResourceFile(String resourcePath) {
        log.debug("[READ_RES] Lendo arquivo de resources: {}", resourcePath);

        try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Arquivo não encontrado em resources: " + resourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("[READ_RES] Erro ao ler arquivo de resources: {}", resourcePath, e);
            throw new RuntimeException("Falha ao ler arquivo de resources: " + resourcePath, e);
        }
    }

}
