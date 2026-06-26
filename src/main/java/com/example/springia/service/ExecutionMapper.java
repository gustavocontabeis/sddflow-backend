package com.example.springia.service;

import com.example.springia.dto.ArtifactChangeResponse;
import com.example.springia.dto.AttemptResponse;
import com.example.springia.dto.CompilationLogResponse;
import com.example.springia.dto.ExecutionResponse;
import com.example.springia.dto.ExecutionSummaryResponse;
import com.example.springia.entity.ArtifactChange;
import com.example.springia.entity.Attempt;
import com.example.springia.entity.CompilationLog;
import com.example.springia.entity.Execution;

import java.util.List;

public class ExecutionMapper {

    private ExecutionMapper() {
    }

    public static ExecutionSummaryResponse toSummary(Execution execution, int attempts) {
        return new ExecutionSummaryResponse(
                execution.getNuId(),
                execution.getCoStatus(),
                execution.getDeSolicitacao(),
                execution.getCoCompileBy(),
                attempts,
                execution.getDhInicio(),
                execution.getDhFim()
        );
    }

    public static ExecutionResponse toResponse(Execution execution, List<Attempt> attempts, List<ArtifactChange> changes, List<CompilationLog> compilationLogs) {
        return new ExecutionResponse(
                execution.getNuId(),
                execution.getCoStatus(),
                execution.getDeSolicitacao(),
                execution.getCoCompileBy(),
                execution.getNuTentativaAtual(),
                execution.getNuMaxTentativas(),
                execution.getDeBackendPath(),
                execution.getDeFrontendPath(),
                execution.getDeContexto(),
                execution.getDeResultado(),
                execution.getDhInicio(),
                execution.getDhFim(),
                attempts.stream().map(attempt -> toAttempt(attempt, changes, compilationLogs)).toList()
        );
    }

    private static AttemptResponse toAttempt(Attempt attempt, List<ArtifactChange> changes, List<CompilationLog> compilationLogs) {
        List<ArtifactChangeResponse> attemptChanges = changes.stream()
                .filter(change -> change.getNuTentativaId().equals(attempt.getNuId()))
                .map(change -> new ArtifactChangeResponse(change.getNuId(), change.getDeCaminhoArquivo(), change.getCoTipoAcao(), change.getDeResumo()))
                .toList();
        List<CompilationLogResponse> attemptLogs = compilationLogs.stream()
                .filter(log -> log.getNuTentativaId().equals(attempt.getNuId()))
                .map(log -> new CompilationLogResponse(log.getNuId(), log.getCoDestino(), log.getCoComando(), log.getIcSucesso(), !log.getIcSucesso() && log.getCoStatus() != null && log.getCoStatus().contains("TIMEOUT"),  log.getIcSucesso() ? 0 : 1, log.getDeSaida(), log.getDeSaida(), log.getDhInicio(), log.getDhFim()))
                .toList();
        return new AttemptResponse(
                attempt.getNuId(),
                attempt.getNuNumero(),
                attempt.getCoStatus(),
                attempt.getDePlano(),
                attempt.getDePrompt(),
                attempt.getDeResposta(),
                attempt.getDeFeedback(),
                attempt.getDeErro(),
                attempt.getDhInicio(),
                attempt.getDhFim(),
                attemptChanges,
                attemptLogs
        );
    }
}


