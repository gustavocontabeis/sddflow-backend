package com.example.springia.agent.tool.discovery;

import com.example.springia.agent.responseapi.request.RequestToolDefinition;
import com.example.springia.agent.responseapi.request.RequestToolParameters;
import com.example.springia.agent.responseapi.request.RequestToolProperty;
import com.example.springia.agent.tool.Tool;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ferramenta que gera um system prompt com os dados do projeto e seus repositórios
 * em formato adequado para a OpenAI Responses API.
 * <p>{@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.discovery.ProjectTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}</p>
 */
@Slf4j
@Component
public class ProjectTool implements Tool {

    private final ProjectRepository projectRepository;

    public ProjectTool(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public String getName() {
        return "project_tool";
    }

    @Override
    public String getDescription() {
        return "Retorna um system prompt com os dados do projeto, incluindo constitution do projeto e dos repositórios, usando estrutura compatível com a OpenAI Responses API";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("project_id", "ID do projeto a ser carregado - OBRIGATORIO");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) {
        log.info("[EXECUTE] Iniciando execução da project_tool");

        String projectIdRaw = params.get("project_id");
        Long projectId = parseProjectId(projectIdRaw);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado: " + projectId));

        return buildSystemPrompt(project);
    }

    @org.springframework.ai.tool.annotation.Tool(
            name = "project_tool",
            description = "Retorna um system prompt com os dados do projeto usando a estrutura da OpenAI API"
    )
    public String projectTool(
            @org.springframework.ai.tool.annotation.ToolParam(description = "ID do projeto") Long projectId
    ) {
        log.info("[PROJECT_TOOL] Iniciando execução da projectTool");

        Map<String, String> params = new HashMap<>();
        params.put("project_id", projectId == null ? "" : String.valueOf(projectId));
        return execute(params);
    }

    public String getSystemPrompt(Long id) {
        log.info("[GET_SYS_PROMPT] Iniciando geração do system prompt");

        if (id == null) {
            throw new IllegalArgumentException("O id do projeto é obrigatório");
        }

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado: " + id));

        return buildSystemPrompt(project);
    }

    public static RequestToolDefinition createTool() {
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
            throw new IllegalArgumentException("O parâmetro 'project_id' é obrigatório");
        }

        try {
            return Long.valueOf(projectIdRaw.trim());
        } catch (NumberFormatException e) {
            log.error("[PARSE_PROJECT_ID] Erro ao converter project_id", e);
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
        sb.append("- id: ").append(project.getId()).append("\n");
        sb.append("- sigla: ").append(safe(project.getSigla())).append("\n");
        sb.append("- nome: ").append(safe(project.getName())).append("\n");
        sb.append("- constitution:\n");
        sb.append(safeBlock(project.getConstitution())).append("\n");

        List<CodeRepo> repos = project.getRepos();
        if (repos == null || repos.isEmpty()) {
            sb.append("\n## Repositórios\n");
            sb.append("[nenhum repositório configurado]\n");
            return sb.toString().trim();
        }

        sb.append("\n## Repositórios\n");

        for (CodeRepo repo : repos) {
            log.trace("[BUILD_SYS_PROMPT] Processando repositório {}", repo.getName());

            sb.append("\n### Repositório\n");
            sb.append("- id: ").append(repo.getId()).append("\n");
            sb.append("- nome: ").append(safe(repo.getName())).append("\n");
            sb.append("- path: ").append(safe(repo.getPath())).append("\n");
            sb.append("- url: ").append(safe(repo.getUrl())).append("\n");
            sb.append("- branch: ").append(safe(repo.getBranch())).append("\n");
            sb.append("- tipo: ").append(repo.getType() != null ? repo.getType().name() : "[vazio]").append("\n");
            sb.append("- extensoesDeArquivosFonte: ").append(safe(repo.getExtensoesDeArquivosFonte())).append("\n");
            sb.append("- comandoCompilacao: ").append(safe(repo.getComandoCompilacao())).append("\n");
            sb.append("- constitution:\n");
            sb.append(safeBlock(repo.getConstitution())).append("\n");
            sb.append("- structure:\n");
            sb.append(safeBlock(repo.getStructure())).append("\n");
        }

        sb.append("""
                
                ## Regras de uso
                - Não invente classes, métodos, imports ou arquivos sem conferir o contexto real.
                - Use constitution e structure como fonte principal de verdade.
                - Respeite os caminhos e a linguagem de cada repositório.
                - Antes de alterar código existente, leia o arquivo completo.
                """);

        return sb.toString().trim();
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "[vazio]";
        }
        return value;
    }

    private String safeBlock(String value) {
        if (value == null || value.isBlank()) {
            return "[vazio]";
        }
        return value.strip();
    }
}