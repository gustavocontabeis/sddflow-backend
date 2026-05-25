package com.example.springia.service;

import com.example.springia.agent.loop.AgentExecution;
import com.example.springia.agent.loop.AgentLoop;
import com.example.springia.agent.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;

/**
 * Serviço que orquestra a execução do Agent com ReAct pattern
 */
@Slf4j
@Service
public class ExecutorAgentService {

    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final AgentLoop agentLoop;

    /**
     * Diretório base para operações de filesystem (pode ser configurado)
     */
    private String basePath;

    public ExecutorAgentService(
            ChatClient.Builder chatClientBuilder,
            @Value("${clone.directory}") String cloneDirectory
    ) {
        this.chatClient = chatClientBuilder.build();
        this.toolRegistry = new ToolRegistry();

        // Configura base path via application.properties (clone.directory)
        this.basePath = new File(cloneDirectory).getAbsolutePath();

        // Registra todas as ferramentas disponíveis
        registerTools();

        // Cria o agent loop com máximo de 15 passos
        this.agentLoop = new AgentLoop(this.chatClient, this.toolRegistry, 15);

        log.info("[EXECUTOR_AGENT] Serviço inicializado com basePath: {}", basePath);
    }

    /**
     * Registra todas as ferramentas disponíveis para o agente
     */
    private void registerTools() {
        toolRegistry.registerTool(new CreateFileTool(basePath));
        toolRegistry.registerTool(new ReadFileTool(basePath));
        toolRegistry.registerTool(new CreateDirectoryTool(basePath));
        toolRegistry.registerTool(new ExecuteCommandTool(basePath));
        toolRegistry.registerTool(new ListFilesTool(basePath));
    }

    /**
     * Executa o agent com o input fornecido
     * O agent usará as tools para executar ações no filesystem
     */
    public AgentExecution executeTask(String taskDescription) throws Exception {
        log.info("[EXECUTOR_AGENT] Iniciando execução de tarefa");
        return agentLoop.execute(taskDescription);
    }

    /**
     * Define o caminho base para operações do filesystem
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
        log.info("[EXECUTOR_AGENT] basePath atualizado para: {}", basePath);
        // Re-registra as tools com o novo caminho
        registerTools();
    }

    /**
     * Retorna registrytools disponíveis
     */
    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }
}

