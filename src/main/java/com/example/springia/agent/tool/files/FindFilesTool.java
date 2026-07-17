package com.example.springia.agent.tool.files;

import com.example.springia.agent.responseapi.request.RequestToolDefinition;
import com.example.springia.agent.responseapi.request.RequestToolParameters;
import com.example.springia.agent.responseapi.request.RequestToolProperty;
import com.example.springia.agent.tool.Tool;
import com.example.springia.utils.FileUtils;
import dev.langchain4j.agent.tool.P;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ferramenta para listar arquivos e diretórios.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.files.FindFilesTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 * logging.level.com.example.springia.agent.tool.files.FindFilesTool=TRACE
 */
@Slf4j
@Component
public class FindFilesTool implements Tool {

    private static final List<String> IGNORED_DIRECTORIES = List.of("node_modules", "target", ".git");

    @Override
    public String getName() {
        log.info("[GET_NAME] Retornando nome da tool");
        return "find_files";
    }

    @Override
    public String getDescription() {
        log.info("[GET_DESCRIPTION] Retornando descricao da tool");
        return "Busca arquivos por nome, recursivamente, a partir de um diretório específico";
    }

    @Override
    public Map<String, String> getParameters() {
        log.info("[GET_PARAMETERS] Montando parametros da tool");
        Map<String, String> params = new HashMap<>();
        params.put("directory_path", "Path do diretório raiz da busca - OBRIGATORIO");
        params.put("file_name", "Nome do arquivo para busca exata (ignora maiúsculas/minúsculas) - OBRIGATORIO");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        log.info("[EXECUTE] Iniciando busca de arquivos");
        String dirPath = params.getOrDefault("directory_path", "").trim();
        String fileName = params.getOrDefault("file_name", "").trim();

        if (dirPath.isBlank()) {
            throw new IllegalArgumentException("directory_path é obrigatório");
        }

        if (fileName.isBlank()) {
            throw new IllegalArgumentException("file_name é obrigatório");
        }

        var path = Paths.get(FileUtils.fixPath(dirPath));

        if (!Files.exists(path)) {
            return "[Diretório não encontrado: " + dirPath + "]";
            //throw new IllegalArgumentException("Diretório não encontrado: " + dirPath);
        }

        if (!Files.isDirectory(path)) {
            return "[Caminho não é um diretório: " + dirPath + "]";
        }

        List<String> itens;
        try (var stream = Files.walk(path)) {
            var normalizedFileName = fileName.toLowerCase();

            itens = stream
                    .filter(Files::isRegularFile)
                    .filter(current -> !isInsideIgnoredDirectory(path, current))
                    .filter(current -> current.getFileName().toString().toLowerCase().equals(normalizedFileName))
                    .peek(current -> log.trace("[EXECUTE] Arquivo candidato: {}", current))
                    .sorted()
                    .map(current -> "[FILE] " + path.relativize(current).toString().replace("\\", "/"))
                    .collect(Collectors.toList());
        }

        String result = String.join("\n", itens);
        log.info("[EXECUTE] Busca por arquivo '{}' em {} retornou {} itens", fileName, path, itens.size());
        return result;
    }

    private boolean isInsideIgnoredDirectory(Path rootPath, Path filePath) {
        log.debug("[IS_IGNORED_DIR] Verificando diretorio ignorado para {}", filePath);
        Path relativePath;
        try {
            relativePath = rootPath.relativize(filePath);
        } catch (IllegalArgumentException e) {
            log.error("[IS_IGNORED_ERROR] Falha ao relativizar caminho {}", filePath, e);
            return false;
        }

        for (Path segment : relativePath) {
            String segmentName = segment.toString();
            log.trace("[IS_IGNORED_DIR] Segmento analisado: {}", segmentName);

            // Verifica se é diretório oculto (começa com ponto)
            if (segmentName.startsWith(".")) {
                log.trace("[IS_IGNORED_DIR] Diretorio oculto encontrado: {}", segmentName);
                return true;
            }

            // Verifica se é um dos diretórios ignorados
            if (IGNORED_DIRECTORIES.contains(segmentName)) {
                log.trace("[IS_IGNORED_DIR] Diretorio ignorado encontrado: {}", segmentName);
                return true;
            }
        }
        return false;
    }

    public static RequestToolDefinition createTool(){
        log.info("[CREATE_TOOL] Montando definicao da tool find_files");
        return RequestToolDefinition.builder()
                .type("function")
                .name("find_files")
                .description("Busca arquivos por nome, recursivamente, a partir de um diretório específico")
                .parameters(RequestToolParameters.builder()
                        .type("object")
                        .properties(Map.of(
                                "directory_path", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Path do diretório raiz da busca")
                                        .build(),
                                "file_name", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Nome do arquivo para busca exata (ignora maiúsculas/minúsculas)")
                                        .build()
                        ))
                        .required(List.of("directory_path", "file_name"))
                        .additionalProperties(false)
                        .build())
                .strict(true)
                .build();
    }

    @dev.langchain4j.agent.tool.Tool(name = "find_files", value = "Busca arquivos por nome, recursivamente")
    public String findFiles(
            @P(value = "Path do diretório raiz da busca") String directoryPath,
            @P(value = "Nome do arquivo para busca exata") String fileName
    ) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("directory_path", directoryPath == null ? "" : directoryPath);
        params.put("file_name", fileName == null ? "" : fileName);
        return execute(params);
    }

}
