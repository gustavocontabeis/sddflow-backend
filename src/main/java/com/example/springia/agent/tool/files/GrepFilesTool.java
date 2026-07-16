package com.example.springia.agent.tool.files;

import com.example.springia.agent.responseapi.request.RequestToolDefinition;
import com.example.springia.agent.responseapi.request.RequestToolParameters;
import com.example.springia.agent.responseapi.request.RequestToolProperty;
import com.example.springia.agent.tool.Tool;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.repository.CodeRepoRepository;
import com.example.springia.repository.ProjectRepository;
import com.example.springia.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Ferramenta para buscar conteúdo em arquivos usando grep via Files.walk.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.files.GrepFilesTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 * logging.level.com.example.springia.agent.tool.files.GrepFilesTool=TRACE
 */
@Slf4j
@Component
public class GrepFilesTool implements Tool {

    private static final List<String> IGNORED_DIRECTORIES = List.of("node_modules", "target", ".git");

    @Override
    public String getName() {
        log.info("[GET_NAME] Retornando nome da tool");
        return "grep_files";
    }

    @Override
    public String getDescription() {
        log.info("[GET_DESCRIPTION] Retornando descricao da tool");
        return "Busca recursivamente por um padrão de texto em arquivos dentro de um diretório usando grep (Files.walk)";
    }

    @Override
    public Map<String, String> getParameters() {
        log.info("[GET_PARAMETERS] Montando parametros da tool");
        Map<String, String> params = new HashMap<>();
        params.put("path", "Path do diretório raiz da busca (obrigatório)");
        params.put("pattern", "Texto ou expressão regular a buscar nos arquivos (obrigatório)");
        params.put("file_extension", "Filtrar apenas arquivos com esta extensão (opcional, ex: .java, .xml)");
        params.put("ignore_case", "Ignorar maiúsculas/minúsculas: true ou false (opcional, padrão: false)");
        return params;
    }

    @org.springframework.ai.tool.annotation.Tool(name = "grep_files", description = "Busca padrão de texto em arquivos dos repositórios do projeto")
    public String grepFiles(
            @org.springframework.ai.tool.annotation.ToolParam(description = "Path do diretório raiz da busca") String pathStr,
            @org.springframework.ai.tool.annotation.ToolParam(description = "Texto ou expressão regular a buscar") String pattern,
            @org.springframework.ai.tool.annotation.ToolParam(description = "Extensões separadas por vírgula (opcional). Ex: .java,.xml") String fileExtension,
            @org.springframework.ai.tool.annotation.ToolParam(description = "Ignorar maiúsculas/minúsculas (opcional). Ex: true") Boolean ignoreCase
    ) throws Exception {
        log.info("[GREP_FILES] Iniciando chamada via anotacao tool");
        Map<String, String> params = new HashMap<>();
        params.put("path", pathStr);
        params.put("pattern", pattern);
        params.put("file_extension", fileExtension == null ? "" : fileExtension);
        params.put("ignore_case", Boolean.toString(Boolean.TRUE.equals(ignoreCase)));
        return execute(params);
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        log.info("[EXECUTE] Iniciando busca por padrao em arquivos");
        String pathStr = params.get("path");

        String pattern = params.get("pattern");
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'pattern' é obrigatório");
        }

        String fileExtension = params.getOrDefault("file_extension", "");
        Object ignoreCaseRaw = ((Map<?, ?>) params).get("ignore_case");
        boolean ignoreCase = parseBooleanParam(ignoreCaseRaw);

        int regexFlags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
        Pattern compiledPattern = Pattern.compile(pattern, regexFlags);

        AtomicInteger totalMatches = new AtomicInteger(0);
        StringBuilder result = new StringBuilder();

        Path path = Paths.get(FileUtils.fixPath(pathStr));

        log.debug("[EXECUTE] Localizando em {} arquivos tipo {} que contenham '{}'", path, fileExtension, pattern);

        try (Stream<Path> walk = Files.walk(path)) {
            walk.filter(Files::isRegularFile)
                    .filter(file -> !isInsideHiddenDirectory(path, file))
                    .filter(p -> {
                        if (fileExtension.isBlank()) {
                            return true;
                        }
                        String name = p.getFileName().toString().toLowerCase();
                        return Arrays.stream(fileExtension.split(","))
                                .map(String::trim)
                                .filter(ext -> !ext.isBlank())
                                .map(String::toLowerCase)
                                .anyMatch(name::endsWith);
                    })                .sorted()

                    .forEach(file -> {
                        log.trace("[EXECUTE] Processando arquivo {}", file);
                        try {
                            var lines = Files.readAllLines(file);
                            StringBuilder fileMatches = new StringBuilder();
                            for (int i = 0; i < lines.size(); i++) {
                                if (compiledPattern.matcher(lines.get(i)).find()) {
                                    log.trace("[EXECUTE] Match em arquivo {} linha {}", file, i + 1);
                                    fileMatches.append("  linha ").append(i + 1)
                                            .append(": ").append(lines.get(i).strip())
                                            .append("\n");
                                    totalMatches.incrementAndGet();
                                }
                            }
                            if (!fileMatches.isEmpty()) {
                                //String relativePath = path.relativize(file).toString();
                                String relativePath = file.toString();
                                result.append("[FILE] ").append("(").append(pathStr).append(") ").append(relativePath).append("\n");
                                result.append(fileMatches);
                            }
                        } catch (IOException e) {
                            log.error("[EXECUTE_ERROR] Falha ao ler arquivo {}", file, e);
                        }
                    });
            }


        if (result.isEmpty()) {
            log.info("[EXECUTE] Nenhum resultado para '{}'", pattern);
            return "Nenhum resultado encontrado para o padrão: " + pattern;
        }

        log.info("[EXECUTE] {} ocorrencia(s) encontrada(s) para '{}'", totalMatches.get(), pattern);
        log.debug("[EXECUTE] Resultado consolidado:\n{}", result);

        return "Total de ocorrências: " + totalMatches.get() + "\n\n" + result;
    }

    private boolean isInsideHiddenDirectory(Path rootPath, Path filePath) {
        log.debug("[IS_HIDDEN_DIR] Verificando diretorio oculto para {}", filePath);
        Path relativePath;
        try {
            relativePath = rootPath.relativize(filePath);
        } catch (IllegalArgumentException e) {
            // Se não for possível relativizar, mantém o arquivo elegível.
            log.error("[IS_HIDDEN_ERROR] Falha ao relativizar caminho {}", filePath, e);
            return false;
        }

        for (Path segment : relativePath) {
            String segmentName = segment.toString();
            log.trace("[IS_HIDDEN_DIR] Segmento analisado: {}", segmentName);

            // Verifica se é diretório oculto (começa com ponto)
            if (segmentName.startsWith(".")) {
                log.trace("[IS_HIDDEN_DIR] Diretorio oculto encontrado: {}", segmentName);
                return true;
            }

            // Verifica se é um dos diretórios ignorados
            if (IGNORED_DIRECTORIES.contains(segmentName)) {
                log.trace("[IS_HIDDEN_DIR] Diretorio ignorado encontrado: {}", segmentName);
                return true;
            }
        }
        return false;
    }

    private Long parseProjectId(Object projectIdObj) {
        log.debug("[PARSE_PROJECT_ID] Convertendo project_id");
        String projectId = projectIdObj.toString();
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'project_id' é obrigatório");
        }

        try {
            return Long.valueOf(projectId.trim());
        } catch (NumberFormatException e) {
            log.error("[PARSE_ID_ERROR] project_id invalido: {}", projectId, e);
            throw new IllegalArgumentException("O parâmetro 'project_id' deve ser numérico: " + projectId, e);
        }
    }

    private boolean parseBooleanParam(Object rawValue) {
        log.debug("[PARSE_BOOLEAN] Convertendo parametro booleano");
        if (rawValue == null) {
            return false;
        }
        if (rawValue instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(rawValue.toString().trim());
    }

    public static RequestToolDefinition createTool(){
        log.info("[CREATE_TOOL] Montando definicao da tool grep_files");
        return RequestToolDefinition.builder()
                .type("function")
                .name("grep_files")
                .description("Busca recursivamente por um padrão de texto no conteúdo de um arquivo dentro de um diretório")
                .parameters(RequestToolParameters.builder()
                        .type("object")
                        .properties(Map.of(
                                "path", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Path do diretório raiz da busca")
                                        .build(),
                                "pattern", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Texto ou expressão regular a buscar")
                                        .build(),
                                "file_extension", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Extensões separadas por vírgula (opcional). Ex: .java,.xml")
                                        .build(),
                                "ignore_case", RequestToolProperty.builder()
                                        .type("boolean")
                                        .description("Ignorar maiúsculas/minúsculas (opcional). Ex: true")
                                        .build()
                        ))
                        .required(List.of("path", "pattern", "file_extension", "ignore_case"))
                        .additionalProperties(false)
                        .build())
                .strict(true)
                .build();
    }
}

