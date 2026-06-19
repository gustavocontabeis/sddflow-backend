package com.example.springia.agent.tool.files;

import com.example.springia.agent.tool.Tool;
import com.example.springia.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Ferramenta para criar arquivos no filesystem
 */
@Slf4j
public class CreateFileTool implements Tool {

    private final String basePath;

    public CreateFileTool(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public String getName() {
        return "create_file";
    }

    @Override
    public String getDescription() {
        return "Cria um NOVO arquivo no filesystem (não sobrescreve arquivo existente)";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("file_path", "Caminho do arquivo a criar (relativo ao basePath ou absoluto)");
        params.put("content", "Conteúdo do arquivo");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String filePath = params.get("file_path");
        String content = params.get("content");

        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("file_path é obrigatório");
        }
        if (content == null) {
            content = "";
        }

        String fullPath = basePath + "/" + filePath;
        var path = Paths.get(FileUtils.fixPath(fullPath));

        if (Files.exists(path)) {
            throw new IllegalArgumentException(
                    "Arquivo já existe: " + path + ". Use a tool update_file para alterar apenas as linhas necessárias."
            );
        }

        // Cria diretórios pais se não existirem
        Files.createDirectories(path.getParent());

        // Cria o arquivo apenas quando ele ainda não existe
        Files.writeString(path, content);

        log.info("[TOOL] Arquivo criado: {}", path);
        return "Arquivo criado com sucesso: " + path;
    }
}

