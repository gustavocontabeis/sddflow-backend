package com.example.springia.agent.tool.files;

import com.example.springia.agent.model.ChangeOperation;
import com.example.springia.agent.model.CodeDiffSummary;
import com.example.springia.agent.model.FileChangeCommand;
import com.example.springia.agent.model.FilePatchCommand;
import com.example.springia.agent.model.FileWriteResult;
import com.example.springia.agent.tool.diff.CodeDiffTool;
import com.example.springia.config.AgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Escreve alteracoes diretamente nos arquivos permitidos.
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.files.FileWriteTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileWriteTool {

    private static final DateTimeFormatter BACKUP_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final AgentProperties agentProperties;
    private final CodeDiffTool codeDiffTool;

    @Tool(name = "write_files", description = "Aplica alteracoes diretamente em arquivos permitidos com backup automatico")
    public List<FileWriteResult> write(
            @ToolParam(description = "Lista de alteracoes de arquivo a aplicar") List<FileChangeCommand> changes,
            @ToolParam(description = "Diretorio raiz para backups temporarios") String backupRoot
    ) {
        log.info("{[WRITE_FILE]} aplicando {} alteracoes", changes == null ? 0 : changes.size());
        if (changes == null || changes.isEmpty()) {
            return List.of();
        }

        List<FileWriteResult> results = new ArrayList<>();
        for (FileChangeCommand change : changes) {
            log.trace("{[WRITE_LOOP]} processando item de alteracao");
            results.add(writeOne(change, backupRoot));
        }
        return results;
    }

    private FileWriteResult writeOne(FileChangeCommand change, String backupRoot) {
        log.debug("{[WRITE_ONE]} iniciando escrita de arquivo");
        if (change == null) {
            throw new IllegalArgumentException("Alteracao nao pode ser nula");
        }

        Path filePath = validateAllowedPath(change.filePath());
        ChangeOperation operation = change.operation();
        log.debug("{[WRITE_FILE]} processando {} com operacao {}", filePath, operation);

        try {
            String before = Files.exists(filePath) ? Files.readString(filePath) : "";
            validateExpectedHash(change, before, filePath);
            String backupPath = null;
            if (Files.exists(filePath)) {
                backupPath = createBackup(filePath, backupRoot, before);
            }

            if (operation == ChangeOperation.DELETE) {
                Files.deleteIfExists(filePath);
                CodeDiffSummary diff = codeDiffTool.summarize(filePath.toString(), before, "");
                return new FileWriteResult(filePath.toString(), operation, true, backupPath, diff, "arquivo removido");
            }

            String updatedContent = resolveUpdatedContent(change, before, operation);

            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, updatedContent, StandardCharsets.UTF_8);
            CodeDiffSummary diff = codeDiffTool.summarize(filePath.toString(), before, updatedContent);
            return new FileWriteResult(filePath.toString(), operation, true, backupPath, diff, "arquivo gravado");
        } catch (IOException e) {
            log.error("{[WRITE_FILE]} falha ao escrever {}", filePath, e);
            throw new IllegalStateException("Nao foi possivel escrever o arquivo: " + filePath, e);
        }
    }

    private void validateExpectedHash(FileChangeCommand change, String before, Path filePath) {
        log.debug("{[WRITE_HASH]} validando hash esperado");
        if (change.expectedFileSha256() == null || change.expectedFileSha256().isBlank()) {
            return;
        }
        String currentHash = sha256(before == null ? "" : before);
        if (!currentHash.equalsIgnoreCase(change.expectedFileSha256().trim())) {
            throw new IllegalStateException("Hash divergente para arquivo: " + filePath);
        }
    }

    private String resolveUpdatedContent(FileChangeCommand change, String before, ChangeOperation operation) {
        log.debug("{[WRITE_UPD]} resolvendo conteudo de atualizacao");
        if (operation == ChangeOperation.CREATE) {
            if (change.content() == null) {
                throw new IllegalArgumentException("Conteudo nao pode ser nulo para create");
            }
            return change.content();
        }

        if (!change.patches().isEmpty()) {
            return applyPatches(before, change.patches());
        }

        if (change.content() != null && Boolean.TRUE.equals(change.allowFullReplace())) {
            return change.content();
        }

        if (change.content() != null) {
            throw new IllegalArgumentException("Substituicao completa bloqueada para update sem allowFullReplace=true");
        }

        throw new IllegalArgumentException("Update exige patches ou conteudo com allowFullReplace=true");
    }

    private String applyPatches(String baseContent, List<FilePatchCommand> patches) {
        log.debug("{[WRITE_PATCH]} aplicando {} patches", patches.size());
        String updated = baseContent == null ? "" : baseContent;
        for (FilePatchCommand patch : patches) {
            log.trace("{[PATCH_LOOP]} aplicando patch");
            updated = applySinglePatch(updated, patch);
        }
        return updated;
    }

    private String applySinglePatch(String content, FilePatchCommand patch) {
        log.debug("{[PATCH_ONE]} aplicando patch individual");
        if (patch == null || patch.oldText() == null || patch.oldText().isBlank() || patch.newText() == null) {
            throw new IllegalArgumentException("Patch invalido: oldText e newText sao obrigatorios");
        }

        int start;
        if (patch.occurrenceIndex() != null) {
            if (patch.occurrenceIndex() < 1) {
                throw new IllegalArgumentException("occurrenceIndex deve ser >= 1");
            }
            start = findOccurrenceStart(content, patch.oldText(), patch.occurrenceIndex());
            if (start < 0) {
                throw new IllegalStateException("Patch nao encontrado para occurrenceIndex=" + patch.occurrenceIndex());
            }
        } else {
            int count = countOccurrences(content, patch.oldText());
            if (count == 0) {
                throw new IllegalStateException("Patch nao encontrado no arquivo");
            }
            if (count > 1) {
                throw new IllegalStateException("Patch ambiguo: oldText aparece " + count + " vezes");
            }
            start = content.indexOf(patch.oldText());
        }

        int end = start + patch.oldText().length();
        return content.substring(0, start) + patch.newText() + content.substring(end);
    }

    private int countOccurrences(String content, String target) {
        log.debug("{[PATCH_COUNT]} contando ocorrencias de patch");
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }

    private int findOccurrenceStart(String content, String target, int occurrenceIndex) {
        log.debug("{[PATCH_FIND]} buscando ocorrencia especifica de patch");
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(target, index)) >= 0) {
            count++;
            if (count == occurrenceIndex) {
                return index;
            }
            index += target.length();
        }
        return -1;
    }

    private String sha256(String value) {
        log.debug("{[WRITE_SHA]} calculando hash sha256");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte hashByte : hashBytes) {
                hex.append(String.format("%02x", hashByte));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("{[WRITE_SHA]} erro ao calcular hash sha256", e);
            throw new IllegalStateException("Algoritmo SHA-256 indisponivel", e);
        }
    }

    private Path validateAllowedPath(String filePath) {
        log.debug("{[WRITE_VALID]} validando caminho de escrita");
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Caminho do arquivo nao pode ser nulo ou vazio");
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
        return normalized;
    }

    private String createBackup(Path filePath, String backupRoot, String currentContent) throws IOException {
        log.debug("{[WRITE_BACKUP]} criando backup de arquivo");
        Path root = backupRoot == null || backupRoot.isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "springia-backups")
                : Path.of(backupRoot).toAbsolutePath().normalize();
        Files.createDirectories(root);

        String backupFileName = filePath.getFileName() + "." + LocalDateTime.now().format(BACKUP_SUFFIX) + ".bak";
        Path backupFile = root.resolve(backupFileName);
        Files.writeString(backupFile, currentContent == null ? "" : currentContent, StandardCharsets.UTF_8);
        return backupFile.toString();
    }
}

