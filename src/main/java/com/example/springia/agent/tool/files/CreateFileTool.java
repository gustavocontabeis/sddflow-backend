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
 * Ferramenta para criar arquivos no filesystem.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.files.CreateFileTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 * logging.level.com.example.springia.agent.tool.files.CreateFileTool=TRACE
 */
@Slf4j
@Component
public class CreateFileTool implements Tool {

    @Override
    public String getName() {
        log.info("[GET_NAME] Retornando nome da tool");
        return "create_file";
    }

    @Override
    public String getDescription() {
        log.info("[GET_DESCRIPTION] Retornando descricao da tool");
        return "Cria um NOVO arquivo no filesystem (não sobrescreve arquivo existente)";
    }

    @Override
    public Map<String, String> getParameters() {
        log.info("[GET_PARAMETERS] Montando parametros da tool");
        Map<String, String> params = new HashMap<>();
        params.put("file_path", "Caminho absoluto do arquivo a criar");
        params.put("content", "Conteúdo do arquivo");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        log.info("[EXECUTE] Iniciando criacao de arquivo");
        try {
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

            Files.deleteIfExists(path);

            // Cria diretórios pais se não existirem
            Files.createDirectories(path.getParent());

            // Cria o arquivo apenas quando ele ainda não existe
            Files.writeString(path, content);

            log.info("[EXECUTE] Arquivo criado: {}", path);
            return "Arquivo criado com sucesso: " + path;
        } catch (Exception ex) {
            log.error("[EXECUTE_ERROR] Erro ao criar arquivo", ex);
            throw ex;
        }
    }

    public static RequestToolDefinition createTool(){
        log.info("[CREATE_TOOL] Montando definicao da tool create_file");
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

