package com.example.springia.utils;

import com.example.springia.dto.ProcessBuilderReturnDTO;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
public class
ProcessBuilderUtils {

    public static ProcessBuilderReturnDTO execute(String path, String...command) {

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
                    log.info(line); // opcional: ver em tempo real
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            ret.setExitCode(exitCode);
            ret.setOutput(output.toString());

            log.info("Exit code: " + exitCode);

            if (exitCode != 0) {
                log.error("Erro detectado no build! Output:\n{}", ret.getOutput());
            } else {
                log.info("Comando executado com sucesso. Output:\n{}", ret.getOutput());
            }
        } catch (Exception e) {
            ret.setExitCode(-1);
            ret.setOutput("Erro ao executar comando: " + e.getMessage());
            log.error("Exceção ao executar ProcessBuilder", e);
        }

        return ret;
    }
}
