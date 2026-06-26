package com.example.springia.agent.advisor;

import com.example.springia.agent.model.FileChangeCommand;
import com.example.springia.agent.model.FilePatchCommand;
import com.example.springia.agent.model.ProjectDiscoverySnapshot;
import com.example.springia.config.AgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Limita as alteracoes aos diretorios permitidos.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.advisor.ScopeAdvisor" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScopeAdvisor implements CallAdvisor {

    private final AgentProperties agentProperties;

    @Override
    public String getName() {
        return "scope-advisor";
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("{[SCOPE_CHAT]} iniciando advisor do chat request={}", request.prompt());
        ChatClientResponse response = chain.nextCall(request);
        log.info("{[SCOPE_CHAT_RT]} advisor do chat concluido");
        return response;
    }

    public List<String> validate(ProjectDiscoverySnapshot discovery, List<FileChangeCommand> changes) {
        log.info("{[SCOPE_ADV]} validando escopo das alteracoes summaryLength={}",
                discovery == null || discovery.summary() == null ? 0 : discovery.summary().length());
        for (FileChangeCommand change : changes) {
            log.info("{[SCOPE_ADV]} change.filePath: {}", change.filePath());
            log.info("{[SCOPE_ADV]} change.content: {}", change.content());
            log.info("{[SCOPE_ADV]} change.operation: {}", change.operation());
            log.info("{[SCOPE_ADV]} change.summary: {}", change.summary());
            log.info("{[SCOPE_ADV]} change.allowFullReplace: {}", change.allowFullReplace());
            List<FilePatchCommand> patches = change.patches();
            for (FilePatchCommand patch : patches) {
                log.info("{[SCOPE_ADV]} FilePatchCommand.oldText: {}", patch.oldText());
                log.info("{[SCOPE_ADV]} FilePatchCommand.newText: {}", patch.newText());
                log.info("{[SCOPE_ADV]} FilePatchCommand.occurrenceIndex: {}", patch.occurrenceIndex());
            }
        }
        List<String> errors = new ArrayList<>();
        if (changes == null || changes.isEmpty()) {
            log.info("{[SCOPE_ADV_RT]} escopo validado sem alteracoes");
            return errors;
        }

        Path backendRoot = Path.of(agentProperties.getBackendRoot()).toAbsolutePath().normalize();
        Path frontendRoot = Path.of(agentProperties.getFrontendRoot()).toAbsolutePath().normalize();

        for (FileChangeCommand change : changes) {
            if (change == null || change.filePath() == null) {
                errors.add("Alteracao sem caminho de arquivo");
                continue;
            }

            Path path = Path.of(change.filePath()).toAbsolutePath().normalize();
            boolean allowed = path.startsWith(backendRoot) || path.startsWith(frontendRoot);
            if (!allowed) {
                errors.add("Arquivo fora do escopo permitido: " + path);
            }
        }

        log.info("{[SCOPE_ADV_RT]} validacao concluida; totalErros={}", errors.size());
        return errors;
    }
}

