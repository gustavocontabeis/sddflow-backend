package com.example.springia.agent.tool.files;

import com.example.springia.agent.responseapi.request.RequestToolDefinition;
import com.example.springia.agent.responseapi.request.RequestToolParameters;
import com.example.springia.agent.responseapi.request.RequestToolProperty;
import com.example.springia.agent.tool.Tool;
import com.example.springia.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ferramenta para criar arquivos no filesystem
 */
@Slf4j
@Component
public class CreateFileTool implements Tool {

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
        params.put("file_path", "Caminho absoluto do arquivo a criar");
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

        String fullPath = filePath;
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

    public static RequestToolDefinition createTool(){
        return RequestToolDefinition.builder()
                .type("function")
                .name("create_file")
                .description("Cria um NOVO arquivo no filesystem (não sobrescreve arquivo existente)")
                .parameters(RequestToolParameters.builder()
                        .type("object")
                        .properties(Map.of(
                                "file_path", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Caminho absoluto do arquivo a criar")
                                        .build(),
                                "content", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Conteúdo do arquivo")
                                        .build()
                        ))
                        .required(List.of("file_path", "content"))
                        .additionalProperties(false)
                        .build())
                .strict(true)
                .build();
    }
}

