package com.example.springia.controller;

import com.example.springia.agent.CodeGeneratorAgent;
import com.example.springia.agent.CodeGeneratorAzureSdkAgent;
import com.example.springia.dto.ExecutorAgentRequest;
import com.example.springia.dto.ExecutorAgentResponse;
import com.example.springia.dto.ProcessBuilderReturnDTO;
import com.example.springia.model.Project;
import com.example.springia.service.ExecutorAgentService;
import com.example.springia.service.ProjectService;
import com.example.springia.service.TaskSddService;
import com.example.springia.agent.loop.AgentExecution;
import com.example.springia.model.TaskSdd;
import com.example.springia.utils.ProcessBuilderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * Controller para executar Agent loops com ReAct pattern
 * Endpoints:
 * - POST /executor-agent/execute: Executa uma tarefa
 * - POST /executor-agent/execute-task/{taskId}: Executa uma tarefa salva no SDD
 * <p>{@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.controller.ExecutorAgentController" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}</p>
 */
@Slf4j
@RestController
@RequestMapping("/executor-agent")
@RequiredArgsConstructor
public class ExecutorAgentController {

    private final CodeGeneratorAgent codeGeneratorAgent;
    private final CodeGeneratorAzureSdkAgent codeGeneratorAzureSdkAgent;
    private final ExecutorAgentService executorAgentService;
    private final TaskSddService taskSddService;
    private final ProjectService projectService;

    /**
     * Executa o agent com uma descrição de tarefa
     *
     * <p>Exemplo de execução:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/executor-agent/execute \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "taskDescription": "Crie arquivos de teste-01.txt a teste-10.txt em clone-repo com conteúdo Hello World",
     *     "basePath": "/tmp",
     *     "projectId": 1
     *   }'
     * }</pre>
     */
    @PostMapping("/execute")
    public ResponseEntity<ExecutorAgentResponse> execute(@RequestBody ExecutorAgentRequest request) {
        log.info("[EXECUTE] POST /execute taskDescription_length={}",
            request.getTaskDescription() != null ? request.getTaskDescription().length() : 0);

        try {
            // basePath é lido do JSON e resolvido como subdiretório do temp do sistema.
            executorAgentService.setBasePath(request.getBasePath());

            Project selectedProject = resolveProject(request.getProjectId());

            // Executa o agent
            AgentExecution execution = executorAgentService.executeTask(request.getTaskDescription(), selectedProject);

            // Converte para DTO
            ExecutorAgentResponse response = mapExecutionToResponse(execution);

            log.info("[EXECUTE] Execução concluída: {} - {} passos",
                execution.getStatus(), execution.getStepCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[EXECUTE] Erro ao executar task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ExecutorAgentResponse.builder()
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * <p>{@code curl -X POST http://localhost:8080/executor-agent/execute2 -H "Content-Type: application/json" -d '{}'}</p>
     *
     * @return resposta textual do gerador de código
     */
    @PostMapping("/execute2")
    public ResponseEntity<String> execute2() {
        log.info("[EXECUTE_2] POST /execute2");
        try {
            resolveProject(1L);

            String userPrompt = """
                    Crie um Crud de Pessoa (id, nome, email) somente no Backend.
                    Para isso gere:
                    - Gere Classes de entidade JPA
                    - Repository
                    - Service
                    - Endpoints REST.
                    """;
            String response = codeGeneratorAgent.generateJavaCode(userPrompt);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[EXECUTE_2] Erro ao executar task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("X");
        }
    }

    /**
     * curl -X GET http://localhost:8080/executor-agent/execute-azure-sdk -H "Content-Type: application/text"
     * @return
     */
    @GetMapping("/execute-azure-sdk")
    public ResponseEntity<String> executeAzureSdk() {
        log.info("[EXECUTE_2] POST /execute-azure-sdk");

        try {
            String userPrompt = """
                    Crie um Crud de Pessoa (id, nome, email) somente no Backend.
                    Para isso gere:
                    - Gere Classes de entidade JPA
                    - Repository
                    - Service
                    - Endpoints REST.
                    """;
            codeGeneratorAzureSdkAgent.executar(userPrompt);;
            return ResponseEntity.ok("{}");

        } catch (Exception e) {
            e.printStackTrace();
            log.error("[EXECUTE_2] Erro ao executar task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("X");
        }
    }

    /**
     * <p>{@code curl "http://localhost:8080/executor-agent/teste-builder?path=/tmp/tarefas-backend&goals=clean%20package"}</p>
     * <p>{@code curl "http://localhost:8080/executor-agent/teste-builder?path=/tmp/tarefas-frontend&goals=ng%20build"}</p>
     */
    @GetMapping("/teste-builder")
    public ResponseEntity<String> executeTesteBuilder() {
        try {
            ProcessBuilderReturnDTO execute = null;
            execute = ProcessBuilderUtils.execute("/tmp/tarefas-backend", "./mvnw", "clean", "package");
            log.info("[TESTE_BUILDER] exitCode={}", execute.getExitCode());
            log.info("[TESTE_BUILDER] output={}", execute.getOutput());

            execute = ProcessBuilderUtils.execute("/tmp/tarefas-frontend", "node", "-v");
            log.info("[TESTE_BUILDER] exitCode={}", execute.getExitCode());
            log.info("[TESTE_BUILDER] output={}", execute.getOutput());

            execute = ProcessBuilderUtils.execute("/tmp/tarefas-frontend", "ng", "build");
            log.info("[TESTE_BUILDER] exitCode={}", execute.getExitCode());
            log.info("[TESTE_BUILDER] output={}", execute.getOutput());

            if (!execute.isOk()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(execute.getOutput());
            }

            return ResponseEntity.ok(execute.getOutput());

        } catch (Exception e) {
            log.error("[TESTE_BUILDER] Erro ao executar task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    /**
     * Executa um TaskSdd salvo no banco de dados
     *
     * <p>Exemplo de execução:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/executor-agent/execute-task/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "basePath": "springia-workspace"
     *   }'
     * }</pre>
     */
    //@PostMapping("/execute-task/{taskId}")
    public ResponseEntity<ExecutorAgentResponse> executeTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) ExecutorAgentRequest request) {

        String basePath = request != null ? request.getBasePath() : null;

        log.info("[EXECUTE_TASK] POST /execute-task/{} basePath={}", taskId, basePath);

        try {
            // Busca a tarefa no banco
            TaskSdd taskSdd = taskSddService.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("TaskSdd não encontrada: " + taskId));

            String taskContent = taskSdd.getContent();
            log.info("[EXECUTE_TASK] TaskSdd encontrada: {} bytes", taskContent.length());

            // basePath é lido do JSON e resolvido como subdiretório do temp do sistema.
            executorAgentService.setBasePath(basePath);

            Long projectId = request != null ? request.getProjectId() : null;
            Project selectedProject = resolveProject(projectId);

            // Executa o agent com o conteúdo da tarefa
            AgentExecution execution = executorAgentService.executeTask(taskContent, selectedProject);

            // Converte para DTO
            ExecutorAgentResponse response = mapExecutionToResponse(execution);

            log.info("[EXECUTE_TASK] Task {} executada: {} - {} passos",
                taskId, execution.getStatus(), execution.getStepCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[EXECUTE_TASK] Erro ao executar task {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ExecutorAgentResponse.builder()
                            .status("ERROR")
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * Retorna as ferramentas disponíveis para o agent
     * <p>{@code curl -i http://localhost:8080/executor-agent/tools}</p>
     */
    @GetMapping("/tools")
    public ResponseEntity<String> getAvailableTools() {
        log.info("[GET_TOOLS] GET /tools");
        String toolsDescription = executorAgentService.getToolRegistry().getToolsDescription();
        return ResponseEntity.ok(toolsDescription);
    }

    /**
     * Mapeia AgentExecution para ExecutorAgentResponse
     */
    private ExecutorAgentResponse mapExecutionToResponse(AgentExecution execution) {
        log.debug("[MAP_EXECUTION] Mapeando executionId={}", execution.getExecutionId());
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

    private Project resolveProject(Long projectId) {
        log.debug("[RESOLVE_PROJECT] Resolvendo projectId={}", projectId);
        if (projectId == null) {
            return null;
        }

        Project project = projectService.findById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Projeto não encontrado: " + projectId);
        }
        return project;
    }

    private Path resolveBuilderPath(String path) {
        log.debug("[RESOLVE_PATH] Resolvendo path={}", path);

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Informe um diretório válido no parâmetro 'path'.");
        }

        Path repoPath = Paths.get(path).toAbsolutePath().normalize();
        if (!Files.isDirectory(repoPath)) {
            throw new IllegalArgumentException("Diretório não encontrado: " + repoPath);
        }

        return repoPath;
    }

    private String[] buildMavenCommand(String mavenCommand, String goals) {
        log.debug("[BUILD_COMMAND] Montando comando Maven goals={}", goals);

        String effectiveGoals = (goals == null || goals.isBlank()) ? "clean package" : goals.trim();
        String[] goalArgs = effectiveGoals.split("\\s+");
        String[] command = new String[goalArgs.length + 1];
        command[0] = mavenCommand;

        System.arraycopy(goalArgs, 0, command, 1, goalArgs.length);
        return command;
    }
}

