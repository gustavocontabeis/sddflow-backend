package com.example.springia.agent.advisor;

import com.example.springia.agent.model.CompilationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Verifica se a iteracao terminou com evidencias validas.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.advisor.VerificationAdvisor" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 */
@Slf4j
@Component
public class VerificationAdvisor implements CallAdvisor {

    @Override
    public String getName() {
        return "verification-advisor";
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("{[VERIFY_CHAT]} iniciando advisor do chat request={}", request.prompt());
        ChatClientResponse response = chain.nextCall(request);
        log.info("{[VERIFY_CHAT_RT]} advisor do chat concluido");
        return response;
    }

    public boolean isSuccessful(List<CompilationResult> compilationResults) {
        log.info("{[VERIFY_AD]} verificando resultados de compilacao");
        boolean successful = compilationResults != null
                && !compilationResults.isEmpty()
                && compilationResults.stream().allMatch(CompilationResult::success);
        log.info("{[VERIFY_AD_RT]} verificacao concluida; successful={}", successful);
        return successful;
    }
}

