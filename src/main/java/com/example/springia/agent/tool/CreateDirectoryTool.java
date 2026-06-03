package com.example.springia.agent.tool;

import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Ferramenta para criar diretórios
 */
@Slf4j
public class CreateDirectoryTool implements Tool {

    private final String basePath;

    public CreateDirectoryTool(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public String getName() {
        return "create_directory";
    }

    @Override
    public String getDescription() {
        return "Cria um novo diretório com todos os diretórios pais necessários";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("directory_path", "Caminho relativo do diretório a criar (ex: src/main/java/com/example)");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String dirPath = params.get("directory_path");

        if (dirPath == null || dirPath.isBlank()) {
            throw new IllegalArgumentException("directory_path é obrigatório");
        }

        String fullPath = basePath + "/" + dirPath;
        var path = Paths.get(fullPath.replace("/tmp/tmp/", "/tmp/") );

        Files.createDirectories(path);

        log.info("[TOOL] Diretório criado: {}", fullPath);
        return "Diretório criado com sucesso: " + dirPath;
    }
}

