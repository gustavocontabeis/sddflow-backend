package com.example.springia.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
public class ProcessBuilderUtils {

    public static String execute(String path, String...command) {

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

            log.info("Exit code: " + exitCode);

            if (exitCode != 0) {
                log.info("Erro detectado no build!");
                log.info(output.toString());
                return output.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}
