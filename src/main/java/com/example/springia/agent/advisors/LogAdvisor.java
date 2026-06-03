package com.example.springia.agent.advisors;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class LogAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(LogAdvisor.class);
    private static final int MAX_LOG_LENGTH = 400;
    private static final int ORDER = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 100;

    // ─── CallAdvisor ────────────────────────────────────────────────────────────

    @Override
    public @NonNull ChatClientResponse adviseCall(@NonNull ChatClientRequest request,
                                                  @NonNull CallAdvisorChain chain) {
        logRequest(request);
        ChatClientResponse response = chain.nextCall(request);
        logResponse(response);
        return response;
    }

    // ─── StreamAdvisor ──────────────────────────────────────────────────────────

    @Override
    public @NonNull Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest request,
                                                          @NonNull StreamAdvisorChain chain) {
        logRequest(request);
        return new ChatClientMessageAggregator()
                .aggregateChatClientResponse(chain.nextStream(request), this::logResponse);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void logRequest(ChatClientRequest request) {
        String userText = request.prompt().getUserMessage().getText();
        log.info("[AI][REQ] advisor={} contextKeys={} user={}",
                getName(), request.context().keySet(), truncate(userText));
    }

    private void logResponse(ChatClientResponse response) {
        ChatResponse chatResponse = response != null ? response.chatResponse() : null;
        String text = (chatResponse != null && chatResponse.getResult() != null)
                ? chatResponse.getResult().getOutput().getText()
                : null;
        log.info("[AI][RES] advisor={} answer={}", getName(), truncate(text));
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) return "<empty>";
        String s = value.replaceAll("\\s+", " ").trim();
        return s.length() <= MAX_LOG_LENGTH ? s : s.substring(0, MAX_LOG_LENGTH) + "…";
    }

    // ─── Advisor identity ────────────────────────────────────────────────────────

    @Override
    public @NonNull String getName() {
        return LogAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
