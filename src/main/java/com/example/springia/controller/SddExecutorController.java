package com.example.springia.controller;

import com.example.springia.dto.ExecutorAgentResponse;
import com.example.springia.service.SddTaskExecutorService;
import com.example.springia.agent.loop.AgentExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * Controller para executar Task.md do SDD com contexto completo
 *
 * Endpoints:
 * - POST /sdd-executor/execute-task/{taskId}: Executa TaskSdd com contexto (Spec+Plan+Task)
 * - POST /sdd-executor/execute-userstory/{userStoryId}: Executa tarefa associada à UserStory
 * - POST /sdd-executor/execute-impl/{implId}: Executa implementação associada ao ImplSdd
 * - GET /sdd-executor/preview/{taskId}: Preview do contexto de execução (sem executar)
 */
@Slf4j
@RestController
@RequestMapping("/sdd-executor")
@RequiredArgsConstructor
public class SddExecutorController {

    private final SddTaskExecutorService sddTaskExecutorService;

    /**
     * Executa uma tarefa (Task.md) com contexto completo (Spec + Plan)
     *
     * <p>Exemplo de execução:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/sdd-executor/execute-task/1
     * }</pre>
     *
     * Isso irá:
     * 1. Buscar o TaskSdd com ID 1
     * 2. Obter UserStory associada
     * 3. Carregar Spec e Plan associados
     * 4. Montar contexto completo
     * 5. Executar agent loop com ReAct pattern
     */
    //@PostMapping("/execute-task/{taskId}")
    public ResponseEntity<ExecutorAgentResponse> executeTask(@PathVariable Long taskId) {
        log.info("[SDD_EXECUTOR_CONTROLLER] POST /execute-task/{}", taskId);

        try {
            AgentExecution execution = sddTaskExecutorService.executeTaskWithContext(taskId);
            ExecutorAgentResponse response = mapExecutionToResponse(execution);

            log.info("[SDD_EXECUTOR_CONTROLLER] Tarefa {} executada: {} - {} passos",
                    taskId, execution.getStatus(), execution.getStepCount());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("[SDD_EXECUTOR_CONTROLLER] Erro de validação ao executar tarefa {}", taskId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ExecutorAgentResponse.builder()
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("[SDD_EXECUTOR_CONTROLLER] Erro ao executar tarefa {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ExecutorAgentResponse.builder()
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * Executa a tarefa associada a uma UserStory
     *
     * <p>Exemplo de execução:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/sdd-executor/execute-userstory/1
     * }</pre>
     */
    //@PostMapping("/execute-userstory/{userStoryId}")
    public ResponseEntity<ExecutorAgentResponse> executeByUserStory(@PathVariable Long userStoryId) {
        log.info("[SDD_EXECUTOR_CONTROLLER] POST /execute-userstory/{}", userStoryId);

        try {
            AgentExecution execution = sddTaskExecutorService.executeByUserStory(userStoryId);
            ExecutorAgentResponse response = mapExecutionToResponse(execution);

            log.info("[SDD_EXECUTOR_CONTROLLER] UserStory {} executada: {} - {} passos",
                    userStoryId, execution.getStatus(), execution.getStepCount());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("[SDD_EXECUTOR_CONTROLLER] Erro de validação para UserStory {}", userStoryId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ExecutorAgentResponse.builder()
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("[SDD_EXECUTOR_CONTROLLER] Erro ao executar UserStory {}", userStoryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ExecutorAgentResponse.builder()
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * Executa a implementação associada a um ImplSdd
     *
     * <p>Exemplo de execução:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/sdd-executor/execute-impl/1
     * }</pre>
     */
    @PostMapping("/execute-impl/{implId}")
    public ResponseEntity<ExecutorAgentResponse> executeByImpl(@PathVariable Long implId) {
        log.info("[SDD_EXECUTOR_CONTROLLER] POST /execute-impl/{}", implId);

        try {
            AgentExecution execution = sddTaskExecutorService.executeByImpl(implId);
            ExecutorAgentResponse response = mapExecutionToResponse(execution);

            log.info("[SDD_EXECUTOR_CONTROLLER] ImplSdd {} executada: {} - {} passos",
                    implId, execution.getStatus(), execution.getStepCount());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("[SDD_EXECUTOR_CONTROLLER] Erro de validação ao executar impl {}", implId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ExecutorAgentResponse.builder()
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("[SDD_EXECUTOR_CONTROLLER] Erro ao executar impl {}", implId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ExecutorAgentResponse.builder()
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * Executa a implementação associada a um ImplSdd
     *
     * <p>Exemplo de execução:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/sdd-executor/execute-impl/1
     * }</pre>
     */
    @PostMapping("/execute-docker/{implId}")
    public ResponseEntity testeDocker(@PathVariable Long implId) {
        log.info("[SDD_EXECUTOR_CONTROLLER] POST /execute-docker/{}", implId);

            sddTaskExecutorService.xxx("tarefas-backend");

            return ResponseEntity.noContent().build();
    }

    /**
     * Visualiza o contexto que será enviado ao agent (sem executar)
     *
     * <p>Exemplo de execução:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/sdd-executor/preview/1
     * }</pre>
     *
     * Útil para validar o contexto antes de realmente executar
     */
    //@GetMapping("/preview/{taskId}")
    public ResponseEntity<String> previewContext(@PathVariable Long taskId) {
        log.info("[SDD_EXECUTOR_CONTROLLER] GET /preview/{}", taskId);

        try {
            String context = sddTaskExecutorService.previewExecutionContext(taskId);
            return ResponseEntity.ok(context);

        } catch (IllegalArgumentException e) {
            log.error("[SDD_EXECUTOR_CONTROLLER] Erro ao fazer preview da tarefa {}", taskId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Erro: " + e.getMessage());
        } catch (Exception e) {
            log.error("[SDD_EXECUTOR_CONTROLLER] Erro ao fazer preview {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro: " + e.getMessage());
        }
    }

    /**
     * Mapeia AgentExecution para ExecutorAgentResponse
     */
    private ExecutorAgentResponse mapExecutionToResponse(AgentExecution execution) {
        return ExecutorAgentResponse.builder()
                .executionId(execution.getExecutionId())
                .input(execution.getInput() != null ?
                    (execution.getInput().length() > 500 ?
                        execution.getInput().substring(0, 500) + "..." :
                        execution.getInput())
                    : null)
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

