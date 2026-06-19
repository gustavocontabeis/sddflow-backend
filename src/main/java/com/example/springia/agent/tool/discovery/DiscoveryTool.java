package com.example.springia.agent.tool.discovery;

import com.example.springia.agent.tool.Tool;
import com.example.springia.agent.tool.files.GrepFilesTool;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.repository.CodeRepoRepository;
import com.example.springia.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ferramenta para descobrir informações em arquivos do projeto a partir da pergunta do usuário.
 * O fluxo usa o diagrama de classes salvo em structure para inferir o melhor critério de busca
 * e então delega a leitura do código-fonte para a GrepFilesTool.
 */
@Slf4j
public class DiscoveryTool implements Tool {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "as", "ao", "aos", "com", "como", "da", "das", "de", "do", "dos",
            "e", "em", "na", "nas", "no", "nos", "o", "os", "ou", "para", "por",
            "projeto", "repositorio", "repositorios", "repo", "repos", "id", "tem",
            "teve", "ter", "possui", "possuir", "contém", "contem", "existe", "temos",
            "arquivo", "arquivos", "classe", "classes", "atributo", "atributos", "campo",
            "campos", "sobre", "qual", "quais", "que", "se", "uma", "um", "uns", "umas"
    );

    private final ProjectRepository projectRepository;
    private final CodeRepoRepository codeRepoRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public DiscoveryTool(
            ProjectRepository projectRepository,
            CodeRepoRepository codeRepoRepository,
            ChatClient chatClient
    ) {
        this.projectRepository = projectRepository;
        this.codeRepoRepository = codeRepoRepository;
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "discovery_tool";
    }

    @Override
    public String getDescription() {
        return "Busca informações em arquivos de um projeto a partir de uma pergunta funcional. " +
                "Localiza o projeto, lê os structures dos repositórios, infere o critério de busca " +
                "e executa grep nos arquivos relevantes.";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("project_id", "ID do projeto a ser analisado (obrigatório)");
        params.put("question", "Pergunta sobre o código-fonte do projeto (obrigatória)");
        return params;
    }

    @org.springframework.ai.tool.annotation.Tool(
            name = "discovery_tool",
            description = "Descobre informações no código-fonte de um projeto a partir da pergunta do usuário"
    )
    public String discoveryTool(
            @org.springframework.ai.tool.annotation.ToolParam(description = "ID do projeto") Long projectId,
            @org.springframework.ai.tool.annotation.ToolParam(description = "Pergunta sobre os arquivos do projeto") String question
    ) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("project_id", projectId == null ? "" : String.valueOf(projectId));
        params.put("question", question == null ? "" : question);
        return execute(params);
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        Long projectId = parseProjectId(params.get("project_id"));
        String question = params.get("question");

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'question' é obrigatório");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado: " + projectId));

        List<CodeRepo> repos = codeRepoRepository.findByProjectId(projectId);
        if (repos == null || repos.isEmpty()) {
            throw new IllegalStateException("Projeto sem repositórios configurados para discovery_tool");
        }

        project.setRepos(repos);

        SearchCriteria criteria = null;
        StringBuilder grepResults = new StringBuilder();
        for (CodeRepo repo : repos) {
            String structuresContext = repo.getStructure();
            String defaultExtensions = inferDefaultExtensions(structuresContext);
            criteria = inferSearchCriteria(question, repo.getStructure(), defaultExtensions);

            Map<String, String> grepParams = new HashMap<>();
            grepParams.put("project_id", String.valueOf(projectId));
            grepParams.put("pattern", criteria.searchPattern());
            grepParams.put("file_extension", criteria.fileExtensions());
            grepParams.put("ignore_case", "true");

            String grepResult = new GrepFilesTool(projectRepository, codeRepoRepository).execute(grepParams);

            grepResults.append(grepResult);
            grepResults.append("\n");

        }

        return """
                Discovery do projeto %d (%s)
                Pergunta: %s

                Critério inferido:
                - classe: %s
                - atributo: %s
                - palavra-chave: %s
                - padrão grep: %s
                - justificativa: %s

                Resultado da busca:
                %s
                """.formatted(
                project.getId(),
                safe(project.getName()),
                question.strip(),
                safe(criteria.targetClass()),
                safe(criteria.targetAttribute()),
                safe(criteria.searchKeyword()),
                safe(criteria.searchPattern()),
                safe(criteria.rationale()),
                grepResults
        ).trim();
    }

    private Long parseProjectId(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'project_id' é obrigatório");
        }

        try {
            return Long.valueOf(projectId.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O parâmetro 'project_id' deve ser numérico: " + projectId, e);
        }
    }

    private String buildStructuresContext(List<CodeRepo> repos) {
        StringBuilder builder = new StringBuilder();

        for (CodeRepo repo : repos) {
            builder.append("\n--- REPOSITORIO ---\n")
                    .append("Nome: ").append(safe(repo.getName())).append("\n")
                    .append("Path: ").append(safe(repo.getPath())).append("\n")
                    .append("Structure:\n")
                    .append(repo.getStructure() == null || repo.getStructure().isBlank() ? "[vazio]" : repo.getStructure())
                    .append("\n");
        }

        return builder.toString().trim();
    }

    private SearchCriteria inferSearchCriteria(String question, String structuresContext, String defaultExtensions) {
        if (structuresContext == null || structuresContext.isBlank()) {
            return fallbackCriteria(question, defaultExtensions);
        }

        String prompt = """
                Você é um arquiteto de software especialista em leitura de diagramas de classes e busca em código-fonte.
                Sua tarefa é identificar o melhor critério de pesquisa para responder a pergunta do usuário.

                Regras:
                - Use o conteúdo de Structure como fonte principal.
                - Identifique a classe e o atributo mais prováveis relacionados à pergunta.
                - search_keyword deve ser o termo mais provável de existir no código-fonte.
                - search_pattern deve ser adequado para grep/regex Java simples.
                - file_extensions deve conter extensões separadas por vírgula.
                - Quando a pergunta for do tipo "X tem Y?", foque o search_keyword no atributo Y.
                - Se não houver evidência suficiente, faça a melhor inferência possível com base no diagrama.
                - Retorne somente JSON puro.

                JSON esperado:
                {
                  "target_class": "Tarefa",
                  "target_attribute": "prioridade",
                  "search_keyword": "prioridade",
                  "search_pattern": "prioridade",
                  "file_extensions": ".java",
                  "rationale": "A classe Tarefa possui o atributo prioridade no diagrama."
                }

                file_extensions padrão quando não souber: %s

                Pergunta:
                %s

                Structures:
                %s
                """.formatted(defaultExtensions, question, structuresContext);

        try {
            String content = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                return fallbackCriteria(question, defaultExtensions);
            }

            return parseCriteria(content, question, defaultExtensions);
        } catch (Exception e) {
            log.warn("[TOOL] discovery_tool: falha ao inferir critério com IA. Aplicando fallback. question={}", question, e);
            return fallbackCriteria(question, defaultExtensions);
        }
    }

    SearchCriteria parseCriteria(String content, String question, String defaultExtensions) throws Exception {
        String json = extractJsonObject(content);
        JsonNode node = objectMapper.readTree(json);

        String targetClass = text(node, "target_class");
        String targetAttribute = text(node, "target_attribute");
        String searchKeyword = text(node, "search_keyword");
        String searchPattern = text(node, "search_pattern");
        String fileExtensions = normalizeExtensions(text(node, "file_extensions"), defaultExtensions);
        String rationale = text(node, "rationale");

        SearchCriteria fallback = fallbackCriteria(question, defaultExtensions);

        return new SearchCriteria(
                blankOrDefault(targetClass, fallback.targetClass()),
                blankOrDefault(targetAttribute, fallback.targetAttribute()),
                blankOrDefault(searchKeyword, fallback.searchKeyword()),
                blankOrDefault(searchPattern, blankOrDefault(searchKeyword, fallback.searchPattern())),
                fileExtensions,
                blankOrDefault(rationale, fallback.rationale())
        );
    }

    static SearchCriteria fallbackCriteria(String question, String defaultExtensions) {
        List<String> keywords = extractKeywords(question);

        String targetClass = keywords.isEmpty() ? "[não identificado]" : capitalize(keywords.getFirst());
        String targetAttribute = keywords.size() > 1 ? keywords.getLast() : (keywords.isEmpty() ? question.strip() : keywords.getFirst());
        String searchKeyword = keywords.isEmpty() ? question.strip() : keywords.getLast();
        String rationale = "Critério inferido por fallback a partir dos termos mais relevantes da pergunta.";

        return new SearchCriteria(
                targetClass,
                targetAttribute,
                searchKeyword,
                searchKeyword,
                normalizeExtensions(defaultExtensions, ".java"),
                rationale
        );
    }

    static List<String> extractKeywords(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }

        String normalized = Normalizer.normalize(question, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{Alnum}_ ]", " ")
                .toLowerCase();

        List<String> keywords = new ArrayList<>();
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (STOP_WORDS.contains(token) || token.chars().allMatch(Character::isDigit)) {
                continue;
            }
            keywords.add(token);
        }
        return keywords;
    }

    static String normalizeExtensions(String rawExtensions, String defaultExtensions) {
        String source = rawExtensions == null || rawExtensions.isBlank() ? defaultExtensions : rawExtensions;
        if (source == null || source.isBlank()) {
            return ".java";
        }

        LinkedHashSet<String> normalized = Arrays.stream(source.split(","))
                .map(String::trim)
                .filter(ext -> !ext.isBlank())
                .map(ext -> ext.startsWith(".") ? ext.toLowerCase() : "." + ext.toLowerCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return normalized.isEmpty() ? ".java" : String.join(",", normalized);
    }

    static String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');

        if (start < 0 || end <= start) {
            throw new IllegalStateException("Nao foi encontrado JSON valido na resposta: " + content);
        }

        return content.substring(start, end + 1).trim();
    }

    private String inferDefaultExtensions(String structuresContext) {
        String structures = structuresContext == null ? "" : structuresContext.toLowerCase();

        if (structures.contains("package.json") || structures.contains("tsconfig.json")) {
            return ".ts,.tsx,.js,.jsx";
        }
        if (structures.contains("requirements.txt") || structures.contains("pyproject.toml")) {
            return ".py";
        }
        if (structures.contains(".csproj")) {
            return ".cs";
        }
        if (structures.contains("cargo.toml")) {
            return ".rs";
        }
        if (structures.contains("go.mod")) {
            return ".go";
        }
        if (structures.contains("pom.xml") || structures.contains("build.gradle") || structures.contains("src/main/java")) {
            return ".java";
        }

        return ".java";
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private static String blankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "[vazio]" : value;
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    public record SearchCriteria(
            String targetClass,
            String targetAttribute,
            String searchKeyword,
            String searchPattern,
            String fileExtensions,
            String rationale
    ) {
    }
}


