package com.example.springia.service;

import com.example.springia.agent.model.CompilationResult;
import com.example.springia.agent.model.CompileBy;
import com.example.springia.agent.model.CodeDiffSummary;
import com.example.springia.agent.model.FileChangeCommand;
import com.example.springia.agent.model.FileWriteResult;
import com.example.springia.agent.model.GeneratedChangeSet;
import com.example.springia.agent.model.ProjectDiscoverySnapshot;
import com.example.springia.agent.tool.compiler.CompilationTool;
import com.example.springia.agent.tool.discovery.ProjectDiscoveryTool;
import com.example.springia.agent.tool.feedback.FeedbackTool;
import com.example.springia.agent.tool.files.FileWriteTool;
import com.example.springia.config.AgentProperties;
import com.example.springia.dto.ArtifactChangeResponse;
import com.example.springia.dto.AttemptResponse;
import com.example.springia.dto.ExecutionRequest;
import com.example.springia.dto.ExecutionResponse;
import com.example.springia.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openai.errors.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Orquestra discovery, geração de prompt, escrita de arquivos, compilação e feedback.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.service.AgentExecutionService" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionService {

    private static final String STATUS_SUCESSO = "SUCESSO";
    private static final String STATUS_FALHA = "FALHO_MAX_TENTATIVAS";
    private static final String STATUS_FALHA_MODELO = "FALHA_MODELO_NAO_SUPORTADO";

    private final AgentProperties agentProperties;
    private final ProjectDiscoveryTool projectDiscoveryTool;
    private final FileWriteTool fileWriteTool;
    private final CompilationTool compilationTool;
    private final FeedbackTool feedbackTool;

    public ExecutionResponse execute(ExecutionRequest request) {
        log.info("[EXECUTE] iniciando execucao taskDescription='{}' compileBy='{}'",
                request == null ? null : request.taskDescription(),
                request == null || request.compileBy() == null ? null : request.compileBy().name());
        validateRequest(request);

        LocalDateTime startedAt = LocalDateTime.now();
        int maxAttempts = agentProperties.getMaxIterations() == null || agentProperties.getMaxIterations() < 1
                ? 1
                : agentProperties.getMaxIterations();

        ProjectDiscoverySnapshot snapshot = projectDiscoveryTool.discover();
        String prompt = buildInitialPrompt(request.taskDescription(), snapshot);

        String finalStatus = STATUS_FALHA;
        String finalSummary = "";
        String finalResult = "";
        String feedback = "";
        List<AttemptResponse> attempts = new ArrayList<>();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            log.trace("[EXEC_LOOP] tentativa={} de {}", attempt, maxAttempts);
            LocalDateTime attemptStart = LocalDateTime.now();
            String currentPrompt = prompt;
            String previousFeedback = feedback;
            String llmResponse = "";
            GeneratedChangeSet changeSet = new GeneratedChangeSet(null, null, List.of(), List.of());
            List<FileWriteResult> writeResults = List.of();
            List<CompilationResult> compilationResults = List.of();
            String attemptStatus = "EM_REPARO";
            String attemptError = null;
            boolean success = false;
            boolean nonRetryable = false;

            try {
                llmResponse = generate(currentPrompt);
                changeSet = parseGeneratedChangeSet(llmResponse);

                validateScope(changeSet.changes(), snapshot);
                writeResults = fileWriteTool.write(changeSet.changes(), resolveBackupRoot());
                compilationResults = compileProjects(request.compileBy(), changeSet);

                if (isSuccessful(compilationResults)) {
                    attemptStatus = "VALIDADO";
                    finalStatus = STATUS_SUCESSO;
                    finalSummary = safeValue(changeSet.summary());
                    finalResult = llmResponse;
                    success = true;
                    log.info("[EXEC_DONE] execucao validada na tentativa={}", attempt);
                } else {
                    attemptStatus = "FALHA_COMPILACAO";
                    finalSummary = safeValue(changeSet.summary());
                    finalResult = llmResponse;
                    String feedbackText = feedbackTool.buildFeedback(
                            compilationResults,
                            toDiffSummaries(writeResults),
                            llmResponse,
                            safeValue(changeSet.notes())
                    );
                    prompt = buildRepairPrompt(currentPrompt, snapshot, llmResponse, feedbackText);
                    feedback = feedbackText;
                }
            } catch (Exception e) {
                log.error("[EXEC_ERR] erro na tentativa={}", attempt, e);
                attemptStatus = "ERRO";
                attemptError = safeValue(e.getMessage());
                finalSummary = "Falha na execucao do agente";
                finalResult = "BUILD_ERROR: " + attemptError;

                if (isNonRetryableModelError(e)) {
                    String rootCause = extractRootCauseMessage(e);
                    finalStatus = STATUS_FALHA_MODELO;
                    finalSummary = "Falha nao recuperavel do provedor LLM";
                    finalResult = "MODEL_ERROR: " + rootCause;
                    log.error("[NON_RETRY] encerrando por erro nao recuperavel: {}", rootCause);
                    nonRetryable = true;
                } else {
                    prompt = buildRepairPrompt(currentPrompt, snapshot, llmResponse, attemptError);
                    feedback = attemptError;
                }
            }

            attempts.add(new AttemptResponse(
                    null,
                    attempt,
                    attemptStatus,
                    "plano-gerado-pelo-llm",
                    currentPrompt,
                    llmResponse,
                    previousFeedback,
                    attemptError,
                    attemptStart,
                    LocalDateTime.now(),
                    toArtifactChanges(changeSet.changes()),
                    List.of()
            ));

            log.trace("[EXEC_LOOP_RT] tentativa={} finalizada status={}", attempt, attemptStatus);

            if (success || nonRetryable) {
                break;
            }
        }

        ExecutionResponse response = new ExecutionResponse(
                null,
                finalStatus,
                request.taskDescription(),
                request.compileBy().name(),
                attempts.size(),
                maxAttempts,
                agentProperties.getBackendRoot(),
                agentProperties.getFrontendRoot(),
                safeValue(finalSummary),
                safeValue(finalResult),
                startedAt,
                LocalDateTime.now(),
                attempts
        );
        log.info("[EXECUTE_RT] execucao finalizada status='{}' tentativas={}", response.status(), response.currentAttempt());
        return response;
    }

    String generate(String prompt) {
        log.info("[GENERATE] geracao do modelo desativada nesta versao; retornando alteracoes vazias");
        GeneratedChangeSet noChanges = new GeneratedChangeSet(
                "Execucao sem alteracoes",
                "PromptGenerationClient removido; nenhuma alteracao sugerida pelo modelo.",
                List.of(),
                List.of()
        );
        return JsonUtils.toJson(noChanges);
    }

    private String buildInitialPrompt(String taskDescription, ProjectDiscoverySnapshot snapshot) {
        log.debug("[BUILD_INIT] montando prompt inicial sem cliente dedicado");
        return "TAREFA: " + safeValue(taskDescription) + "\n\nDISCOVERY:\n" + safeValue(snapshot.summary());
    }

    private String buildRepairPrompt(String previousPrompt, ProjectDiscoverySnapshot snapshot, String llmResponse, String errorOrFeedback) {
        log.debug("[BUILD_REP] montando prompt de reparo com feedback de compilacao");
        return "PROMPT_ANTERIOR:\n"
                + safeValue(previousPrompt)
                + "\n\nDISCOVERY:\n"
                + safeValue(snapshot.summary())
                + "\n\nULTIMA_RESPOSTA:\n"
                + safeValue(llmResponse)
                + "\n\nFEEDBACK:\n"
                + safeValue(errorOrFeedback);
    }

    private void validateScope(List<FileChangeCommand> changes, ProjectDiscoverySnapshot snapshot) {
        log.debug("[VAL_SCOPE] validando escopo de arquivos antes da escrita");
        if (changes == null || changes.isEmpty()) {
            return;
        }
        Path backendRoot = Path.of(snapshot.backend().rootPath().toString()).toAbsolutePath().normalize();
        Path frontendRoot = Path.of(snapshot.frontend().rootPath().toString()).toAbsolutePath().normalize();
        for (FileChangeCommand change : changes) {
            if (change == null || change.filePath() == null || change.filePath().isBlank()) {
                throw new IllegalArgumentException("Alteracao com caminho de arquivo invalido");
            }
            Path normalized = Path.of(change.filePath()).toAbsolutePath().normalize();
            if (!normalized.startsWith(backendRoot) && !normalized.startsWith(frontendRoot)) {
                throw new IllegalArgumentException("Arquivo fora do escopo permitido: " + normalized);
            }
        }
    }

    private List<CompilationResult> compileProjects(CompileBy compileBy, GeneratedChangeSet changeSet) {
        log.debug("[COMPILE_ALL] compilando backend e frontend no modo {}", compileBy);
        List<CompilationResult> results = new ArrayList<>();
        results.add(compilationTool.compileBackend(compileBy));
        results.add(compilationTool.compileFrontend(compileBy));
        return results;
    }

    private List<CodeDiffSummary> toDiffSummaries(List<FileWriteResult> writeResults) {
        log.debug("[MAP_DIFF] mapeando resultados de escrita para diff summaries");
        if (writeResults == null || writeResults.isEmpty()) {
            return List.of();
        }
        return writeResults.stream()
                .map(FileWriteResult::diffSummary)
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isSuccessful(List<CompilationResult> results) {
        log.debug("[CHK_COMP_OK] validando retorno da compilacao");
        if (results == null || results.isEmpty()) {
            return false;
        }
        for (CompilationResult result : results) {
            if (result == null || !result.success() || result.timedOut() || result.exitCode() != 0) {
                return false;
            }
        }
        return true;
    }

    private String resolveBackupRoot() {
        log.debug("[BACKUP_ROOT] resolvendo diretorio para backup de escrita");
        return Path.of(System.getProperty("java.io.tmpdir"), "springia-backups").toString();
    }

    private void validateRequest(ExecutionRequest request) {
        log.debug("[VAL_REQ] validando request de execucao");
        if (request == null) {
            throw new IllegalArgumentException("Request nao pode ser nulo");
        }
        if (request.taskDescription() == null || request.taskDescription().isBlank()) {
            throw new IllegalArgumentException("taskDescription nao pode ser vazio");
        }
        if (request.compileBy() == null) {
            throw new IllegalArgumentException("compileBy nao pode ser nulo");
        }
        log.debug("[VAL_REQ_RT] request valido");
    }

    private GeneratedChangeSet parseGeneratedChangeSet(String llmResponse) {
        log.debug("[PARSE_GSET] convertendo resposta do modelo para GeneratedChangeSet");
        try {
            String json = unwrapJsonCodeFence(llmResponse);
            JsonNode root = JsonUtils.toTree(json);
            normalizeAliasesAndEnums(root);
            GeneratedChangeSet parsed = JsonUtils.objectMapper().treeToValue(root, GeneratedChangeSet.class);
            log.debug("[PARSE_GSET_RT] changes={} targets={}", parsed.changes().size(), parsed.compilationTargets().size());
            return parsed;
        } catch (Exception e) {
            log.error("[PARSE_GSET] falha ao converter resposta do modelo", e);
            throw new IllegalStateException("Resposta do LLM nao esta no formato GeneratedChangeSet", e);
        }
    }

    private String unwrapJsonCodeFence(String value) {
        log.debug("[UNWRAP_JSON] removendo markdown fence da resposta");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Resposta do modelo nao pode ser vazia");
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            String withoutStart = trimmed.replaceFirst("^```[a-zA-Z]*\\n", "");
            String withoutEnd = withoutStart.replaceFirst("\\n```$", "");
            log.debug("[UNWRAP_JSON_RT] markdown removido");
            return withoutEnd.trim();
        }
        log.debug("[UNWRAP_JSON_RT] resposta ja estava em json puro");
        return trimmed;
    }

    private void normalizeAliasesAndEnums(JsonNode root) {
        log.debug("[NORM_JSON] normalizando aliases e enums do json");
        if (!(root instanceof ObjectNode objectNode)) {
            return;
        }
        if (!objectNode.has("changes") && objectNode.has("files")) {
            objectNode.set("changes", objectNode.get("files"));
            objectNode.remove("files");
        }
        JsonNode changes = objectNode.get("changes");
        if (changes != null && changes.isArray()) {
            for (JsonNode change : changes) {
                if (change instanceof ObjectNode changeNode) {
                    JsonNode operation = changeNode.get("operation");
                    if (operation != null && operation.isTextual()) {
                        changeNode.put("operation", operation.asText().toUpperCase(Locale.ROOT));
                    }
                }
            }
        }
        log.debug("[NORM_JSON_RT] normalizacao concluida");
    }

    private List<ArtifactChangeResponse> toArtifactChanges(List<FileChangeCommand> changes) {
        log.debug("[MAP_CHG] convertendo alteracoes para response dto");
        if (changes == null || changes.isEmpty()) {
            return List.of();
        }
        List<ArtifactChangeResponse> mapped = new ArrayList<>();
        for (FileChangeCommand change : changes) {
            mapped.add(new ArtifactChangeResponse(
                    null,
                    change.filePath(),
                    change.operation() == null ? "" : change.operation().name(),
                    safeValue(change.summary())
            ));
        }
        log.debug("[MAP_CHG_RT] {} alteracoes mapeadas", mapped.size());
        return mapped;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private boolean isNonRetryableModelError(Exception exception) {
        log.debug("[NON_RETRY_CHK] avaliando se erro e nao recuperavel");
        Throwable cursor = exception;
        while (cursor != null) {
            if (cursor instanceof BadRequestException badRequestException) {
                String message = safeValue(badRequestException.getMessage()).toLowerCase(Locale.ROOT);
                if (message.contains("unsupported")
                        || message.contains("not supported")
                        || message.contains("operation is unsupported")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private String extractRootCauseMessage(Exception exception) {
        log.debug("[ROOT_CAUSE] extraindo mensagem raiz do erro");
        Throwable cursor = exception;
        String lastMessage = safeValue(exception.getMessage());
        while (cursor != null) {
            String current = safeValue(cursor.getMessage());
            if (!current.isBlank()) {
                lastMessage = current;
            }
            cursor = cursor.getCause();
        }
        return StringUtils.abbreviate(lastMessage, 300);
    }
}
