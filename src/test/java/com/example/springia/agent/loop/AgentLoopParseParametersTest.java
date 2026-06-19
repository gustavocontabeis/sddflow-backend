package com.example.springia.agent.loop;

import com.example.springia.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AgentLoopParseParametersTest {

    @Test
    void parseParametersShouldSupportJsonFenceAndMultilineContent() throws Exception {
        AgentLoop agentLoop = new AgentLoop(mock(ChatClient.class), mock(ToolRegistry.class), 1);

        String params = "```json\n"
                + "{\n"
                + "  \"file_path\": \"src/app/components/criar-tarefa/criar-tarefa.component.html\",\n"
                + "  \"content\": \"<textarea pTextarea id=\\\"descricao\\\" formControlName=\\\"descricao\\\" rows=\\\"4\\\" [autoResize]=\\\"true\\\"></textarea>\\n<label for=\\\"descricao\\\">Faça uma descrição da tarefa aqui</label>\"\n"
                + "}\n"
                + "```";

        Method method = AgentLoop.class.getDeclaredMethod("parseParameters", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(agentLoop, params);

        assertEquals("src/app/components/criar-tarefa/criar-tarefa.component.html", result.get("file_path"));
        assertTrue(result.get("content").contains("Faça uma descrição da tarefa aqui"));
    }

    @Test
    void parseParametersShouldSupportConcatenatedJsonStringLiterals() throws Exception {
        AgentLoop agentLoop = new AgentLoop(mock(ChatClient.class), mock(ToolRegistry.class), 1);

        String params = "{\n"
                + "  \"file_path\": \"/tmp/tarefas-frontend/src/app/components/lista-tarefas/lista-tarefas.component.ts\",\n"
                + "  \"content\": \"if (prioridade === 1) return 'warn';\\n\" +\n"
                + "             \"if (prioridade <= 2) return 'success';\\n\" +\n"
                + "             \"if (prioridade === 3) return 'info';\\n\" +\n"
                + "             \"if (prioridade === 4) return 'warn';\"\n"
                + "}";

        Method method = AgentLoop.class.getDeclaredMethod("parseParameters", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(agentLoop, params);

        assertEquals("/tmp/tarefas-frontend/src/app/components/lista-tarefas/lista-tarefas.component.ts", result.get("file_path"));
        assertTrue(result.get("content").contains("if (prioridade === 1) return 'warn';"));
        assertTrue(result.get("content").contains("if (prioridade === 4) return 'warn';"));
    }
}


