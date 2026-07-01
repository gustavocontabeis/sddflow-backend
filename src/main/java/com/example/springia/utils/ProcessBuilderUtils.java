package com.example.springia.utils;

import com.example.springia.dto.ProcessBuilderReturnDTO;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Utilitário para execução de comandos via {@link ProcessBuilder}.
 * <p>{@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.utils.ProcessBuilderUtils" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}</p>
 */
@Slf4j
public class ProcessBuilderUtils {

    public static ProcessBuilderReturnDTO execute(String path, String... command) {
        log.info("[EXECUTE] Executando comando={} path={}", String.join(" ", command), path);

        ProcessBuilderReturnDTO ret = new ProcessBuilderReturnDTO();

        if (path == null || path.isBlank()) {
            ret.setExitCode(-1);
            ret.setOutput("Erro ao executar comando: path não informado.");
            log.error("[EXECUTE] Path não informado para execução do comando");
            return ret;
        }

        if (command == null || command.length == 0) {
            ret.setExitCode(-1);
            ret.setOutput("Erro ao executar comando: comando não informado.");
            log.error("[EXECUTE] Comando não informado para execução no path={}", path);
            return ret;
        }

        try {
            File workingDirectory = new File(path);
            if (!workingDirectory.isDirectory()) {
                ret.setExitCode(-1);
                ret.setOutput("Erro ao executar comando: diretório inválido " + workingDirectory.getAbsolutePath());
                log.error("[EXECUTE] Diretório inválido path={}", workingDirectory.getAbsolutePath());
                return ret;
            }

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            processBuilder.directory(workingDirectory);
            applyJavaEnvironment(processBuilder);

            // Junta stdout + stderr
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    log.trace("[READ_OUTPUT] {}", line);
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            ret.setExitCode(exitCode);
            ret.setOutput(output.toString());
            ret.setImageName(path);

            log.debug("[EXECUTE] Exit code={}", exitCode);

            if (exitCode != 0) {
                log.error("[EXECUTE] Erro detectado no build. Output:\n{}", ret.getOutput());
            } else {
                log.info("[EXECUTE] Comando executado com sucesso.");
            }
        } catch (Exception e) {
            ret.setExitCode(-1);
            ret.setOutput("Erro ao executar comando: " + e.getMessage());
            log.error("[EXECUTE] Exceção ao executar ProcessBuilder", e);
        }

        return ret;
    }

    private static void applyJavaEnvironment(ProcessBuilder processBuilder) {
        log.debug("[JAVA_ENV] Configurando JAVA_HOME para o processo filho");

        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isBlank()) {
            return;
        }

        Map<String, String> environment = processBuilder.environment();
        environment.put("JAVA_HOME", javaHome);

        String javaBin = new File(javaHome, "bin").getAbsolutePath();
        String currentPath = environment.getOrDefault("PATH", "");
        if (!currentPath.contains(javaBin)) {
            environment.put("PATH", javaBin + File.pathSeparator + currentPath);
        }
    }
}
