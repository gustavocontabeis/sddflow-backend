package com.example.springia.agent.tool.files;

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
 * Ferramenta para buscar conteúdo em arquivos usando grep via Files.walk
 */
@Slf4j
@Component
public class GrepFilesTool implements Tool {

    @Override
    public String getName() {
        return "grep_files";
    }

    @Override
    public String getDescription() {
        return "Busca recursivamente por um padrão de texto em arquivos dentro de um diretório usando grep (Files.walk)";
    }

    @Override
    public Map<String, String> getParameters() {
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
        Map<String, String> params = new HashMap<>();
        params.put("path", pathStr);
        params.put("pattern", pattern);
        params.put("file_extension", fileExtension == null ? "" : fileExtension);
        params.put("ignore_case", Boolean.toString(Boolean.TRUE.equals(ignoreCase)));
        return execute(params);
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
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

        log.info("[TOOL] grep_files: localizando em {} arquivos tipo {} que contenha '{}'", path, fileExtension, pattern, compiledPattern);

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
                        try {
                            var lines = Files.readAllLines(file);
                            StringBuilder fileMatches = new StringBuilder();
                            for (int i = 0; i < lines.size(); i++) {
                                if (compiledPattern.matcher(lines.get(i)).find()) {
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
                            log.warn("[TOOL] grep_files: Não foi possível ler arquivo: {}", file, e);
                        }
                    });
            }


        if (result.isEmpty()) {
            log.info("[TOOL] grep_files: nenhum resultado para '{}'", pattern);
            return "Nenhum resultado encontrado para o padrão: " + pattern;
        }

        log.info("[TOOL] grep_files: {} ocorrência(s) encontrada(s) para '{}'", totalMatches.get(), pattern);
        log.info("[TOOL] grep_files: \n{}", result);

        return "Total de ocorrências: " + totalMatches.get() + "\n\n" + result;
    }

    private boolean isInsideHiddenDirectory(Path rootPath, Path filePath) {
        Path relativePath;
        try {
            relativePath = rootPath.relativize(filePath);
        } catch (IllegalArgumentException e) {
            // Se não for possível relativizar, mantém o arquivo elegível.
            return false;
        }

        for (Path segment : relativePath) {
            if (segment.toString().startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    private Long parseProjectId(Object projectIdObj) {
        String projectId = projectIdObj.toString();
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'project_id' é obrigatório");
        }

        try {
            return Long.valueOf(projectId.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O parâmetro 'project_id' deve ser numérico: " + projectId, e);
        }
    }

    private boolean parseBooleanParam(Object rawValue) {
        if (rawValue == null) {
            return false;
        }
        if (rawValue instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(rawValue.toString().trim());
    }
}



