package com.example.springia.agent;

import com.example.springia.agent.responseapi.request.*;
import com.example.springia.agent.responseapi.response.CreateFileToolParams;
import com.example.springia.agent.responseapi.response.ResponseOutputItem;
import com.example.springia.agent.responseapi.response.ResponseToolDefinition;
import com.example.springia.agent.responseapi.response.ResponsesApiResponse;
import com.example.springia.agent.tool.DockerBuildAndTestTool;
import com.example.springia.agent.tool.files.*;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.repository.ProjectRepository;
import com.example.springia.utils.JsonUtils;
import com.example.springia.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.CodeGeneratorAgent" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Service
@Slf4j
public class CodeGeneratorAgent {

    private static final String SYSTEM_PROMPT_RESOURCE_PATH = "prompts/system-prompt.md";

    private final RestClient restClient;
    private final String baseUrl;

    @Autowired
    CreateFileTool createFileTool;

    @Autowired
    DockerBuildAndTestTool dockerBuildAndTestTool;

    @Autowired
    ProjectRepository projectRepository;

    public CodeGeneratorAgent(
            RestClient.Builder restClientBuilder,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl
    ) {
        this.baseUrl = baseUrl;
        this.restClient = restClientBuilder
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String generateJavaCode(String userPrompt) {
        log.info("[GEN_CODE] Iniciando geração com Responses API");

        ResponsesApiRequest responsesApiRequest = criarRequest();
        String systemPrompt = readResourceFile(SYSTEM_PROMPT_RESOURCE_PATH);

        responsesApiRequest.getInput().get(0).getContent().get(0).setText(escapeDoubleQuotes(systemPrompt));
        responsesApiRequest.getInput().get(1).getContent().get(0).setText(escapeDoubleQuotes(userPrompt));
        responsesApiRequest.setToolChoice("required");
        String requestBody = JsonUtils.toJsonFormated(responsesApiRequest);

        log.info("[GEN_CODE] REQUEST: file:{}", LogUtils.saveLog(requestBody, "request", "json"));
        try {
            String response = restClient.post()
                    .uri(baseUrl + "/responses")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info("[GEN_CODE] RESPONSE: file:{}", LogUtils.saveLog(response, "response", "json"));

            ResponsesApiResponse resp = JsonUtils.fromJson(response, ResponsesApiResponse.class);
            List<ResponseOutputItem> output = resp.getOutput();

            for (ResponseOutputItem responseOutputItem : output) {
                log.info("[GEN_CODE] {} - {} - {}", responseOutputItem.getType(), responseOutputItem.getName(), responseOutputItem.getArguments());
            }

            for (ResponseOutputItem responseOutputItem : output) {

                responseOutputItem.getId();
                responseOutputItem.getType();
                responseOutputItem.getStatus();
                responseOutputItem.getArguments();
                responseOutputItem.getName();
                responseOutputItem.getContent();

                if("function_call".equals(responseOutputItem.getType())) {
                    if("create_file".equals(responseOutputItem.getName())) {
                        String arguments = responseOutputItem.getArguments();
                        log.info("[GEN_CODE] Function Call: create_file - Arguments: {}", arguments);
                        String normalizedArguments = normalizeArguments(arguments);
                        log.info("[GEN_CODE] Function Call: create_file - Arguments: {}", normalizedArguments);
                        CreateFileToolParams obj = JsonUtils.toObject(normalizedArguments, CreateFileToolParams.class);
                        log.info("[GEN_CODE] Function Call: create_file - obj: {}", obj);

                        createFileTool.execute(Map.of(
                                "file_path", obj.getFilePath(),
                                "content", obj.getContent()
                        ));

                    } else if("write_file".equals(responseOutputItem.getName())) {
                        log.info("[GEN_CODE] Function Call: write_file - Arguments: {}", responseOutputItem.getArguments());
                    } else if("create_directory".equals(responseOutputItem.getName())) {
                        log.info("[GEN_CODE] Function Call: create_directory - Arguments: {}", responseOutputItem.getArguments());
                    } else if("find_files".equals(responseOutputItem.getName())) {
                        log.info("[GEN_CODE] Function Call: find_files - Arguments: {}", responseOutputItem.getArguments());
                    } else if("grep_files".equals(responseOutputItem.getName())) {
                        log.info("[GEN_CODE] Function Call: grep_files - Arguments: {}", responseOutputItem.getArguments());
                    } else if("read_file".equals(responseOutputItem.getName())) {
                        log.info("[GEN_CODE] Function Call: read_file - Arguments: {}", responseOutputItem.getArguments());
                    } else {
                        log.info("[GEN_CODE] Function Call: {} - Arguments: {}", responseOutputItem.getName(), responseOutputItem.getArguments());
                    }
                    log.info("[GEN_CODE] Function Call: {}", responseOutputItem.getArguments());
                }
            }

            List<ResponseToolDefinition> tools = resp.getTools();
            for (ResponseToolDefinition tool : tools) {
                tool.getType();
                tool.getDescription();
                tool.getName();
                Map<String, Object> parameters = tool.getParameters();
                for (Map.Entry<String, Object> s : parameters.entrySet()) {
                    s.getKey();
                    s.getValue();
                }
                tool.getType();
                tool.getType();
                tool.getType();
            }

            Project project = projectRepository.findById(1L).orElse(null);
            dockerBuildAndTestTool.setProjetc(project);
            String execute = dockerBuildAndTestTool.execute(Map.of(
                    "dockerfile_path", "Dockerfile",
                    "context_path", "."
            ));

            log.info("{}", execute);

            return extractCodeFromResponse(response);

        } catch (Exception e) {
            log.error("[GEN_CODE] Erro ao chamar Responses API", e);
            throw new RuntimeException("Falha ao gerar código", e);
        }
    }

    private String escapeDoubleQuotes(String text) {
        log.debug("[ESC_DQ] Normalizando aspas duplas");
        if (text == null) {
            return null;
        }

        return text.replace("\"", "\\\"");
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

    private String extractCodeFromResponse(String response) {
        // Parse JSON response e extrai o conteúdo
        // Implementação depende da estrutura real da Responses API
        return response;
    }

    private String normalizeArguments(String arguments) {
        log.debug("[PARSE_CFP] Normalizando argumentos da create_file");

        if (arguments == null || arguments.isBlank()) {
            return null;
        }

        String normalizedArguments = arguments.trim();
        if (normalizedArguments.startsWith("\"") && normalizedArguments.endsWith("\"")) {
            normalizedArguments = JsonUtils.toObject(normalizedArguments, String.class);
        }

        return normalizedArguments;
    }

    public ResponsesApiRequest criarRequest() {
        log.info("[CRIAR_REQ] Montando request da Responses API");

        return ResponsesApiRequest.builder()
                .model("gpt-5.3-codex")
                .input(List.of(
                        RequestInputMessage.builder()
                                .role("system")
                                .content(List.of(
                                        RequestInputContent.builder()
                                                .type("input_text")
                                                .text("Você é um agente gerador de código especializado em Java + Spring Boot. Gere código limpo, com imports corretos, estrutura de projeto e boas práticas. o pacote da aplicacao é br.com.gustavodasilva.minhaapp")
                                                .build()
                                ))
                                .build(),
                        RequestInputMessage.builder()
                                .role("user")
                                .content(List.of(
                                        RequestInputContent.builder()
                                                .type("input_text")
                                                .text("Crie um CRUD completo de Pessoa com entidade, repository, service e controller. Salve os arquivos no sistema usando as tools disponíveis.")
                                                .build()
                                ))
                                .build()
                ))
                .tools(List.of(
                        CreateDirectoryTool.createTool(),
                        CreateFileTool.createTool(),
                        FindFilesTool.createTool(),
                        GrepFilesTool.createTool(),
                        ReadFileTool.createTool()
                ))
                .parallelToolCalls(true)
                .temperature(1.0)
                .topP(0.98)
                .maxOutputTokens(null)
                .toolChoice(null)
                .text(RequestTextConfig.builder()
                        .format(RequestTextFormat.builder()
                                .type("text")
                                .build())
                        .verbosity("medium")
                        .build())
                .build();
    }

}