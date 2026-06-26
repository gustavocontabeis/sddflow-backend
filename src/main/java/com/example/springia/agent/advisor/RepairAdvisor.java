package com.example.springia.agent.advisor;

import com.example.springia.agent.model.ProjectDiscoverySnapshot;
import com.example.springia.agent.tool.feedback.FeedbackTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

/**
 * Converte falhas de compilacao em instrucao de reparo.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.advisor.RepairAdvisor" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairAdvisor implements CallAdvisor {

    private final FeedbackTool feedbackTool;

    @Override
    public String getName() {
        return "repair-advisor";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("{[REPAIR_CHAT]} iniciando advisor do chat request={}", request.prompt());
        ChatClientResponse response = chain.nextCall(request);
        log.info("{[REPAIR_CHAT_RT]} advisor do chat concluido");
        return response;
    }

    public String buildRepairPrompt(String taskDescription, ProjectDiscoverySnapshot discovery, String previousResponse, String feedback) {
        log.info("{[REPAIR_AD]} construindo prompt de reparo");
        String prompt = "TAREFA: " + taskDescription + '\n'
                + "DISCOVERY:\n" + discovery.summary() + '\n'
                + "FEEDBACK:\n" + feedbackTool.buildFeedback(null, null, previousResponse, feedback) + '\n'
                + "RESPONDA NOVAMENTE EM JSON VALIDO COM AS ALTERACOES NECESSARIAS.";
        log.info("{[REPAIR_AD_RT]} prompt de reparo montado; {}", prompt);
        return prompt;
    }
}

