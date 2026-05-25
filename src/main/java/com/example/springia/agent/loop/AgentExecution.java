package com.example.springia.agent.loop;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Contém o resultado completo de uma execução do agente
 */
@Data
@Builder
public class AgentExecution {

    /**
     * ID único da execução
     */
    private String executionId;

    /**
     * Entrada original fornecida ao agente
     */
    private String input;

    /**
     * Resposta final do agente
     */
    private String finalAnswer;

    /**
     * Lista de todos os passos executados
     */
    private List<AgentStep> steps;

    /**
     * Tempo total de execução em ms
     */
    private long totalExecutionTimeMs;

    /**
     * Número de iterações/passos executados
     */
    private int stepCount;

    /**
     * Timestamp de início da execução
     */
    private LocalDateTime startTime;

    /**
     * Timestamp de fim da execução
     */
    private LocalDateTime endTime;

    /**
     * Status final (SUCCESS, ERROR, TIMEOUT, etc)
     */
    private String status;

    /**
     * Mensagem de erro, se houver
     */
    private String errorMessage;

    /**
     * Inicializa a lista de steps
     */
    public void initSteps() {
        if (this.steps == null) {
            this.steps = new ArrayList<>();
        }
    }

    /**
     * Adiciona um step
     */
    public void addStep(AgentStep step) {
        initSteps();
        this.steps.add(step);
        this.stepCount = this.steps.size();
    }
}

