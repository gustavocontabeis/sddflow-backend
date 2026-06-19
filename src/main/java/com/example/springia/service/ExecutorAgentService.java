package com.example.springia.service;

import com.example.springia.agent.loop.AgentExecution;
import com.example.springia.agent.loop.AgentLoop;
import com.example.springia.agent.tool.ExecuteCommandTool;
import com.example.springia.agent.tool.discovery.DiscoveryTool;
import com.example.springia.agent.tool.GitHubListRepositoriesTool;
import com.example.springia.agent.tool.ReadFileTool;
import com.example.springia.agent.tool.ToolRegistry;
import com.example.springia.agent.tool.files.CreateDirectoryTool;
import com.example.springia.agent.tool.files.CreateFileTool;
import com.example.springia.agent.tool.files.GrepFilesTool;
import com.example.springia.agent.tool.files.ListFilesTool;
import com.example.springia.agent.tool.github.GitHubCloneRepositoryTool;
import com.example.springia.agent.tool.github.GitHubCreateCommitTool;
import com.example.springia.agent.tool.github.GitHubCreatePullRequestTool;
import com.example.springia.agent.tool.github.GitHubDiscoveryTool;
import com.example.springia.model.Project;
import com.example.springia.repository.CodeRepoRepository;
import com.example.springia.repository.ProjectRepository;
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
    private final ProjectRepository projectRepository;
    private final CodeRepoRepository codeRepoRepository;


    /**
     * Diretório base para operações de filesystem (pode ser configurado)
     */
    private String basePath;

    public ExecutorAgentService(ChatClient.Builder chatClientBuilder,
                                GitHubService gitHubService,
                                ProjectRepository projectRepository,
                                CodeRepoRepository codeRepoRepository) {
        this.gitHubService = gitHubService;
        this.projectRepository = projectRepository;
        this.codeRepoRepository = codeRepoRepository;
        this.chatClient = chatClientBuilder.build();
        this.toolRegistry = new ToolRegistry();

        // Sempre usa o diretório temporário do sistema operacional (raiz, por padrão).
        this.basePath = resolveBasePath(null);

        // Registra todas as ferramentas disponíveis
        registerTools(null);

        // Cria o agent loop com máximo de 15 passos
        this.agentLoop = new AgentLoop(this.chatClient, this.toolRegistry, 30);

        log.info("[EXECUTOR_AGENT] Serviço inicializado com basePath: {}", basePath);
    }

    /**
     * Registra todas as ferramentas disponíveis para o agente
     */
    private void registerTools(Project selectedProject) {
        toolRegistry.registerTool(new CreateFileTool(basePath));
        toolRegistry.registerTool(new ReadFileTool(basePath));
        toolRegistry.registerTool(new CreateDirectoryTool(basePath));
        toolRegistry.registerTool(new ExecuteCommandTool(basePath));
        toolRegistry.registerTool(new ListFilesTool(basePath));
        toolRegistry.registerTool(new GrepFilesTool(projectRepository, codeRepoRepository));
        toolRegistry.registerTool(new DiscoveryTool(projectRepository, codeRepoRepository, chatClient));
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
        return executeTask(taskDescription, null);
    }

    /**
     * Executa o agent com o input fornecido para um projeto selecionado
     */
    public AgentExecution executeTask(String taskDescription, Project project) throws Exception {
        registerTools(project);
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
        registerTools(null);
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

