package com.example.springia.agent.service;

import com.example.springia.agent.advisor.PlanningAdvisor;
import com.example.springia.agent.advisor.RepairAdvisor;
import com.example.springia.agent.advisor.ScopeAdvisor;
import com.example.springia.agent.advisor.VerificationAdvisor;
import com.example.springia.agent.model.CompilationResult;
import com.example.springia.agent.model.CompileBy;
import com.example.springia.agent.model.FileChangeCommand;
import com.example.springia.agent.model.FileWriteResult;
import com.example.springia.agent.model.GeneratedChangeSet;
import com.example.springia.agent.model.ProjectDiscoverySnapshot;
import com.example.springia.agent.tool.compiler.CompilationTool;
import com.example.springia.agent.tool.diff.CodeDiffTool;
import com.example.springia.agent.tool.discovery.ProjectDiscoveryTool;
import com.example.springia.agent.tool.feedback.FeedbackTool;
import com.example.springia.agent.tool.files.FileReadTool;
import com.example.springia.agent.tool.files.FileWriteTool;
import com.example.springia.config.AgentProperties;
import com.example.springia.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Integra o agente com o provider Spring AI.
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.service.SpringAiPromptGenerationClient" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiPromptGenerationClient implements PromptGenerationClient {

    private static final String DEFAULT_TOOL_CALLING_SYSTEM_PROMPT_LOCATION = "classpath:prompts/tool-calling-system-prompt.md";

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ResourceLoader resourceLoader;
    private final AgentProperties agentProperties;
    private final ProjectDiscoveryTool projectDiscoveryTool;
    private final FileReadTool fileReadTool;
    private final FileWriteTool fileWriteTool;
    private final CompilationTool compilationTool;
    private final FeedbackTool feedbackTool;
    private final CodeDiffTool codeDiffTool;
    private final PlanningAdvisor planningAdvisor;
    private final RepairAdvisor repairAdvisor;
    private final ScopeAdvisor scopeAdvisor;
    private final VerificationAdvisor verificationAdvisor;

    @Override
    public ProjectDiscoverySnapshot discover() {
        log.info("{[DISCOVER]} iniciando discovery de projetos");
        ProjectDiscoverySnapshot snapshot = projectDiscoveryTool.discover();
        log.info("{[DISCOVER_RET]} discovery concluido");
        return snapshot;
    }

    @Override
    public String buildInitialPrompt(String taskDescription, ProjectDiscoverySnapshot discovery) {
        log.info("{[BUILD_INIT]} montando prompt inicial taskDescription='{}'", taskDescription);
        String plan = planningAdvisor.buildPlan(taskDescription, discovery);
        log.info("{[BUILD_INIT_RT]} prompt inicial montado; length={}", plan == null ? 0 : plan.length());
        return plan;
    }

    @Override
    public String buildRepairPrompt(String taskDescription, ProjectDiscoverySnapshot discovery, String previousResponse, String feedback) {
        log.info("{[BUILD_REPAIR]} montando prompt de reparo taskDescription='{}'", taskDescription);
        String prompt = repairAdvisor.buildRepairPrompt(taskDescription, discovery, previousResponse, feedback);
        log.info("{[BUILD_REPAIR_RT]} prompt de reparo montado; length={}", prompt == null ? 0 : prompt.length());
        return prompt;
    }

    private volatile String cachedToolCallingSystemPrompt;

    @Override
    public String generate(String prompt) {
        log.info("{[GENERATE_LLM]} gerando resposta do modelo; promptLength={}", prompt == null ? 0 : prompt.length());
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null || Boolean.FALSE.equals(agentProperties.getLlmEnabled())) {
            log.warn("{[GENERATE_LLM]} modelo indisponivel, usando fallback deterministico");
            return fallbackResponse();
        }

        int attempts = Math.max(1, agentProperties.getTransientRetries());
        String systemPrompt = getToolCallingSystemPrompt();
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                log.trace("{[GENERATE_LLM]} tentativa {} de {}", attempt, attempts);
                return createChatClient(chatModel)
                        .prompt()
                        .system(systemPrompt)
                        .advisors(planningAdvisor, repairAdvisor, scopeAdvisor, verificationAdvisor)
                        .tools(projectDiscoveryTool, fileReadTool, fileWriteTool, compilationTool, feedbackTool, codeDiffTool)
                        .user(prompt)
                        .call()
                        .content();
            } catch (RuntimeException ex) {
                lastError = ex;
                log.error("{[GENERATE_LLM]} falha ao gerar resposta na tentativa {}", attempt, ex);
            }
        }

        throw new IllegalStateException("Nao foi possivel obter resposta do LLM", lastError);
    }

    protected ChatClient createChatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    @Override
    public void validateScope(ProjectDiscoverySnapshot discovery, GeneratedChangeSet changeSet) {
        log.info("{[VALID_SCOPE]} validando escopo de alteracoes");
        List<String> errors = scopeAdvisor.validate(discovery, changeSet == null ? null : changeSet.changes());
        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join("; ", errors));
        }
        log.info("{[VALID_SCOPE_RT]} escopo validado sem erros");
    }

    @Override
    public List<FileWriteResult> writeChanges(List<FileChangeCommand> changes) {
        log.info("{[WRITE_FILES]} aplicando alteracoes; total={}", changes == null ? 0 : changes.size());
        List<FileWriteResult> results = fileWriteTool.write(changes, System.getProperty("java.io.tmpdir") + "/springia-backups");
        log.info("{[WRITE_FILES_RT]} alteracoes aplicadas; total={}", results.size());
        return results;
    }

    @Override
    public List<CompilationResult> compile(CompileBy compileBy) {
        log.info("{[COMPILE_ALL]} compilando backend e frontend compileBy='{}'", compileBy == null ? null : compileBy.name());
        CompilationResult backend = compilationTool.compileBackend(compileBy);
        CompilationResult frontend = compilationTool.compileFrontend(compileBy);
        List<CompilationResult> results = List.of(backend, frontend);
        log.info("{[COMPILE_ALL_RT]} compilacao concluida; backend={} frontend={}", backend.success(), frontend.success());
        return results;
    }

    @Override
    public String buildFeedback(List<CompilationResult> compilationResults, String previousResponse, String repairHint) {
        log.info("{[BUILD_FB]} montando feedback de compilacao");
        String feedback = feedbackTool.buildFeedback(compilationResults, List.of(), previousResponse, repairHint);
        log.info("{[BUILD_FB_RT]} feedback montado; length={}", feedback == null ? 0 : feedback.length());
        return feedback;
    }

    @Override
    public boolean isSuccessful(List<CompilationResult> compilationResults) {
        log.info("{[VERIFY_OK]} verificando sucesso da iteracao");
        boolean successful = verificationAdvisor.isSuccessful(compilationResults);
        log.info("{[VERIFY_OK_RT]} resultado de verificacao={}", successful);
        return successful;
    }

    String getToolCallingSystemPrompt() {
        log.debug("{[LOAD_SYS_PRMPT]} resolvendo system prompt de tools");
        String cached = cachedToolCallingSystemPrompt;
        if (StringUtils.hasText(cached)) {
            return cached;
        }

        String location = agentProperties.getToolCallingSystemPrompt();
        if (!StringUtils.hasText(location)) {
            log.warn("{[LOAD_SYS_PRMPT]} caminho nao configurado; usando resource padrao={}", DEFAULT_TOOL_CALLING_SYSTEM_PROMPT_LOCATION);
            cachedToolCallingSystemPrompt = resolveFromResource(DEFAULT_TOOL_CALLING_SYSTEM_PROMPT_LOCATION);
            return cachedToolCallingSystemPrompt;
        }

        cachedToolCallingSystemPrompt = resolveFromResource(location);
        return cachedToolCallingSystemPrompt;
    }

    private String resolveFromResource(String location) {
        String loadedFromRequestedLocation = loadFromResource(location);
        if (StringUtils.hasText(loadedFromRequestedLocation)) {
            return loadedFromRequestedLocation;
        }

        if (DEFAULT_TOOL_CALLING_SYSTEM_PROMPT_LOCATION.equals(location)) {
            throw new IllegalStateException("Resource de system prompt nao carregada em " + location);
        }

        log.warn("{[LOAD_SYS_PRMPT]} fallback para resource padrao={}", DEFAULT_TOOL_CALLING_SYSTEM_PROMPT_LOCATION);
        String loadedFromDefaultLocation = loadFromResource(DEFAULT_TOOL_CALLING_SYSTEM_PROMPT_LOCATION);
        if (!StringUtils.hasText(loadedFromDefaultLocation)) {
            throw new IllegalStateException("Resource de system prompt nao carregada em " + DEFAULT_TOOL_CALLING_SYSTEM_PROMPT_LOCATION);
        }

        return loadedFromDefaultLocation;
    }

    private String loadFromResource(String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                log.warn("{[LOAD_SYS_PRMPT]} resource nao encontrada em {}", location);
                return null;
            }
            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                String loadedPrompt = FileCopyUtils.copyToString(reader);
                if (!StringUtils.hasText(loadedPrompt)) {
                    log.warn("{[LOAD_SYS_PRMPT]} resource vazia em {}", location);
                    return null;
                }
                return loadedPrompt;
            }
        } catch (IOException ex) {
            log.error("{[LOAD_SYS_PRMPT]} falha ao carregar resource {}", location, ex);
            return null;
        }
    }

    private String fallbackResponse() {
        log.debug("{[FALLBACK_RESP]} montando resposta fallback sem alteracoes");
        GeneratedChangeSet changeSet = new GeneratedChangeSet(
                "fallback sem alteracoes",
                "LLM indisponivel",
                List.of(CompileBy.COMMAND),
                List.of()
        );
        return JsonUtils.toJson(changeSet);
    }
}


