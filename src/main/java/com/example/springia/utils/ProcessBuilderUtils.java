package com.example.springia.utils;

import com.example.springia.dto.ProcessBuilderReturnDTO;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Utilitário para execução de comandos via {@link ProcessBuilder}.
 * <p>{@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.utils.ProcessBuilderUtils" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}</p>
 */
@Slf4j
public class ProcessBuilderUtils {

    public static ProcessBuilderReturnDTO execute(String path, String... command) {
        log.info("[EXECUTE_CMD] inicio path={} command={}", path, joinCommand(command));

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
                log.error("[EXECUTE_CMD] diretorio_invalido path={}", workingDirectory.getAbsolutePath());
                return ret;
            }

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            processBuilder.directory(workingDirectory);
            applyEnvironment(processBuilder);

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

            log.debug("[EXECUTE_CMD] exit_code={}", exitCode);

            if (exitCode != 0) {
                log.error("[EXECUTE_CMD] falha output=\n{}", ret.getOutput());
            } else {
                log.info("[EXECUTE_CMD] sucesso");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret.setExitCode(-1);
            ret.setOutput("Erro ao executar comando: " + e.getMessage());
            log.error("[EXECUTE_CMD] excecao_processbuilder", e);
        }

        return ret;
    }

    private static void applyEnvironment(ProcessBuilder processBuilder) {
        log.debug("[JAVA_ENV] inicio configuracao ambiente");

        Map<String, String> environment = processBuilder.environment();

        prependExistingDirectory(environment, "/home/gustavo/.sdkman/candidates/maven/current/bin");
        prependExistingDirectory(environment, "/home/gustavo/.sdkman/candidates/java/current/bin");
        prependExistingDirectory(environment, "/home/gustavo/.nvm/versions/node/v22.19.0/bin");

        String[] split = environment.get("PATH").split(":");
        for (String s : split) {
            log.debug("{}", s);
        }

    }

    private static void applyNodeEnvironment(Map<String, String> environment) {
        log.debug("[NODE_ENV] inicio configuracao ambiente");

        String nvmBin = System.getenv("NVM_BIN");
        if (nvmBin != null && !nvmBin.isBlank()) {
            prependToPath(environment, nvmBin);
            log.debug("[NODE_ENV] nvm_bin_adicionado path={}", nvmBin);
            return;
        }

        String currentPath = environment.getOrDefault("PATH", "");
        if (currentPath.contains("/.nvm/versions/node/")) {
            log.debug("[NODE_ENV] path_ja_contem_nvm");
            return;
        }

        log.debug("[NODE_ENV] mantendo_path_atual");
    }

    private static void prependToPath(Map<String, String> environment, String pathEntry) {
        if (pathEntry == null || pathEntry.isBlank()) {
            return;
        }

        String currentPath = environment.getOrDefault("PATH", "");
        if (currentPath.isBlank()) {
            environment.put("PATH", pathEntry);
            return;
        }

        if (!currentPath.contains(pathEntry)) {
            environment.put("PATH", pathEntry + File.pathSeparator + currentPath);
        }
    }

    private static void prependExistingDirectory(Map<String, String> environment, String pathEntry) {
        if (pathEntry == null || pathEntry.isBlank()) {
            return;
        }

        File directory = new File(pathEntry);
        if (directory.isDirectory()) {
            prependToPath(environment, directory.getAbsolutePath());
            log.debug("[PATH_ENV] diretorio_adicionado path={}", directory.getAbsolutePath());
        }
    }

    private static String joinCommand(String... command) {
        if (command == null || command.length == 0) {
            return "";
        }

        return String.join(" ", command);
    }

}
