package com.example.springia.service;

import com.example.springia.agent.loop.AgentExecution;
import com.example.springia.agent.loop.AgentLoop;
import com.example.springia.agent.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serviço que orquestra a execução do Agent com ReAct pattern
 */
@Slf4j
@Service
public class ExecutorAgentService {

    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final AgentLoop agentLoop;
    private final GitHubService gitHubService;

    /**
     * Diretório base para operações de filesystem (pode ser configurado)
     */
    private String basePath;

    public ExecutorAgentService(
            ChatClient.Builder chatClientBuilder,
            GitHubService gitHubService
    ) {
        this.gitHubService = gitHubService;
        this.chatClient = chatClientBuilder.build();
        this.toolRegistry = new ToolRegistry();

        // Sempre usa o diretório temporário do sistema operacional (raiz, por padrão).
        this.basePath = resolveBasePath(null);

        // Registra todas as ferramentas disponíveis
        registerTools();

        // Cria o agent loop com máximo de 15 passos
        this.agentLoop = new AgentLoop(this.chatClient, this.toolRegistry, 30);

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
        toolRegistry.registerTool(new GitHubListRepositoriesTool(gitHubService));
        toolRegistry.registerTool(new GitHubCloneRepositoryTool(gitHubService));
        toolRegistry.registerTool(new GitHubCreateCommitTool(gitHubService));
        toolRegistry.registerTool(new GitHubCreatePullRequestTool(gitHubService));
        toolRegistry.registerTool(new GitHubDiscoveryTool(gitHubService));
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
        this.basePath = resolveBasePath(basePath);
        log.info("[EXECUTOR_AGENT] basePath configurado como: {}", this.basePath);
        // Re-registra as tools com o novo caminho
        registerTools();
    }

    private String resolveBasePath(String requestedBasePath) {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();

        if (requestedBasePath == null || requestedBasePath.isBlank()) {
            ensureDirectoryExists(tempRoot);
            return tempRoot.toString();
        }

        Path candidate = Path.of(requestedBasePath.trim());
        Path resolved = candidate.isAbsolute() ? candidate.toAbsolutePath().normalize() : tempRoot.resolve(candidate).normalize();

        if (!resolved.startsWith(tempRoot)) {
            throw new IllegalArgumentException("basePath deve ficar dentro do diretório temporário do sistema: " + tempRoot);
        }

        ensureDirectoryExists(resolved);
        return resolved.toString();
    }

    private void ensureDirectoryExists(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível preparar o diretório base: " + path, e);
        }
    }

    /**
     * Retorna registrytools disponíveis
     */
    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }
}

