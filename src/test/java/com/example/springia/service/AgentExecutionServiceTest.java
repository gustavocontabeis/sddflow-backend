package com.example.springia.service;

import com.example.springia.agent.model.ChangeOperation;
import com.example.springia.agent.model.CompilationResult;
import com.example.springia.agent.model.CompileBy;
import com.example.springia.agent.model.FileChangeCommand;
import com.example.springia.agent.model.FileWriteResult;
import com.example.springia.agent.model.GeneratedChangeSet;
import com.example.springia.agent.model.ProjectDiscoveryReport;
import com.example.springia.agent.model.ProjectDiscoverySnapshot;
import com.example.springia.agent.tool.compiler.CompilationTool;
import com.example.springia.agent.tool.discovery.ProjectDiscoveryTool;
import com.example.springia.agent.tool.feedback.FeedbackTool;
import com.example.springia.agent.tool.files.FileWriteTool;
import com.example.springia.config.AgentProperties;
import com.example.springia.dto.ExecutionRequest;
import com.example.springia.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
        ProjectDiscoveryTool discoveryTool = mock(ProjectDiscoveryTool.class);
        FileWriteTool fileWriteTool = mock(FileWriteTool.class);
        CompilationTool compilationTool = mock(CompilationTool.class);
        FeedbackTool feedbackTool = mock(FeedbackTool.class);
        configureHappyPath(discoveryTool, fileWriteTool, compilationTool, feedbackTool, backend, frontend);

        AgentExecutionService service = newService(props, discoveryTool, fileWriteTool, compilationTool, feedbackTool);
        doReturn(JsonUtils.toJson(new GeneratedChangeSet("sum", "notes", List.of(CompileBy.COMMAND), List.of())))
                .when(service)
                .generate(anyString());
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
        ProjectDiscoveryTool discoveryTool = mock(ProjectDiscoveryTool.class);
        FileWriteTool fileWriteTool = mock(FileWriteTool.class);
        CompilationTool compilationTool = mock(CompilationTool.class);
        FeedbackTool feedbackTool = mock(FeedbackTool.class);
        configureHappyPath(discoveryTool, fileWriteTool, compilationTool, feedbackTool, backend, frontend);

        GeneratedChangeSet generated = new GeneratedChangeSet(
                "sum",
                "notes",
                List.of(CompileBy.COMMAND),
                List.of(new FileChangeCommand(backend.resolve("README.md").toString(), ChangeOperation.UPDATE, "conteudo", "atualiza"))
        );
        AgentExecutionService service = newService(props, discoveryTool, fileWriteTool, compilationTool, feedbackTool);
        doReturn(JsonUtils.toJson(generated).replace("\"changes\"", "\"files\""))
                .when(service)
                .generate(anyString());

        var response = service.execute(new ExecutionRequest("criar", CompileBy.COMMAND));

        assertEquals("SUCESSO", response.status());
        verify(fileWriteTool).write(
                argThat(list -> list != null && list.size() == 1 && list.getFirst().filePath().equals(backend.resolve("README.md").toString())),
                anyString()
        );
    }

    @Test
    void shouldParseLlmResponseWithLowercaseOperation() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend-lowercase-operation"));
        Path frontend = Files.createDirectories(tempDir.resolve("frontend-lowercase-operation"));

        AgentProperties props = newProperties(backend, frontend, 1);
        ProjectDiscoveryTool discoveryTool = mock(ProjectDiscoveryTool.class);
        FileWriteTool fileWriteTool = mock(FileWriteTool.class);
        CompilationTool compilationTool = mock(CompilationTool.class);
        FeedbackTool feedbackTool = mock(FeedbackTool.class);
        configureHappyPath(discoveryTool, fileWriteTool, compilationTool, feedbackTool, backend, frontend);

        GeneratedChangeSet generated = new GeneratedChangeSet(
                "sum",
                "notes",
                List.of(CompileBy.COMMAND),
                List.of(new FileChangeCommand(backend.resolve("README.md").toString(), ChangeOperation.CREATE, "conteudo", "cria"))
        );
        AgentExecutionService service = newService(props, discoveryTool, fileWriteTool, compilationTool, feedbackTool);
        doReturn(JsonUtils.toJson(generated).replace("\"CREATE\"", "\"create\""))
                .when(service)
                .generate(anyString());

        var response = service.execute(new ExecutionRequest("criar", CompileBy.COMMAND));

        assertEquals("SUCESSO", response.status());
        verify(fileWriteTool).write(
                argThat(list -> list != null && list.size() == 1 && list.getFirst().operation() == ChangeOperation.CREATE),
                anyString()
        );
    }

    @Test
    void shouldRetryWhenFileWriteFailsDueToFullReplaceGuardrail() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend-write-failure"));
        Path frontend = Files.createDirectories(tempDir.resolve("frontend-write-failure"));

        AgentProperties props = newProperties(backend, frontend, 2);
        ProjectDiscoveryTool discoveryTool = mock(ProjectDiscoveryTool.class);
        FileWriteTool fileWriteTool = mock(FileWriteTool.class);
        CompilationTool compilationTool = mock(CompilationTool.class);
        FeedbackTool feedbackTool = mock(FeedbackTool.class);
        configureHappyPath(discoveryTool, fileWriteTool, compilationTool, feedbackTool, backend, frontend);

        GeneratedChangeSet generated = new GeneratedChangeSet(
                "sum",
                "notes",
                List.of(CompileBy.COMMAND),
                List.of(new FileChangeCommand(backend.resolve("README.md").toString(), ChangeOperation.UPDATE, "conteudo", "atualiza"))
        );
        AgentExecutionService service = newService(props, discoveryTool, fileWriteTool, compilationTool, feedbackTool);
        doReturn(JsonUtils.toJson(generated)).when(service).generate(anyString());
        when(fileWriteTool.write(any(), anyString()))
                .thenThrow(new IllegalArgumentException("Substituicao completa bloqueada para update sem allowFullReplace=true"))
                .thenReturn(List.of());

        var response = service.execute(new ExecutionRequest("criar", CompileBy.COMMAND));

        assertEquals("SUCESSO", response.status());
        assertEquals(2, response.currentAttempt());
        verify(service, times(2)).generate(anyString());
        verify(fileWriteTool, times(2)).write(any(), anyString());
        verify(compilationTool, times(1)).compileBackend(CompileBy.COMMAND);
        verify(compilationTool, times(1)).compileFrontend(CompileBy.COMMAND);
    }

    private AgentExecutionService newService(
            AgentProperties props,
            ProjectDiscoveryTool discoveryTool,
            FileWriteTool fileWriteTool,
            CompilationTool compilationTool,
            FeedbackTool feedbackTool
    ) {
        return spy(new AgentExecutionService(props, discoveryTool, fileWriteTool, compilationTool, feedbackTool));
    }

    private void configureHappyPath(
            ProjectDiscoveryTool discoveryTool,
            FileWriteTool fileWriteTool,
            CompilationTool compilationTool,
            FeedbackTool feedbackTool,
            Path backend,
            Path frontend
    ) {
        ProjectDiscoverySnapshot snapshot = new ProjectDiscoverySnapshot(
                new ProjectDiscoveryReport(backend, true, true, List.of(), List.of(), List.of(), List.of(), java.util.Map.of(), "backend"),
                new ProjectDiscoveryReport(frontend, true, true, List.of(), List.of(), List.of(), List.of(), java.util.Map.of(), "frontend"),
                "full"
        );
        when(discoveryTool.discover()).thenReturn(snapshot);
        when(fileWriteTool.write(any(), anyString())).thenReturn(List.of(
                new FileWriteResult(backend.resolve("README.md").toString(), ChangeOperation.UPDATE, true, null, null, "ok")
        ));
        when(compilationTool.compileBackend(any())).thenReturn(
                new CompilationResult(CompileBy.COMMAND, "backend", backend.toString(), "mvn clean test", true, false, 0, Duration.ZERO, "ok", "")
        );
        when(compilationTool.compileFrontend(any())).thenReturn(
                new CompilationResult(CompileBy.COMMAND, "frontend", frontend.toString(), "ng build", true, false, 0, Duration.ZERO, "ok", "")
        );
        when(feedbackTool.buildFeedback(any(), any(), anyString(), anyString())).thenReturn("feedback");
    }

    private AgentProperties newProperties(Path backend, Path frontend, int maxIterations) {
        AgentProperties props = new AgentProperties();
        props.setBackendRoot(backend.toString());
        props.setFrontendRoot(frontend.toString());
        props.setMaxIterations(maxIterations);
        return props;
    }

}


