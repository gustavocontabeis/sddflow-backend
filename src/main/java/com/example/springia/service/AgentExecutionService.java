package com.example.springia.service;

import com.example.springia.agent.model.CompilationResult;
import com.example.springia.agent.model.FileWriteResult;
import com.example.springia.agent.model.GeneratedChangeSet;
import com.example.springia.agent.model.ProjectDiscoverySnapshot;
import com.example.springia.agent.service.PromptGenerationClient;
import com.example.springia.config.AgentProperties;
import com.example.springia.dto.ExecutionRequest;
import com.example.springia.dto.ExecutionResponse;
import com.example.springia.entity.ArtifactChange;
import com.example.springia.entity.Attempt;
import com.example.springia.entity.CompilationLog;
import com.example.springia.entity.Execution;
import com.example.springia.entity.ExecutionStatus;
import com.example.springia.repository.ArtifactChangeRepository;
import com.example.springia.repository.AttemptRepository;
import com.example.springia.repository.CompilationLogRepository;
import com.example.springia.repository.ExecutionRepository;
import com.example.springia.repository.ExecutionStatusRepository;
import com.example.springia.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orquestra discovery, LLM, escrita, compilacao e feedback.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.service.AgentExecutionService" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionService {

    private final AgentProperties agentProperties;
    private final PromptGenerationClient promptGenerationClient;
    private final ExecutionRepository executionRepository;
    private final AttemptRepository attemptRepository;
    private final ArtifactChangeRepository artifactChangeRepository;
    private final CompilationLogRepository compilationLogRepository;
    private final ExecutionStatusRepository executionStatusRepository;

    @Transactional
    public ExecutionResponse execute(ExecutionRequest request) {
        log.info("{[EXEC_START]} iniciando execucao do agente taskDescription='{}' compileBy='{}'",
                request == null ? null : request.taskDescription(),
                request == null || request.compileBy() == null ? null : request.compileBy().name());
        validateRequest(request);

        ExecutionStatus statusReceived = getOrCreateStatus("RECEBIDO", "Recebido");
        ExecutionStatus statusRunning = getOrCreateStatus("EM_EXECUCAO", "Em execucao");
        ExecutionStatus statusSuccess = getOrCreateStatus("SUCESSO", "Sucesso");
        ExecutionStatus statusFailure = getOrCreateStatus("FALHA", "Falha");

        ProjectDiscoverySnapshot discovery = promptGenerationClient.discover();
        String plan = promptGenerationClient.buildInitialPrompt(request.taskDescription(), discovery);

        Execution execution = executionRepository.save(Execution.builder()
                .nuStatusId(statusRunning.getNuId())
                .coStatus(statusRunning.getCoCodigo())
                .nuTentativaAtual(0)
                .nuMaxTentativas(agentProperties.getMaxIterations())
                .dhInicio(LocalDateTime.now())
                .coCompileBy(request.compileBy().name())
                .deSolicitacao(request.taskDescription())
                .deContexto(discovery.summary())
                .deResultado(null)
                .deBackendPath(agentProperties.getBackendRoot())
                .deFrontendPath(agentProperties.getFrontendRoot())
                .build());

        List<Attempt> attempts = new ArrayList<>();
        List<ArtifactChange> changes = new ArrayList<>();
        List<CompilationLog> compilationLogs = new ArrayList<>();
        String lastResponse = null;
        String feedback = plan;

        for (int attemptNumber = 1; attemptNumber <= agentProperties.getMaxIterations(); attemptNumber++) {
            execution.setNuTentativaAtual(attemptNumber);
            Attempt attempt = attemptRepository.save(Attempt.builder()
                    .nuExecucaoId(execution.getNuId())
                    .nuNumero(attemptNumber)
                    .coStatus("EM_EXECUCAO")
                    .dhInicio(LocalDateTime.now())
                    .dePlano(plan)
                    .dePrompt(feedback)
                    .build());
            attempts.add(attempt);

            log.info("\nANTES: \n{}", feedback);
            String rawResponse = promptGenerationClient.generate(feedback);
            log.info("\nDEPOIS: \n{}", rawResponse);
            lastResponse = rawResponse;
            GeneratedChangeSet changeSet = parseGeneratedChangeSet(rawResponse);
            promptGenerationClient.validateScope(discovery, changeSet);

            List<FileWriteResult> writeResults;
            try {
                writeResults = promptGenerationClient.writeChanges(changeSet.changes());
            } catch (RuntimeException ex) {
                log.error("{[EXEC_WRITE]} falha ao aplicar alteracoes de arquivo na tentativa {}", attemptNumber, ex);
                String writeFailureFeedback = "Falha ao aplicar alteracoes de arquivo: " + ex.getMessage();
                feedback = promptGenerationClient.buildRepairPrompt(request.taskDescription(), discovery, rawResponse, writeFailureFeedback);
                attempt.setCoStatus("FALHA");
                attempt.setDhFim(LocalDateTime.now());
                attempt.setDeResposta(rawResponse);
                attempt.setDeFeedback(writeFailureFeedback);
                attempt.setDeErro(writeFailureFeedback);
                attemptRepository.save(attempt);
                continue;
            }
            for (FileWriteResult writeResult : writeResults) {
                changes.add(artifactChangeRepository.save(ArtifactChange.builder()
                        .nuTentativaId(attempt.getNuId())
                        .coTipoAcao(writeResult.operation().name())
                        .deCaminhoArquivo(writeResult.filePath())
                        .deConteudoAnterior(null)
                        .deConteudoNovo(null)
                        .deResumo(writeResult.message() + " | " + writeResult.diffSummary().summary())
                        .build()));
            }

            List<CompilationResult> compilationResults = promptGenerationClient.compile(request.compileBy());
            for (CompilationResult result : compilationResults) {
                compilationLogs.add(saveCompilation(attempt.getNuId(), result));
            }

            if (promptGenerationClient.isSuccessful(compilationResults)) {
                attempt.setCoStatus("VALIDADO");
                attempt.setDhFim(LocalDateTime.now());
                attemptRepository.save(attempt);
                execution.setNuStatusId(statusSuccess.getNuId());
                execution.setCoStatus(statusSuccess.getCoCodigo());
                execution.setDhFim(LocalDateTime.now());
                execution.setDeResultado("Execucao concluida com sucesso");
                executionRepository.save(execution);
                log.info("{[EXEC_RET]} execucao concluida com sucesso na tentativa {}", attemptNumber);
                return ExecutionMapper.toResponse(execution, attempts, changes, compilationLogs);
            }

            String generatedFeedback = promptGenerationClient.buildFeedback(compilationResults, rawResponse, buildRepairHint(compilationResults));
            feedback = promptGenerationClient.buildRepairPrompt(request.taskDescription(), discovery, rawResponse, generatedFeedback);
            attempt.setCoStatus("FALHA");
            attempt.setDhFim(LocalDateTime.now());
            attempt.setDeResposta(rawResponse);
            attempt.setDeFeedback(generatedFeedback);
            attempt.setDeErro(generatedFeedback);
            attemptRepository.save(attempt);
        }

        execution.setNuStatusId(statusFailure.getNuId());
        execution.setCoStatus(statusFailure.getCoCodigo());
        execution.setDhFim(LocalDateTime.now());
        execution.setDeResultado("Execucao encerrada sem sucesso");
        executionRepository.save(execution);
        log.info("{[EXEC_RET]} execucao encerrada sem sucesso apos {} tentativas", agentProperties.getMaxIterations());
        return ExecutionMapper.toResponse(execution, attempts, changes, compilationLogs);
    }

    private void validateRequest(ExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requisicao nao pode ser nula");
        }
        if (request.taskDescription() == null || request.taskDescription().isBlank()) {
            throw new IllegalArgumentException("taskDescription nao pode ser nulo ou vazio");
        }
        if (request.compileBy() == null) {
            throw new IllegalArgumentException("compileBy nao pode ser nulo");
        }
    }

    private ExecutionStatus getOrCreateStatus(String codigo, String descricao) {
        Optional<ExecutionStatus> existing = executionStatusRepository.findByCoCodigo(codigo);
        if (existing.isPresent()) {
            return existing.get();
        }
        return executionStatusRepository.save(ExecutionStatus.builder()
                .coCodigo(codigo)
                .deDescricao(descricao)
                .icAtivo(Boolean.TRUE)
                .build());
    }

    private CompilationLog saveCompilation(Long attemptId, CompilationResult result) {
        return compilationLogRepository.save(CompilationLog.builder()
                .nuTentativaId(attemptId)
                .coDestino(result.projectName())
                .coComando(result.command())
                .coStatus(result.success() ? "SUCESSO" : (result.timedOut() ? "TIMEOUT" : "FALHA"))
                .icSucesso(result.success())
                .dhInicio(LocalDateTime.now().minus(result.duration()))
                .dhFim(LocalDateTime.now())
                .deSaida(result.output() + "\n" + result.errorOutput())
                .build());
    }

    private String buildRepairHint(List<CompilationResult> compilationResults) {
        StringBuilder hint = new StringBuilder();
        for (CompilationResult result : compilationResults) {
            if (hint.length() > 0) {
                hint.append(", ");
            }
            hint.append(result.projectName())
                    .append("=")
                    .append(result.success())
                    .append("(exit=")
                    .append(result.exitCode())
                    .append(")");
        }
        return hint.toString();
    }

    private GeneratedChangeSet parseGeneratedChangeSet(String rawResponse) {
        log.debug("{[PARSE_JSON]} normalizando resposta do modelo para JSON");
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("Resposta do LLM veio vazia");
        }

        String normalized = rawResponse.trim();
        if (normalized.startsWith("```")) {
            int firstNewLine = normalized.indexOf('\n');
            int fenceStart = firstNewLine >= 0 ? firstNewLine + 1 : 3;
            int fenceEnd = normalized.lastIndexOf("```");
            if (fenceEnd > fenceStart) {
                normalized = normalized.substring(fenceStart, fenceEnd).trim();
            }
        }

        int jsonObjectStart = normalized.indexOf('{');
        int jsonObjectEnd = normalized.lastIndexOf('}');
        if (jsonObjectStart >= 0 && jsonObjectEnd > jsonObjectStart) {
            normalized = normalized.substring(jsonObjectStart, jsonObjectEnd + 1);
        }

        try {
            return JsonUtils.fromJson(normalized, GeneratedChangeSet.class);
        } catch (RuntimeException ex) {
            log.error("{[PARSE_JSON]} falha ao desserializar resposta do modelo", ex);
            throw new IllegalStateException("Resposta do LLM invalida para GeneratedChangeSet", ex);
        }
    }
}

