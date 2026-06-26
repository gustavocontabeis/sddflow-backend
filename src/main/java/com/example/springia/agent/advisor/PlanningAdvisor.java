package com.example.springia.agent.advisor;

import com.example.springia.agent.model.ProjectDiscoverySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

/**
 * Transforma a solicitacao em um plano minimo e ordenado.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.advisor.PlanningAdvisor" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 */
@Slf4j
@Component
public class PlanningAdvisor implements CallAdvisor {

    @Override
    public String getName() {
        return "planning-advisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.info("{[PLAN_CHAT]} iniciando advisor do chat request={}", request.prompt());
        ChatClientResponse response = chain.nextCall(request);
        log.info("{[PLAN_CHAT_RT]} advisor do chat concluido");
        return response;
    }

    public String buildPlan(String taskDescription, ProjectDiscoverySnapshot discovery) {
        log.info("{[PLAN_ADVI]} montando plano de execucao");
        String plan = "TAREFA: " + taskDescription + '\n'
                + "DISCOVERY:\n" + discovery.summary() + '\n'
                + "REGRAS:\n- alterar somente arquivos impactados\n- responder em JSON valido\n- informar filePath, operation, content e summary\n";
        log.info("{[PLAN_ADVI_RT]} plano montado; {}", plan);
        return plan;
    }
}

