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
 * Ferramenta para ler arquivos do filesystem.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.files.ReadFileTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 * logging.level.com.example.springia.agent.tool.files.ReadFileTool=TRACE
 */
@Slf4j
@Component
public class ReadFileTool implements Tool {

    @Override
    public String getName() {
        log.info("[GET_NAME] Retornando nome da tool");
        return "read_file";
    }

    @Override
    public String getDescription() {
        log.info("[GET_DESCRIPTION] Retornando descricao da tool");
        return "Lê o conteúdo de um arquivo existente no filesystem";
    }

    @Override
    public Map<String, String> getParameters() {
        log.info("[GET_PARAMETERS] Montando parametros da tool");
        Map<String, String> params = new HashMap<>();
        params.put("file_path", "Caminho absoluto do arquivo a ler");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        log.info("[EXECUTE] Iniciando leitura de arquivo");
        String filePath = params.get("file_path");

        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("file_path é obrigatório");
        }

        var path = Paths.get(FileUtils.fixPath(filePath));

        if (!Files.exists(path)) {
            return "[Arquivo não encontrado: "+filePath+"]"; //throw new IllegalArgumentException("Arquivo não encontrado: " + filePath);
        }

        String content = Files.readString(path);
        log.info("[EXECUTE] Arquivo lido: {} ({} bytes)", path, content.length());
        return content;
    }

    public static RequestToolDefinition createTool(){
        log.info("[CREATE_TOOL] Montando definicao da tool read_file");
        return RequestToolDefinition.builder()
                .type("function")
                .name("read_file")
                .description("Lê o conteúdo de um arquivo existente no filesystem")
                .parameters(RequestToolParameters.builder()
                        .type("object")
                        .properties(Map.of(
                                "file_path", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Caminho absoluto do arquivo a ler")
                                        .build()
                        ))
                        .required(List.of("file_path"))
                        .additionalProperties(false)
                        .build())
                .strict(true)
                .build();
    }

}
