package com.example.springia.agent.tool.compiler;

import com.example.springia.agent.model.CompilationResult;
import com.example.springia.agent.model.CompileBy;
import com.example.springia.agent.model.ProcessResult;
import com.example.springia.agent.tool.process.ProcessExecutor;
import com.example.springia.config.AgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Compila e executa testes dos projetos impactados.
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.compiler.CompilationTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompilationTool {

    private final ProcessExecutor processExecutor;
    private final AgentProperties agentProperties;

    @Tool(name = "compile_backend", description = "Compila e executa testes do backend usando command ou docker")
    public CompilationResult compileBackend(CompileBy compileBy) {
        log.info("{[COMP_BACKEND]} compilando backend com {}", compileBy);
        return compile("backend", agentProperties.getBackendRoot(), compileBy, List.of("mvn", "clean", "test"), List.of("mvn", "clean", "test"));
    }

    @Tool(name = "compile_frontend", description = "Compila e executa testes do frontend usando command ou docker")
    public CompilationResult compileFrontend(CompileBy compileBy) {
        log.info("{[COMP_FRONTEND]} compilando frontend com {}", compileBy);
        return compile("frontend", agentProperties.getFrontendRoot(), compileBy, List.of("ng", "build"), List.of("ng", "build"));
    }

    @Tool(name = "compile_project", description = "Compila um projeto usando comandos e contexto informados")
    public CompilationResult compile(String projectName, @ToolParam(description = "Caminho absoluto do projeto") String projectRoot, CompileBy compileBy, List<String> command, List<String> testCommand) {
        log.info("{[COMPILE]} compilando {} com {}", projectName, compileBy);
        Path normalizedProjectRoot = Path.of(projectRoot).toAbsolutePath().normalize();
        Duration timeout = agentProperties.getCompilationTimeout();
        long start = System.nanoTime();

        if (compileBy == CompileBy.COMMAND) {
            ProcessResult result = processExecutor.execute(command, normalizedProjectRoot, Map.of(), timeout);
            return toCompilationResult(projectName, normalizedProjectRoot, compileBy, command, result, start);
        }

        return compileWithDocker(projectName, normalizedProjectRoot, command, testCommand, timeout, start);
    }

    private CompilationResult compileWithDocker(String projectName, Path projectRoot, List<String> buildCommand, List<String> testCommand, Duration timeout, long start) {
        Path dockerfile = projectRoot.resolve("Dockerfile");
        if (!Files.exists(dockerfile)) {
            String message = "Dockerfile nao encontrado em " + dockerfile;
            log.error("{[COMPILE]} {}", message);
            return new CompilationResult(CompileBy.DOCKER, projectName, projectRoot.toString(), "docker build/run", false, false, 1, Duration.ofNanos(System.nanoTime() - start), message, message);
        }

        String imageTag = "springia-" + projectName + '-' + System.currentTimeMillis();
        ProcessResult build = processExecutor.execute(List.of("docker", "build", "-f", "Dockerfile", "-t", imageTag, "."), projectRoot, Map.of(), timeout);
        if (!build.success()) {
            return toCompilationResult(projectName, projectRoot, CompileBy.DOCKER, buildCommand, build, start);
        }

        ProcessResult run = processExecutor.execute(List.of("docker", "run", "--rm", imageTag, "sh", "-lc", String.join(" ", testCommand)), projectRoot, Map.of(), timeout);
        return toCompilationResult(projectName, projectRoot, CompileBy.DOCKER, testCommand, run, start, build.stdout() + "\n" + run.stdout(), build.stderr() + "\n" + run.stderr());
    }

    private CompilationResult toCompilationResult(String projectName, Path projectRoot, CompileBy compileBy, List<String> command, ProcessResult result, long start) {
        return toCompilationResult(projectName, projectRoot, compileBy, command, result, start, result.stdout(), result.stderr());
    }

    private CompilationResult toCompilationResult(String projectName, Path projectRoot, CompileBy compileBy, List<String> command, ProcessResult result, long start, String output, String errorOutput) {
        Duration duration = Duration.ofNanos(System.nanoTime() - start);
        String commandLine = String.join(" ", command);
        return new CompilationResult(compileBy, projectName, projectRoot.toString(), commandLine, result.success(), result.timedOut(), result.exitCode(), duration, output, errorOutput);
    }
}


