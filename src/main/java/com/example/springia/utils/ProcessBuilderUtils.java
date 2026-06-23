package com.example.springia.utils;

import com.example.springia.dto.ProcessBuilderReturnDTO;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Utilitário para execução de comandos via {@link ProcessBuilder}.
 *
 * <p>Exemplo para alterar o nível de log desta classe via Actuator:</p>
 *
 * <pre>
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.utils.ProcessBuilderUtils" \
 *   -H "Content-Type: application/json" \
 *   -d '{"configuredLevel":"DEBUG"}'
 * </pre>
 */
@Slf4j
public class
ProcessBuilderUtils {

    public static ProcessBuilderReturnDTO execute(String path, String...command) {

        log.info("Executando comando [{}]", String.join(" ", command));

        ProcessBuilderReturnDTO ret = new ProcessBuilderReturnDTO();

        try {

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            processBuilder.directory(new java.io.File(path));

            // Junta stdout + stderr
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug(line); // opcional: ver em tempo real
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            ret.setExitCode(exitCode);
            ret.setOutput(output.toString());
            ret.setImageName(path);

            log.debug("Exit code: " + exitCode);

            if (exitCode != 0) {
                log.error("Erro detectado no build! Output:\n{}", ret.getOutput());
            } else {
                log.info("Comando executado com sucesso.");
            }
        } catch (Exception e) {
            ret.setExitCode(-1);
            ret.setOutput("Erro ao executar comando: " + e.getMessage());
            log.error("Exceção ao executar ProcessBuilder", e);
        }

        return ret;
    }
}
