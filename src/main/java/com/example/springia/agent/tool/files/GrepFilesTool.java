package com.example.springia.agent.tool.files;

import com.example.springia.agent.tool.Tool;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

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
public class GrepFilesTool implements Tool {

    private final Project project;

    public GrepFilesTool(Project project) {
        this.project = project;
    }

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
        params.put("pattern", "Texto ou expressão regular a buscar nos arquivos (obrigatório)");
        params.put("file_extension", "Filtrar apenas arquivos com esta extensão (opcional, ex: .java, .xml)");
        params.put("ignore_case", "Ignorar maiúsculas/minúsculas: true ou false (opcional, padrão: false)");
        return params;
    }

    @org.springframework.ai.tool.annotation.Tool(name = "grep_files", description = "Busca padrão de texto em arquivos dos repositórios do projeto")
    public String grepFiles(
            @org.springframework.ai.tool.annotation.ToolParam(description = "Texto ou expressão regular a buscar") String pattern,
            @org.springframework.ai.tool.annotation.ToolParam(description = "Extensões separadas por vírgula (opcional). Ex: .java,.xml") String fileExtension,
            @org.springframework.ai.tool.annotation.ToolParam(description = "Ignorar maiúsculas/minúsculas (opcional). Ex: true") Boolean ignoreCase
    ) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("pattern", pattern);
        params.put("file_extension", fileExtension == null ? "" : fileExtension);
        params.put("ignore_case", Boolean.toString(Boolean.TRUE.equals(ignoreCase)));
        return execute(params);
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String pattern = params.get("pattern");
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'pattern' é obrigatório");
        }

        String fileExtension = params.getOrDefault("file_extension", "");
        boolean ignoreCase = Boolean.parseBoolean(params.getOrDefault("ignore_case", "false"));

        if (project == null) {
            throw new IllegalStateException("Projeto selecionado não informado para execução do grep_files");
        }

        List<CodeRepo> repos = project.getRepos();
        if (repos == null || repos.isEmpty()) {
            throw new IllegalStateException("Projeto sem repositórios configurados para execução do grep_files");
        }

        int regexFlags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
        Pattern compiledPattern = Pattern.compile(pattern, regexFlags);

        AtomicInteger totalMatches = new AtomicInteger(0);
        StringBuilder result = new StringBuilder();

        for (CodeRepo repo : repos) {
            Path path = Paths.get(FileUtils.fixPath(repo.getPath()));
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                log.warn("[TOOL] grep_files: caminho de repositório inválido: {}", path);
                continue;
            }

            try (Stream<Path> walk = Files.walk(path)) {
                walk.filter(Files::isRegularFile)
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
                                    String relativePath = path.relativize(file).toString();
                                    result.append("[FILE] ").append("(").append(repo.getName()).append(") ").append(relativePath).append("\n");
                                    result.append(fileMatches);
                                }
                            } catch (IOException e) {
                                log.warn("[TOOL] grep_files: Não foi possível ler arquivo: {}", file, e);
                            }
                        });
            }

        }

        if (result.isEmpty()) {
            log.info("[TOOL] grep_files: nenhum resultado para '{}'", pattern);
            return "Nenhum resultado encontrado para o padrão: " + pattern;
        }

        log.info("[TOOL] grep_files: {} ocorrência(s) encontrada(s) para '{}'", totalMatches.get(), pattern);
        log.info("[TOOL] '{}'", result);

        return "Total de ocorrências: " + totalMatches.get() + "\n\n" + result;
    }
}



