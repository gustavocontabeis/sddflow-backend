package com.example.springia.agent.tool;

import lombok.extern.slf4j.Slf4j;
import dev.langchain4j.agent.tool.P;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Ferramenta para executar comandos do shell
 */
@Slf4j
public class ExecuteCommandTool implements Tool {


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
        params.put("command", "Comando a executar (ex: mvn clean compile, ng build)");
        params.put("codeRepo_path", "Atributo `path` da classe `CodeRepo` que é o local aonde o código do repositório está clonado");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String command = params.get("command");
        String codeRepoPath = params.get("codeRepo_path");

        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command é obrigatório");
        }

        log.info("[TOOL] Executando comando: {}", command);

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);

        pb.directory(new java.io.File(codeRepoPath.replace("/tmp/tmp/", "/tmp/")));
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

    @dev.langchain4j.agent.tool.Tool(name = "execute_command", value = "Executa um comando no shell a partir do diretório base do projeto")
    public String executeCommand(
            @P(value = "Comando a executar (ex: mvn clean compile, ng build)") String command,
            @P(value = "Atributo path de CodeRepo com o diretório do repositório") String codeRepoPath
    ) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("command", command == null ? "" : command);
        params.put("codeRepo_path", codeRepoPath == null ? "" : codeRepoPath);
        return execute(params);
    }
}

