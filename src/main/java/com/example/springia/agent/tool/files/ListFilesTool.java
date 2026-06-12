package com.example.springia.agent.tool.files;

import com.example.springia.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ferramenta para listar arquivos e diretórios
 */
@Slf4j
public class ListFilesTool implements Tool {

    private final String basePath;

    public ListFilesTool(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public String getName() {
        return "list_files";
    }

    @Override
    public String getDescription() {
        return "Lista arquivos e diretórios em um caminho específico";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("directory_path", "Caminho relativo do diretório a listar (ex: src/main/java). Se vazio, lista o diretório base.");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String dirPath = params.getOrDefault("directory_path", "");

        String fullPath = dirPath.isBlank() ? basePath : basePath + "/" + dirPath;
        var path = Paths.get(fullPath.replace("/tmp/tmp/", "/tmp/"));

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Diretório não encontrado: " + dirPath);
        }

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Caminho não é um diretório: " + dirPath);
        }

        File[] files = new File(fullPath).listFiles();
        if (files == null) {
            throw new IllegalArgumentException("Erro ao listar diretório: " + dirPath);
        }

        String result = Arrays.stream(files)
                .map(f -> (f.isDirectory() ? "[DIR] " : "[FILE] ") + f.getName())
                .collect(Collectors.joining("\n"));

        log.info("[TOOL] Diretório listado: {} ({} itens)", fullPath, files.length);
        return result;
    }
}

