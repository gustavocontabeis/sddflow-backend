package com.example.springia.agent.tool;

import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Ferramenta para executar comandos do shell
 */
@Slf4j
public class ExecuteCommandTool implements Tool {

    private final String basePath;

    public ExecuteCommandTool(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public String getName() {
        return "execute_command";
    }

    @Override
    public String getDescription() {
        return "Executa um comando no shell a partir do diretório base do projeto";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("command", "Comando a executar (ex: mvn clean compile, gradle build)");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String command = params.get("command");

        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command é obrigatório");
        }

        log.info("[TOOL] Executando comando: {}", command);

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.directory(new java.io.File(basePath));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Lê a saída
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        log.info("[TOOL] Comando concluído com código: {}", exitCode);

        return "Comando executado com sucesso (código: " + exitCode + ")\n" + output.toString();
    }
}

