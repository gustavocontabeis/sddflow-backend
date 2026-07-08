package com.example.springia.agent.tool.files;

import com.example.springia.agent.responseapi.request.RequestToolDefinition;
import com.example.springia.agent.responseapi.request.RequestToolParameters;
import com.example.springia.agent.responseapi.request.RequestToolProperty;
import com.example.springia.agent.tool.Tool;
import com.example.springia.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Ferramenta para editar arquivos existentes sobrescrevendo todo o conteúdo.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.files.UpdateFileTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 * logging.level.com.example.springia.agent.tool.files.UpdateFileTool=TRACE
 */
@Slf4j
public class UpdateFileTool implements Tool {

    @Override
    public String getName() {
        log.info("[GET_NAME] Retornando nome da tool");
        return "update_file";
    }

    @Override
    public String getDescription() {
        log.info("[GET_DESCRIPTION] Retornando descricao da tool");
        return "Atualiza arquivo existente sobrescrevendo todo o conteúdo";
    }

    @Override
    public Map<String, String> getParameters() {
        log.info("[GET_PARAMETERS] Montando parametros da tool");
        return Map.of(
                "file_path", "Caminho do arquivo a alterar (caminho absoluto) OBRIGATORIO",
                "content", "Novo conteudo completo que sera gravado no arquivo OBRIGATORIO"
        );
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        log.info("[EXECUTE] Iniciando atualizacao de arquivo");
        try {
            String filePath = params.get("file_path");
            String content = params.get("content");

            if (filePath == null || filePath.isBlank()) {
                log.warn("[EXECUTE] file_path obrigatorio");
                throw new IllegalArgumentException("file_path e obrigatorio");
            }
            if (content == null) {
                log.warn("[EXECUTE] content obrigatorio");
                throw new IllegalArgumentException("content e obrigatorio");
            }

            var path = Paths.get(FileUtils.fixPath(filePath));
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                log.warn("[EXECUTE] Arquivo nao encontrado: {}", path);
                throw new IllegalArgumentException("Arquivo nao encontrado: " + path);
            }

            Files.writeString(path, content);
            log.info("[EXECUTE] Arquivo sobrescrito com sucesso: {}", path);
            return "Arquivo atualizado com sucesso: " + path;
        } catch (Exception e) {
            log.error("[EXECUTE] Erro ao atualizar arquivo", e);
            throw e;
        }
    }

    public static RequestToolDefinition createTool(){
        log.info("[CREATE_TOOL] Montando definicao da tool update_file");
        return RequestToolDefinition.builder()
                .type("function")
                .name("update_file")
                .description("Atualiza arquivo existente sobrescrevendo todo o conteúdo")
                .parameters(RequestToolParameters.builder()
                        .type("object")
                        .properties(Map.of(
                                "file_path", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Caminho do arquivo a alterar (caminho absoluto) OBRIGATORIO")
                                        .build(),
                                "content", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Novo conteudo completo que sera gravado no arquivo")
                                        .build()

                        ))
                        .required(List.of("file_path", "content"))
                        .additionalProperties(false)
                        .build())
                .strict(true)
                .build();
    }
}

