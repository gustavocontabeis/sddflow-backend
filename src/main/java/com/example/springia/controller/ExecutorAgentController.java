package com.example.springia.controller;

import com.example.springia.dto.ExecutorAgentRequest;
import com.example.springia.dto.ExecutorAgentResponse;
import com.example.springia.service.ExecutorAgentService;
import com.example.springia.service.TaskSddService;
import com.example.springia.agent.loop.AgentExecution;
import com.example.springia.model.TaskSdd;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * Controller para executar Agent loops com ReAct pattern
 *
 * Endpoints:
 * - POST /executor-agent/execute: Executa uma tarefa
 * - POST /executor-agent/execute-task/{taskId}: Executa uma tarefa salva no SDD
 */
@Slf4j
@RestController
@RequestMapping("/executor-agent")
@RequiredArgsConstructor
public class ExecutorAgentController {

    private final ExecutorAgentService executorAgentService;
    private final TaskSddService taskSddService;

    /**
     * Executa o agent com uma descrição de tarefa
     *
     * <p>Exemplo de execução:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/executor-agent/execute \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "taskDescription": "Crie um arquivo teste.txt em src/test com conteúdo Hello World",
     *     "basePath": "/home/gustavo/dev/teste-spring-ia/springia"
     *   }'
     * }</pre>
     */
    @PostMapping("/execute")
    public ResponseEntity<ExecutorAgentResponse> execute(@RequestBody ExecutorAgentRequest request) {
        log.info("[AGENT_CONTROLLER] POST /execute taskDescription_length={}",
            request.getTaskDescription() != null ? request.getTaskDescription().length() : 0);

        try {
            // Define base path se fornecido
            if (request.getBasePath() != null && !request.getBasePath().isBlank()) {
                executorAgentService.setBasePath(request.getBasePath());
            }

            // Executa o agent
            AgentExecution execution = executorAgentService.executeTask(request.getTaskDescription());

            // Converte para DTO
            ExecutorAgentResponse response = mapExecutionToResponse(execution);

            log.info("[AGENT_CONTROLLER] Execução concluída: {} - {} passos",
                execution.getStatus(), execution.getStepCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[AGENT_CONTROLLER] Erro ao executar task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ExecutorAgentResponse.builder()
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * Executa um TaskSdd salvo no banco de dados
     *
     * <p>Exemplo de execução:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/executor-agent/execute-task/1
     * }</pre>
     */
    @PostMapping("/execute-task/{taskId}")
    public ResponseEntity<ExecutorAgentResponse> executeTask(
            @PathVariable Long taskId,
            @RequestParam(required = false) String basePath) {

        log.info("[AGENT_CONTROLLER] POST /execute-task/{} basePath={}", taskId, basePath);

        try {
            // Busca a tarefa no banco
            TaskSdd taskSdd = taskSddService.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("TaskSdd não encontrada: " + taskId));

            String taskContent = taskSdd.getContent();
            log.info("[AGENT_CONTROLLER] TaskSdd encontrada: {} bytes", taskContent.length());

            // Define base path se fornecido
            if (basePath != null && !basePath.isBlank()) {
                executorAgentService.setBasePath(basePath);
            }

            // Executa o agent com o conteúdo da tarefa
            AgentExecution execution = executorAgentService.executeTask(taskContent);

            // Converte para DTO
            ExecutorAgentResponse response = mapExecutionToResponse(execution);

            log.info("[AGENT_CONTROLLER] Task {} executada: {} - {} passos",
                taskId, execution.getStatus(), execution.getStepCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[AGENT_CONTROLLER] Erro ao executar task {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ExecutorAgentResponse.builder()
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * Retorna as ferramentas disponíveis para o agent
     */
    @GetMapping("/tools")
    public ResponseEntity<String> getAvailableTools() {
        log.info("[AGENT_CONTROLLER] GET /tools");
        String toolsDescription = executorAgentService.getToolRegistry().getToolsDescription();
        return ResponseEntity.ok(toolsDescription);
    }

    /**
     * Mapeia AgentExecution para ExecutorAgentResponse
     */
    private ExecutorAgentResponse mapExecutionToResponse(AgentExecution execution) {
        return ExecutorAgentResponse.builder()
                .executionId(execution.getExecutionId())
                .input(execution.getInput())
                .finalAnswer(execution.getFinalAnswer())
                .stepCount(execution.getStepCount())
                .status(execution.getStatus())
                .errorMessage(execution.getErrorMessage())
                .totalExecutionTimeMs(execution.getTotalExecutionTimeMs())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .steps(execution.getSteps() != null ?
                    execution.getSteps().stream()
                        .map(ExecutorAgentResponse.AgentStepResponse::fromAgentStep)
                        .collect(Collectors.toList())
                    : null)
                .build();
    }
}

