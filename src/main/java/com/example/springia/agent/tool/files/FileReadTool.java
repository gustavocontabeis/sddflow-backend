package com.example.springia.agent.tool.files;

import com.example.springia.config.AgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Le arquivos do projeto sob os diretorios permitidos.
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.files.FileReadTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileReadTool {

    private final AgentProperties agentProperties;

    @Tool(name = "read_file", description = "Lê o conteúdo de um arquivo existente nos diretorios permitidos")
    public String readFile(@ToolParam(description = "Caminho absoluto do arquivo a ler") String filePath) {
        log.info("{[READ_FILE]} lendo arquivo {}", filePath);
        Path normalizedFilePath = validateAllowedPath(filePath);

        try {
            return Files.readString(normalizedFilePath);
        } catch (IOException e) {
            log.error("{[READ_FILE]} falha ao ler arquivo {}", normalizedFilePath, e);
            throw new IllegalStateException("Nao foi possivel ler o arquivo: " + normalizedFilePath, e);
        }
    }


    private Path validateAllowedPath(String filePath) {
        log.debug("{[READ_VALIDATE]} validando caminho de leitura");
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Caminho nao pode ser nulo");
        }

        Path normalized = Path.of(filePath).toAbsolutePath().normalize();
        List<Path> allowedRoots = List.of(
                Path.of(agentProperties.getBackendRoot()).toAbsolutePath().normalize(),
                Path.of(agentProperties.getFrontendRoot()).toAbsolutePath().normalize()
        );

        boolean allowed = allowedRoots.stream().anyMatch(normalized::startsWith);
        if (!allowed) {
            throw new IllegalArgumentException("Caminho fora dos diretorios permitidos: " + normalized);
        }

        if (!Files.exists(normalized) || !Files.isRegularFile(normalized)) {
            throw new IllegalArgumentException("Arquivo invalido ou inexistente: " + normalized);
        }

        return normalized;
    }
}

