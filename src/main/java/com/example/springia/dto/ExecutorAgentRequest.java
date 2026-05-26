package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de execução do Agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutorAgentRequest {

    /**
     * Descrição da tarefa a ser executada
     * Pode ser o conteúdo de um Task.md ou qualquer instrução
     */
    private String taskDescription;

    /**
     * Subdiretório opcional dentro do diretório temporário do sistema operacional.
     * Se vier vazio ou ausente, usa a raiz do diretório temporário.
     */
    private String basePath;

    /**
     * IDs opcionais de referência (taskId, userStoryId, etc)
     */
    private Long taskId;
    private Long userStoryId;
    private Long projectId;
}

