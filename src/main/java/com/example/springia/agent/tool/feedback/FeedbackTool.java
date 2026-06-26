package com.example.springia.agent.tool.feedback;

import com.example.springia.agent.model.CompilationResult;
import com.example.springia.agent.model.CodeDiffSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consolida logs e feedback para a proxima iteracao do agente.
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.feedback.FeedbackTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Slf4j
@Component
public class FeedbackTool {

    @Tool(name = "build_feedback", description = "Consolida logs, diffs e erros de compilacao para a proxima iteracao")
    public String buildFeedback(
            @ToolParam(description = "Resultados de compilacao ou teste") List<CompilationResult> compilationResults,
            @ToolParam(description = "Resumo dos diffs gerados") List<CodeDiffSummary> diffSummaries,
            @ToolParam(description = "Resposta anterior do modelo") String previousResponse,
            @ToolParam(description = "Dica de reparo para a nova tentativa") String repairHint
    ) {
        log.info("{[FEEDBACK]} consolidando feedback da iteracao");
        StringBuilder builder = new StringBuilder();
        builder.append("DICA DE REPARO: ").append(repairHint == null ? "" : repairHint).append('\n');

        if (previousResponse != null && !previousResponse.isBlank()) {
            builder.append("RESPOSTA ANTERIOR:\n").append(previousResponse).append('\n');
        }

        if (diffSummaries != null && !diffSummaries.isEmpty()) {
            builder.append("DIFFS:\n");
            for (CodeDiffSummary diffSummary : diffSummaries) {
                builder.append("- ").append(diffSummary.summary()).append(" -> ").append(diffSummary.filePath()).append('\n');
            }
        }

        if (compilationResults != null && !compilationResults.isEmpty()) {
            builder.append("COMPILACAO:\n");
            for (CompilationResult result : compilationResults) {
                builder.append("- ")
                        .append(result.projectName())
                        .append(" [")
                        .append(result.compileBy())
                        .append("] sucesso=")
                        .append(result.success())
                        .append(", exitCode=")
                        .append(result.exitCode())
                        .append(result.timedOut() ? ", timeout" : "")
                        .append('\n');
                if (result.errorOutput() != null && !result.errorOutput().isBlank()) {
                    builder.append(result.errorOutput()).append('\n');
                }
            }
        }
        log.info("{[FEEDBACK]} {}", builder);
        return builder.toString();
    }
}

