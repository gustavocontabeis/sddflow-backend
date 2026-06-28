package com.example.springia.agent.advisor;

import lombok.RequiredArgsConstructor;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resume logs de compilacao para reduzir ruido antes do reparo.
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.advisor.ErrorSummarizerAdvisor" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorSummarizerAdvisor implements CallAdvisor {

    private static final String BUILD_LOG_MARKER = "BUILD_LOG";
    private static final String BUILD_SUMMARY_MARKER = "BUILD_ERROR_SUMMARY";
    private static final String BUILD_SUMMARY_END_MARKER = "END_BUILD_ERROR_SUMMARY";
    private static final int MAX_LOG_CHARS_TO_SUMMARIZE = 12_000;
    private static final int MAX_SUMMARY_LINES = 12;

    @Override
    public @NonNull ChatClientResponse adviseCall(
            @NonNull ChatClientRequest request,
            @NonNull CallAdvisorChain chain) {
        log.info("{[ERR_SUM_CHAT]} iniciando advisor request={}", StringUtils.abbreviate(request.prompt().toString(), 150));
        Prompt mutatedPrompt = request.prompt();
        String userText = safeText(request.prompt().getUserMessage().getText());

        if (userText.contains(BUILD_LOG_MARKER+"XXXX")) {
            try {
                String buildLog = extractBuildLog(userText);
                String summarizedError = buildCompactSummaryInput(buildLog);
                String compactedUserText = replaceBuildLogBySummary(userText, summarizedError);
                mutatedPrompt = replaceLastUserMessage(request.prompt(), compactedUserText);
                log.info("{[ERR_SUM_CHAT_RT]} resumo de erro aplicado charsOriginais={} charsResumo={}",
                        buildLog.length(), summarizedError.length());
            } catch (Exception e) {
                log.error("{[ERR_SUM_FAIL]} falha ao resumir log, mantendo prompt original", e);
            }
        }

        ChatClientResponse response = chain.nextCall(request.mutate().prompt(mutatedPrompt).build());
        log.info("{[ERR_SUM_CHAT_RT]} advisor concluido");
        return response;
    }

    private String extractBuildLog(String userText) {
        log.debug("{[EXT_BUILD_LOG]} extraindo trecho de build do prompt");
        int markerIndex = userText.indexOf(BUILD_LOG_MARKER);
        if (markerIndex < 0) {
            return "";
        }

        String extracted = userText.substring(markerIndex + BUILD_LOG_MARKER.length()).trim();
        if (extracted.length() > MAX_LOG_CHARS_TO_SUMMARIZE) {
            extracted = extracted.substring(0, MAX_LOG_CHARS_TO_SUMMARIZE);
        }
        log.debug("{[EXT_BUILD_RT]} trecho extraido com {} caracteres", extracted.length());
        return extracted;
    }

    private String buildCompactSummaryInput(String buildLog) {
        log.debug("{[SUM_INPUT]} compactando log para entrada de resumo no mesmo ChatClient");
        if (StringUtils.isBlank(buildLog)) {
            return "Nao foi possivel identificar erro de compilacao no log recebido.";
        }

        String[] lines = buildLog.split("\\R");
        List<String> relevantLines = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String normalized = line.toLowerCase(Locale.ROOT);
            if (normalized.contains("error")
                    || normalized.contains("exception")
                    || normalized.contains("failed")
                    || normalized.contains("cannot")
                    || normalized.contains("symbol")
                    || normalized.contains("[help 1]")) {
                relevantLines.add(line.trim());
            }
            if (relevantLines.size() >= MAX_SUMMARY_LINES) {
                break;
            }
        }

        String summarized = String.join("\n", relevantLines);
        if (StringUtils.isBlank(summarized)) {
            summarized = fallbackSummary(buildLog);
        }
        log.debug("{[SUM_INPUT_RT]} entrada compacta gerada com {} caracteres", summarized.length());
        return summarized;
    }

    private String replaceBuildLogBySummary(String userText, String summarizedError) {
        log.debug("{[RPL_BUILD]} substituindo BUILD_LOG por resumo compacto");
        int markerIndex = userText.indexOf(BUILD_LOG_MARKER);
        if (markerIndex < 0) {
            return userText;
        }

        String prefix = userText.substring(0, markerIndex).trim();
        String compactBlock = """
                %s
                %s:
                %s
                %s

                ORIENTACAO:
                - use somente o bloco BUILD_ERROR_SUMMARY para entender o erro
                - traduza esse bloco para causa raiz + arquivo/linha + acao de correcao
                - ignore stacktrace e linhas repetidas
                """.formatted(prefix, BUILD_SUMMARY_MARKER, summarizedError, BUILD_SUMMARY_END_MARKER).trim();

        log.debug("{[RPL_BUILD_RT]} prompt compactado com {} caracteres", compactBlock.length());
        return compactBlock;
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

    private String fallbackSummary(String buildLog) {
        log.debug("{[SUM_FALLBACK]} gerando fallback de resumo sem LLM");
        if (StringUtils.isBlank(buildLog)) {
            return "Nao foi possivel resumir o erro de compilacao; log vazio.";
        }
        String[] lines = buildLog.split("\\R");
        StringBuilder builder = new StringBuilder();
        int maxLines = Math.min(10, lines.length);
        for (int i = 0; i < maxLines; i++) {
            if (!lines[i].isBlank()) {
                builder.append(lines[i].trim()).append('\n');
            }
        }
        return StringUtils.defaultIfBlank(builder.toString().trim(), "Erro de compilacao sem detalhe adicional.");
    }

    private String safeText(String value) {
        log.debug("{[SAFE_TEXT]} normalizando texto nulo");
        return value == null ? "" : value;
    }

    @Override
    public @NonNull String getName() {
        log.info("{[GET_NAME]} retornando nome do advisor");
        return "error-summarizer-advisor";
    }

    @Override
    public int getOrder() {
        int order = 5;
        log.info("{[GET_ORDER]} retornando ordem do advisor: {}", order);
        return order;
    }
}