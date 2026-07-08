package com.example.springia.agent.tool.files;

import com.example.springia.agent.responseapi.request.RequestToolDefinition;
import com.example.springia.agent.responseapi.request.RequestToolParameters;
import com.example.springia.agent.responseapi.request.RequestToolProperty;
import com.example.springia.agent.tool.Tool;
import com.example.springia.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ferramenta para criar diretórios.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.files.CreateDirectoryTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 * logging.level.com.example.springia.agent.tool.files.CreateDirectoryTool=TRACE
 */
@Slf4j
public class CreateDirectoryTool implements Tool {

    @Override
    public String getName() {
        log.info("[GET_NAME] Retornando nome da tool");
        return "create_directory";
    }

    @Override
    public String getDescription() {
        log.info("[GET_DESCRIPTION] Retornando descricao da tool");
        return "Cria um novo diretório com todos os diretórios pais necessários";
    }

    @Override
    public Map<String, String> getParameters() {
        log.info("[GET_PARAMETERS] Montando parametros da tool");
        Map<String, String> params = new HashMap<>();
        params.put("directory_path", "Caminho absoluto do diretório a criar");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        log.info("[EXECUTE] Iniciando criacao de diretorio");
        String dirPath = params.get("directory_path");

        if (dirPath == null || dirPath.isBlank()) {
            throw new IllegalArgumentException("directory_path é obrigatório");
        }

        var path = Paths.get(FileUtils.fixPath(dirPath));

        Files.createDirectories(path);

        log.info("[EXECUTE] Diretorio criado: {}", path);
        return "Diretório criado com sucesso: " + path;
    }

    public static RequestToolDefinition createTool(){
        log.info("[CREATE_TOOL] Montando definicao da tool create_directory");
        return RequestToolDefinition.builder()
                .type("function")
                .name("create_directory")
                .description("Cria um novo diretório com todos os diretórios pais necessários")
                .parameters(RequestToolParameters.builder()
                        .type("object")
                        .properties(Map.of(
                                "directory_path", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Caminho absoluto do diretório a criar")
                                        .build()
                        ))
                        .required(List.of("directory_path"))
                        .additionalProperties(false)
                        .build())
                .strict(true)
                .build();
    }
}

