package com.example.springia.agent.tool.discovery;

import com.example.springia.agent.responseapi.request.RequestToolDefinition;
import com.example.springia.agent.responseapi.request.RequestToolParameters;
import com.example.springia.agent.responseapi.request.RequestToolProperty;
import com.example.springia.agent.tool.Tool;
import com.example.springia.model.Project;
import com.example.springia.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ferramenta que gera um system prompt com os dados do projeto e seus repositórios
 * em formato adequado para a OpenAI Responses API.
 * <p>{@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.discovery.ProjectTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}</p>
 * <p>{@code logging.level.com.example.springia.agent.tool.discovery.ProjectTool=DEBUG}</p>
 */
@Slf4j
@Component
public class ProjectTool implements Tool {

    private final ProjectService projectService;

    public ProjectTool(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public String getName() {
        log.info("[GET_NAME] Retornando nome da ferramenta");
        return "project_tool";
    }

    @Override
    public String getDescription() {
        log.info("[GET_DESCRIPTION] Retornando descrição da ferramenta");
        return "Retorna um system prompt com os dados do projeto, incluindo constitution do projeto e dos repositórios, usando estrutura compatível com a OpenAI Responses API";
    }

    @Override
    public Map<String, String> getParameters() {
        log.info("[GET_PARAMETERS] Retornando parâmetros da ferramenta");
        Map<String, String> params = new HashMap<>();
        params.put("project_id", "ID do projeto a ser carregado - OBRIGATORIO");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) {
        log.info("[EXECUTE] Iniciando execução da project_tool");

        String projectIdRaw = params.get("project_id");
        Long projectId = parseProjectId(projectIdRaw);

        Project project = projectService.findById(projectId);

        return buildSystemPrompt(project);
    }

    @org.springframework.ai.tool.annotation.Tool(
            name = "project_tool",
            description = "Retorna um system prompt com os dados do projeto usando a estrutura da OpenAI API"
    )
    public String projectTool(
            @org.springframework.ai.tool.annotation.ToolParam(description = "ID do projeto") Long projectId
    ) {
        log.info("[PROJECT_TOOL] Iniciando execução da projectTool. projectId={}", projectId);

        Map<String, String> params = new HashMap<>();
        params.put("project_id", projectId == null ? "" : String.valueOf(projectId));
        return execute(params);
    }

    public String getSystemPrompt(Long id) {
        log.info("[GET_SYS_PROMPT] Iniciando geração do system prompt");

        if (id == null) {
            log.warn("[GET_SYS_PROMPT] id do projeto não informado");
            throw new IllegalArgumentException("O id do projeto é obrigatório");
        }

        Project project = projectService.findById(id);

        return buildSystemPrompt(project);
    }

    public static RequestToolDefinition createTool() {
        log.info("[CREATE_TOOL] Criando definição da ferramenta project_tool");
        return RequestToolDefinition.builder()
                .type("function")
                .name("project_tool")
                .description("Retorna um system prompt com os dados do projeto usando a estrutura da OpenAI Responses API")
                .parameters(RequestToolParameters.builder()
                        .type("object")
                        .properties(Map.of(
                                "project_id", RequestToolProperty.builder()
                                        .type("string")
                                        .description("ID do projeto a ser carregado")
                                        .build()
                        ))
                        .required(List.of("project_id"))
                        .additionalProperties(false)
                        .build())
                .strict(true)
                .build();
    }

    private Long parseProjectId(String projectIdRaw) {
        log.debug("[PARSE_PROJECT_ID] Convertendo project_id");

        if (projectIdRaw == null || projectIdRaw.isBlank()) {
            log.warn("[PARSE_PROJECT_ID] project_id não informado");
            throw new IllegalArgumentException("O parâmetro 'project_id' é obrigatório");
        }

        try {
            return Long.valueOf(projectIdRaw.trim());
        } catch (NumberFormatException e) {
            log.error("[PARSE_PROJECT_ID] Erro ao converter project_id", e);
            log.warn("[PARSE_PROJECT_ID] project_id inválido: {}", projectIdRaw);
            throw new IllegalArgumentException("O parâmetro 'project_id' deve ser numérico: " + projectIdRaw, e);
        }
    }

    private String buildSystemPrompt(Project project) {
        log.debug("[BUILD_SYS_PROMPT] Montando system prompt do projeto");

        StringBuilder sb = new StringBuilder();

        sb.append("""
                # SYSTEM PROMPT
                                
                Você está trabalhando em um projeto de software com os dados abaixo.
                Considere estas informações como fonte oficial de contexto antes de gerar, alterar ou validar código.
                                
                ## Projeto
                """);

        sb.append("\n");
        sb.append(projectService.getConstitution(project));
        sb.append("""
                
                ## Regras de uso
                - Não invente classes, métodos, imports ou arquivos sem conferir o contexto real.
                - Use constitution e structure como fonte principal de verdade.
                - Respeite os caminhos e a linguagem de cada repositório.
                - Antes de alterar código existente, leia o arquivo completo.
                """);

        return sb.toString().trim();
    }

}