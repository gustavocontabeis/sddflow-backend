package com.example.springia.agent.loop;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * Representa um passo na execução do agente
 */
@Data
@Builder
public class AgentStep {

    /**
     * Número do step na sequência
     */
    private int stepNumber;

    /**
     * Pensamento/raciocínio do agente antes de executar
     */
    private String thinking;

    /**
     * Nome da ferramenta a ser executada (ex: create_file)
     */
    private String toolName;

    /**
     * Parâmetros para a ferramenta
     */
    private Map<String, String> toolParams;

    /**
     * Resultado da execução da ferramenta
     */
    private String toolResult;

    /**
     * Observação/análise pós-execução
     */
    private String observation;

    /**
     * Indica se é o último passo (finalização)
     */
    private boolean isFinal;

    /**
     * Resposta final (quando isFinal = true)
     */
    private String finalAnswer;
}

