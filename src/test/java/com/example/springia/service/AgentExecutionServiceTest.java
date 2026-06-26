package com.example.springia.service;

import com.example.springia.agent.model.ChangeOperation;
import com.example.springia.agent.model.CompilationResult;
import com.example.springia.agent.model.CompileBy;
import com.example.springia.agent.model.FileChangeCommand;
import com.example.springia.agent.model.GeneratedChangeSet;
import com.example.springia.agent.model.ProjectDiscoveryReport;
import com.example.springia.agent.model.ProjectDiscoverySnapshot;
import com.example.springia.agent.service.PromptGenerationClient;
import com.example.springia.config.AgentProperties;
import com.example.springia.dto.ExecutionRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentExecutionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExecuteAndPersistSuccessfulIteration() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend"));
        Path frontend = Files.createDirectories(tempDir.resolve("frontend"));

        AgentProperties props = newProperties(backend, frontend, 1);
        PromptGenerationClient promptGenerationClient = mock(PromptGenerationClient.class);
        configureHappyPath(promptGenerationClient, backend, frontend);
        when(promptGenerationClient.generate(anyString())).thenReturn(JsonUtils.toJson(new GeneratedChangeSet("sum", "notes", List.of(CompileBy.COMMAND), List.of())));

        AgentExecutionService service = newService(props, promptGenerationClient);
        var response = service.execute(new ExecutionRequest("criar", CompileBy.COMMAND));

        assertEquals("SUCESSO", response.status());
        assertEquals(1, response.currentAttempt());
        assertTrue(!response.attempts().isEmpty());
    }

    @Test
    void shouldParseLlmResponseUsingFilesFieldAlias() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend-files-alias"));
        Path frontend = Files.createDirectories(tempDir.resolve("frontend-files-alias"));

        AgentProperties props = newProperties(backend, frontend, 1);
        PromptGenerationClient promptGenerationClient = mock(PromptGenerationClient.class);
        configureHappyPath(promptGenerationClient, backend, frontend);

        GeneratedChangeSet generated = new GeneratedChangeSet(
                "sum",
                "notes",
                List.of(CompileBy.COMMAND),
                List.of(new FileChangeCommand(backend.resolve("README.md").toString(), ChangeOperation.UPDATE, "conteudo", "atualiza"))
        );
        when(promptGenerationClient.generate(anyString())).thenReturn(JsonUtils.toJson(generated).replace("\"changes\"", "\"files\""));

        AgentExecutionService service = newService(props, promptGenerationClient);
        var response = service.execute(new ExecutionRequest("criar", CompileBy.COMMAND));

        assertEquals("SUCESSO", response.status());
        verify(promptGenerationClient).writeChanges(argThat(list -> list != null && list.size() == 1 && list.getFirst().filePath().equals(backend.resolve("README.md").toString())));
    }

    @Test
    void shouldParseLlmResponseWithLowercaseOperation() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend-lowercase-operation"));
        Path frontend = Files.createDirectories(tempDir.resolve("frontend-lowercase-operation"));

        AgentProperties props = newProperties(backend, frontend, 1);
        PromptGenerationClient promptGenerationClient = mock(PromptGenerationClient.class);
        configureHappyPath(promptGenerationClient, backend, frontend);

        GeneratedChangeSet generated = new GeneratedChangeSet(
                "sum",
                "notes",
                List.of(CompileBy.COMMAND),
                List.of(new FileChangeCommand(backend.resolve("README.md").toString(), ChangeOperation.CREATE, "conteudo", "cria"))
        );
        when(promptGenerationClient.generate(anyString())).thenReturn(JsonUtils.toJson(generated).replace("\"CREATE\"", "\"create\""));

        AgentExecutionService service = newService(props, promptGenerationClient);
        var response = service.execute(new ExecutionRequest("criar", CompileBy.COMMAND));

        assertEquals("SUCESSO", response.status());
        verify(promptGenerationClient).writeChanges(argThat(list -> list != null && list.size() == 1 && list.getFirst().operation() == ChangeOperation.CREATE));
    }

    @Test
    void shouldRetryWhenFileWriteFailsDueToFullReplaceGuardrail() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend-write-failure"));
        Path frontend = Files.createDirectories(tempDir.resolve("frontend-write-failure"));

        AgentProperties props = newProperties(backend, frontend, 2);
        PromptGenerationClient promptGenerationClient = mock(PromptGenerationClient.class);
        configureHappyPath(promptGenerationClient, backend, frontend);

        GeneratedChangeSet generated = new GeneratedChangeSet(
                "sum",
                "notes",
                List.of(CompileBy.COMMAND),
                List.of(new FileChangeCommand(backend.resolve("README.md").toString(), ChangeOperation.UPDATE, "conteudo", "atualiza"))
        );
        when(promptGenerationClient.generate(anyString())).thenReturn(JsonUtils.toJson(generated));
        when(promptGenerationClient.writeChanges(any()))
                .thenThrow(new IllegalArgumentException("Substituicao completa bloqueada para update sem allowFullReplace=true"))
                .thenReturn(List.of());
        when(promptGenerationClient.buildRepairPrompt(anyString(), any(), anyString(), anyString())).thenReturn("repair");

        AgentExecutionService service = newService(props, promptGenerationClient);
        var response = service.execute(new ExecutionRequest("criar", CompileBy.COMMAND));

        assertEquals("SUCESSO", response.status());
        assertEquals(2, response.currentAttempt());
        verify(promptGenerationClient, times(2)).generate(anyString());
        verify(promptGenerationClient, times(2)).writeChanges(any());
        verify(promptGenerationClient, times(1)).compile(any());
        verify(promptGenerationClient, times(1)).buildRepairPrompt(anyString(), any(), anyString(), contains("allowFullReplace=true"));
    }

    private AgentExecutionService newService(AgentProperties props, PromptGenerationClient promptGenerationClient) {
        ExecutionRepository executionRepository = mock(ExecutionRepository.class);
        AttemptRepository attemptRepository = mock(AttemptRepository.class);
        ArtifactChangeRepository artifactChangeRepository = mock(ArtifactChangeRepository.class);
        CompilationLogRepository compilationLogRepository = mock(CompilationLogRepository.class);
        ExecutionStatusRepository executionStatusRepository = mock(ExecutionStatusRepository.class);

        when(executionStatusRepository.findByCoCodigo(anyString())).thenReturn(Optional.empty());
        when(executionStatusRepository.save(any())).thenAnswer(invocation -> setId(invocation.getArgument(0), 1L));
        when(executionRepository.save(any())).thenAnswer(invocation -> setId(invocation.getArgument(0), 10L));
        when(attemptRepository.save(any())).thenAnswer(invocation -> setId(invocation.getArgument(0), 20L));
        when(compilationLogRepository.save(any())).thenAnswer(invocation -> setId(invocation.getArgument(0), 30L));

        return new AgentExecutionService(
                props,
                promptGenerationClient,
                executionRepository,
                attemptRepository,
                artifactChangeRepository,
                compilationLogRepository,
                executionStatusRepository
        );
    }

    private void configureHappyPath(PromptGenerationClient promptGenerationClient, Path backend, Path frontend) {
        ProjectDiscoverySnapshot snapshot = new ProjectDiscoverySnapshot(
                new ProjectDiscoveryReport(backend, true, true, List.of(), List.of(), List.of(), List.of(), java.util.Map.of(), "backend"),
                new ProjectDiscoveryReport(frontend, true, true, List.of(), List.of(), List.of(), List.of(), java.util.Map.of(), "frontend"),
                "full"
        );
        when(promptGenerationClient.discover()).thenReturn(snapshot);
        when(promptGenerationClient.buildInitialPrompt(anyString(), any())).thenReturn("plan");
        doNothing().when(promptGenerationClient).validateScope(any(), any());
        when(promptGenerationClient.writeChanges(any())).thenReturn(List.of());
        when(promptGenerationClient.compile(any())).thenReturn(List.of(
                new CompilationResult(CompileBy.COMMAND, "backend", backend.toString(), "mvn clean test", true, false, 0, Duration.ZERO, "ok", ""),
                new CompilationResult(CompileBy.COMMAND, "frontend", frontend.toString(), "ng build", true, false, 0, Duration.ZERO, "ok", "")
        ));
        when(promptGenerationClient.isSuccessful(any())).thenReturn(true);
        when(promptGenerationClient.buildFeedback(any(), anyString(), anyString())).thenReturn("feedback");
        when(promptGenerationClient.buildRepairPrompt(anyString(), any(), anyString(), anyString())).thenReturn("repair");
    }

    private AgentProperties newProperties(Path backend, Path frontend, int maxIterations) {
        AgentProperties props = new AgentProperties();
        props.setBackendRoot(backend.toString());
        props.setFrontendRoot(frontend.toString());
        props.setMaxIterations(maxIterations);
        return props;
    }

    private <T> T setId(T entity, Long id) {
        if (entity instanceof Execution e) e.setNuId(id);
        if (entity instanceof Attempt a) a.setNuId(id);
        if (entity instanceof ArtifactChange a) a.setNuId(id);
        if (entity instanceof CompilationLog c) c.setNuId(id);
        if (entity instanceof ExecutionStatus s) s.setNuId(id);
        return entity;
    }
}


