package com.example.springia.agent.tool.files;

import com.example.springia.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Ferramenta para buscar conteúdo em arquivos usando grep via Files.walk
 */
@Slf4j
public class GrepFilesTool implements Tool {

    private final String basePath;

    public GrepFilesTool(String basePath) {
        this.basePath = basePath;
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
        params.put("directory_path", "Caminho relativo do diretório onde buscar (opcional, padrão: raiz do projeto)");
        params.put("file_extension", "Filtrar apenas arquivos com esta extensão (opcional, ex: .java, .xml)");
        params.put("ignore_case", "Ignorar maiúsculas/minúsculas: true ou false (opcional, padrão: false)");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String pattern = params.get("pattern");
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("O parâmetro 'pattern' é obrigatório");
        }

        String dirPath = params.getOrDefault("directory_path", "");
        String fileExtension = params.getOrDefault("file_extension", "");
        boolean ignoreCase = Boolean.parseBoolean(params.getOrDefault("ignore_case", "false"));

        String fullPath = dirPath.isBlank() ? basePath : basePath + "/" + dirPath;
        Path rootPath = Paths.get(fullPath.replace("/tmp/tmp/", "/tmp/"));

        if (!Files.exists(rootPath)) {
            throw new IllegalArgumentException("Diretório não encontrado: " + dirPath);
        }
        if (!Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException("Caminho não é um diretório: " + dirPath);
        }

        int regexFlags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
        Pattern compiledPattern = Pattern.compile(pattern, regexFlags);

        AtomicInteger totalMatches = new AtomicInteger(0);
        StringBuilder result = new StringBuilder();

        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> fileExtension.isBlank() || p.toString().endsWith(fileExtension))
                .sorted()
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
                            String relativePath = rootPath.relativize(file).toString();
                            result.append("[FILE] ").append(relativePath).append("\n");
                            result.append(fileMatches);
                        }
                    } catch (IOException e) {
                        log.warn("[TOOL] grep_files: Não foi possível ler arquivo: {}", file, e);
                    }
                });
        }

        if (result.isEmpty()) {
            log.info("[TOOL] grep_files: nenhum resultado para '{}' em '{}'", pattern, fullPath);
            return "Nenhum resultado encontrado para o padrão: " + pattern;
        }

        log.info("[TOOL] grep_files: {} ocorrência(s) encontrada(s) para '{}' em '{}'",
                totalMatches.get(), pattern, fullPath);
        return "Total de ocorrências: " + totalMatches.get() + "\n\n" + result;
    }
}



