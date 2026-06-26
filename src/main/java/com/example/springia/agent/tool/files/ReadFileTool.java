package com.example.springia.agent.tool.files;

import com.example.springia.agent.tool.Tool;
import com.example.springia.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Ferramenta para ler arquivos do filesystem
 */
@Slf4j
public class ReadFileTool implements Tool {

    @Override
    public String getName() {
        return "read_file";
    }

    @Override
    public String getDescription() {
        return "Lê o conteúdo de um arquivo existente no filesystem";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("file_path", "Caminho absoluto do arquivo a ler");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String filePath = params.get("file_path");

        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("file_path é obrigatório");
        }

        var path = Paths.get(FileUtils.fixPath(filePath));

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Arquivo não encontrado: " + filePath);
        }

        String content = Files.readString(path);
        log.info("[TOOL] Arquivo lido: {} ({}bytes)", path, content.length());
        return content;
    }
}

