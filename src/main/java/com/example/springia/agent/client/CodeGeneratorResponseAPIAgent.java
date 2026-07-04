package com.example.springia.agent.client;

import com.example.springia.agent.responseapi.request.*;
import com.example.springia.agent.responseapi.response.CreateFileToolParams;
import com.example.springia.agent.responseapi.response.ResponseOutputItem;
import com.example.springia.agent.responseapi.response.ResponsesApiResponse;
import com.example.springia.agent.tool.DockerBuildAndTestTool;
import com.example.springia.agent.tool.files.*;
import com.example.springia.dto.DockerBuildAndTestToolRepo;
import com.example.springia.dto.DockerBuildAndTestToolReturn;
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

/**
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.CodeGeneratorAgent" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Service
@Slf4j
public class CodeGeneratorResponseAPIAgent {

    private static final String SYSTEM_PROMPT_RESOURCE_PATH = "prompts/system-prompt.md";

    private final RestClient restClient;
    private final String baseUrl;

    @Autowired
    CreateFileTool createFileTool;

    @Autowired
    ReadFileTool readFileTool;

    @Autowired
    DockerBuildAndTestTool dockerBuildAndTestTool;

    @Autowired
    ProjectRepository projectRepository;

    public CodeGeneratorResponseAPIAgent(
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

    public void generateCode(Long projectId, String userPrompt) {
        log.info("[GEN_CODE] Iniciando geração com Responses API");

        String systemPrompt = readResourceFile(SYSTEM_PROMPT_RESOURCE_PATH);
        ResponsesApiRequest responsesApiRequest = createDefaultRequest(systemPrompt, userPrompt);

        try {

            String errorLog = "";
            int i = 0;
            do{
                String text = responsesApiRequest.getInput().get(1).getContent().get(0).getText();

                if(i > 0){
                    text = text.concat("\n\n").concat("# Step "+i+"\n" + errorLog);
                }

                responsesApiRequest.getInput().get(1).getContent().get(0).setText(text);
                String requestBody = JsonUtils.toJsonFormated(responsesApiRequest);
                log.info("[GEN_CODE] REQUEST: file:{}", LogUtils.saveLog(requestBody, "request", "json"));
                String response = restClient.post()
                        .uri(baseUrl + "/responses")
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                log.info("[GEN_CODE] RESPONSE: file:{}", LogUtils.saveLog(response, "response", "json"));

                ResponsesApiResponse responseObj = JsonUtils.fromJson(response, ResponsesApiResponse.class);

                List<ResponseOutputItem> output = responseObj.getOutput();

                output.forEach(responseOutputItem->{log.info("[GEN_CODE] {} - {} - {}", responseOutputItem.getType(), responseOutputItem.getName(), responseOutputItem.getArguments());});

                for (ResponseOutputItem responseOutputItem : output) {

                    if("function_call".equals(responseOutputItem.getType())) {
                        if("create_file".equals(responseOutputItem.getName())) {
                            String arguments = responseOutputItem.getArguments();
                            String normalizedArguments = normalizeArguments(arguments);
                            CreateFileToolParams obj = JsonUtils.toObject(normalizedArguments, CreateFileToolParams.class);
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
                            String arguments = responseOutputItem.getArguments();
                            String normalizedArguments = normalizeArguments(arguments);
                            CreateFileToolParams obj = JsonUtils.toObject(normalizedArguments, CreateFileToolParams.class);
                            String content = readFileTool.execute(Map.of(
                                    "file_path", obj.getFilePath()
                            ));
                            log.info("[GEN_CODE] Function Call: read_file - Content: {}", content);
                            StringBuilder sb = new StringBuilder();
                            sb.append("Conteúdo do arquivo: ");
                            sb.append("\n");
                            sb.append("``` ");
                            sb.append(obj.getFilePath());
                            sb.append("\n");
                            sb.append(content);
                            sb.append("\n");
                            sb.append("```");
                            sb.append("\n");
                            errorLog += sb.toString();
                        } else {
                            log.info("[GEN_CODE] Function Call: {} - Arguments: {}", responseOutputItem.getName(), responseOutputItem.getArguments());
                        }
                        log.info("[GEN_CODE] Function Call: {}", responseOutputItem.getArguments());
                    }
                }

                Project project = projectRepository.findById(projectId).orElse(null);

                dockerBuildAndTestTool.setProjetc(project);

                DockerBuildAndTestToolReturn buildReturn = dockerBuildAndTestTool.execute2(Map.of(
                        "dockerfile_path", "Dockerfile",
                        "context_path", "."
                ));

                log.info("[GEN_CODE] BUILD - Sucesso?: {}", (buildReturn.isAllSuccess()?"SIM":"NAO"));

                if(!buildReturn.isAllSuccess()){
                    StringBuilder sb = new StringBuilder();
                    List<DockerBuildAndTestToolRepo> builds = buildReturn.getBuilds();
                    for (DockerBuildAndTestToolRepo build : builds) {
                        if(!build.isSuccess()){
                            sb.append("ERRO DE COMPILAÇÃO EM ");
                            sb.append(build.getRepoName());
                            sb.append("\n");
                            sb.append("Faça a correção de acordo com o log abaixo.");
                            sb.append("\n");
                            sb.append("```");
                            sb.append(build.getLogError());
                            sb.append("```");
                        }
                    }
                    errorLog += sb.toString();
                } else {
                    errorLog = "";
                }

                log.info("{} - {}", i++, buildReturn);

            }while(!"".equals(errorLog) && i < 20);

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

    public ResponsesApiRequest createDefaultRequest(String systemPrompt, String userPrompt) {
        log.info("[CRIAR_REQ] Montando request da Responses API");

        return ResponsesApiRequest.builder()
                .model("gpt-5.3-codex")
                .input(List.of(
                        RequestInputMessage.builder()
                                .role("system")
                                .content(List.of(
                                        RequestInputContent.builder()
                                                .type("input_text")
                                                .text(systemPrompt)
                                                .build()
                                ))
                                .build(),
                        RequestInputMessage.builder()
                                .role("user")
                                .content(List.of(
                                        RequestInputContent.builder()
                                                .type("input_text")
                                                .text(userPrompt)
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
                .toolChoice("required")
                .text(RequestTextConfig.builder()
                        .format(RequestTextFormat.builder()
                                .type("text")
                                .build())
                        .verbosity("medium")
                        .build())
                .build();
    }

}