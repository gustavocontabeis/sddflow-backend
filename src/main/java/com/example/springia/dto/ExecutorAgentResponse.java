package com.example.springia.dto;

import com.example.springia.agent.loop.AgentStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para resposta da execução do Agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutorAgentResponse {

    /**
     * ID único da execução
     */
    private String executionId;

    /**
     * Entrada fornecida ao agent
     */
    private String input;

    /**
     * Resposta final do agent
     */
    private String finalAnswer;

    /**
     * Número de passos executados
     */
    private int stepCount;

    /**
     * Status final: SUCCESS, ERROR, TIMEOUT
     */
    private String status;

    /**
     * Mensagem de erro se houver
     */
    private String errorMessage;

    /**
     * Tempo total de execução em ms
     */
    private long totalExecutionTimeMs;

    /**
     * Timestamp de início
     */
    private LocalDateTime startTime;

    /**
     * Timestamp de fim
     */
    private LocalDateTime endTime;

    /**
     * Passos executados com detalhes
     */
    private List<AgentStepResponse> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentStepResponse {
        private int stepNumber;
        private String thinking;
        private String toolName;
        private String toolResult;
        private String observation;
        private boolean isFinal;
        private String finalAnswer;

        public static AgentStepResponse fromAgentStep(AgentStep step) {
            return AgentStepResponse.builder()
                    .stepNumber(step.getStepNumber())
                    .thinking(step.getThinking())
                    .toolName(step.getToolName())
                    .toolResult(step.getToolResult() != null ?
                        (step.getToolResult().length() > 1000 ?
                            step.getToolResult().substring(0, 1000) + "..." :
                            step.getToolResult())
                        : null)
                    .observation(step.getObservation())
                    .isFinal(step.isFinal())
                    .finalAnswer(step.getFinalAnswer())
                    .build();
        }
    }
}

