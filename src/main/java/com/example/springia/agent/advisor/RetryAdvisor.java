package com.example.springia.agent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reescreve o prompt com orientacao de correcao quando houver erro de build.
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.advisor.RetryAdvisor" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Slf4j
@Component
public class RetryAdvisor implements CallAdvisor {

    private static final String SUMMARY_MARKER = "BUILD_ERROR_SUMMARY";
    private static final String SUMMARY_END_MARKER = "END_BUILD_ERROR_SUMMARY";
    private static final String RETRY_HINT_MARKER = "RETRY_HINT_APPLIED";

    @Override
    public @NonNull ChatClientResponse adviseCall(
            @NonNull ChatClientRequest request,
            @NonNull CallAdvisorChain chain) {
        log.info("{[RETRY_CHAT]} iniciando advisor request={}", StringUtils.abbreviate(request.prompt().toString(), 200));

        String prompt = safeText(request.prompt().getUserMessage().getText());
        Prompt mutatedPrompt = request.prompt();

        if (prompt.contains("BUILD_ERRORXXX") && !prompt.contains(RETRY_HINT_MARKER)) {
            prompt = """
            %s

            %s

            Voce deve corrigir o codigo para compilar com a menor alteracao possivel.

            ERRO:
            %s

            REGRAS:
            - altere somente o necessario
            - nao reescreva tudo
            - corrija apenas o erro principal
            - mantenha resposta em JSON GeneratedChangeSet valido
            - se corrigido, notes deve conter VALIDADO
            """.formatted(prompt, RETRY_HINT_MARKER, extractError(prompt));

            mutatedPrompt = replaceLastUserMessage(request.prompt(), prompt);
        }

        ChatClientResponse response = chain.nextCall(request.mutate().prompt(mutatedPrompt).build());
        log.info("{[RETRY_CHAT_RT]} advisor concluido");
        return response;
    }

    private String extractError(String prompt) {
        log.debug("{[EXTRACT_ERR]} extraindo erro do prompt length={}", prompt == null ? 0 : prompt.length());
        if (prompt == null || prompt.isBlank()) {
            return "Erro de compilacao nao informado.";
        }

        int summaryIndex = prompt.indexOf(SUMMARY_MARKER);
        if (summaryIndex >= 0) {
            int contentStart = summaryIndex + SUMMARY_MARKER.length();
            int endIndex = prompt.indexOf(SUMMARY_END_MARKER, contentStart);
            String extracted = endIndex > contentStart
                    ? prompt.substring(contentStart, endIndex)
                    : prompt.substring(contentStart);
            return extracted.replace(":", "").trim();
        }

        int buildErrorIndex = prompt.indexOf("BUILD_ERROR");
        if (buildErrorIndex >= 0) {
            String extracted = prompt.substring(buildErrorIndex);
            return StringUtils.abbreviate(extracted, 800);
        }
        return StringUtils.abbreviate(prompt, 800);
    }

    private Prompt replaceLastUserMessage(Prompt originalPrompt, String newText) {
        log.debug("{[RPL_USER_MSG]} atualizando ultima mensagem de usuario no prompt");
        Prompt copied = originalPrompt.copy();
        List<Message> messages = copied.getInstructions();

        for (int index = messages.size() - 1; index >= 0; index--) {
            Message message = messages.get(index);
            if (message instanceof UserMessage userMessage) {
                messages.set(index, userMessage.mutate().text(newText).build());
                log.debug("{[RPL_USER_RT]} mensagem de usuario atualizada");
                return copied;
            }
        }

        messages.add(new UserMessage(newText));
        log.debug("{[RPL_USER_RT]} prompt sem user message; nova mensagem adicionada");
        return copied;
    }

    private String safeText(String value) {
        log.debug("{[SAFE_TEXT]} normalizando texto nulo");
        return value == null ? "" : value;
    }

    @Override
    public @NonNull String getName() {
        log.info("{[GET_NAME]} retornando nome do advisor");
        return "retry-advisor";
    }

    @Override
    public int getOrder() {
        int order = 15;
        log.info("{[GET_ORDER]} retornando ordem do advisor: {}", order);
        return order;
    }
}