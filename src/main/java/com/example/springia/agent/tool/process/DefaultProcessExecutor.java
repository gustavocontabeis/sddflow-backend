package com.example.springia.agent.tool.process;

import com.example.springia.agent.model.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Executor de processos do agente.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.process.DefaultProcessExecutor" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 */
@Slf4j
@Component
public class DefaultProcessExecutor implements ProcessExecutor {

    @Override
    public ProcessResult execute(List<String> command, Path workingDirectory, Map<String, String> environment, Duration timeout) {
        log.info("{[EXEC_PROC]} executando processo em {}", workingDirectory);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.environment().putAll(environment);

        long started = System.nanoTime();
        try {
            Process process = processBuilder.start();
            CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                String stdout = stdoutFuture.getNow("");
                String stderr = stderrFuture.getNow("");
                return new ProcessResult(124, true, stdout, stderr, String.join(" ", command), workingDirectory.toString());
            }

            int exitCode = process.exitValue();
            String stdout = stdoutFuture.getNow("");
            String stderr = stderrFuture.getNow("");
            long elapsed = System.nanoTime() - started;
            log.info("{[EXEC_PROC]} processo finalizado em {} ms com codigo {}", Duration.ofNanos(elapsed).toMillis(), exitCode);
            return new ProcessResult(exitCode, false, stdout, stderr, String.join(" ", command), workingDirectory.toString());
        } catch (IOException e) {
            log.error("{[EXEC_PROC]} falha ao iniciar processo", e);
            throw new IllegalStateException("Falha ao executar processo", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("{[EXEC_PROC]} processo interrompido", e);
            throw new IllegalStateException("Execucao interrompida", e);
        } catch (Exception e) {
            log.error("{[EXEC_PROC]} falha inesperada ao executar processo", e);
            throw new IllegalStateException("Falha inesperada ao executar processo", e);
        }
    }

    private String readStream(InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("{[EXEC_PROC]} falha ao ler stream de processo", e);
            return "";
        }
    }
}

