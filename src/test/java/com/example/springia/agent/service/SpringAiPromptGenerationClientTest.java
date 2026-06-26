package com.example.springia.agent.service;

import com.example.springia.agent.advisor.PlanningAdvisor;
import com.example.springia.agent.advisor.RepairAdvisor;
import com.example.springia.agent.advisor.ScopeAdvisor;
import com.example.springia.agent.advisor.VerificationAdvisor;
import com.example.springia.agent.tool.compiler.CompilationTool;
import com.example.springia.agent.tool.diff.CodeDiffTool;
import com.example.springia.agent.tool.discovery.ProjectDiscoveryTool;
import com.example.springia.agent.tool.feedback.FeedbackTool;
import com.example.springia.agent.tool.files.FileReadTool;
import com.example.springia.agent.tool.files.FileWriteTool;
import com.example.springia.config.AgentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SpringAiPromptGenerationClientTest {

    @Test
    void shouldLoadSystemPromptFromClasspathResource() {
        AgentProperties properties = new AgentProperties();
        properties.setToolCallingSystemPrompt("classpath:prompts/tool-calling-system-prompt.md");
        ObjectProvider<ChatModel> chatModelProvider = mock();

        SpringAiPromptGenerationClient client = new SpringAiPromptGenerationClient(
                chatModelProvider,
                new DefaultResourceLoader(),
                properties,
                mock(ProjectDiscoveryTool.class),
                mock(FileReadTool.class),
                mock(FileWriteTool.class),
                mock(CompilationTool.class),
                mock(FeedbackTool.class),
                mock(CodeDiffTool.class),
                mock(PlanningAdvisor.class),
                mock(RepairAdvisor.class),
                mock(ScopeAdvisor.class),
                mock(VerificationAdvisor.class)
        );

        String prompt = client.getToolCallingSystemPrompt();

        assertTrue(prompt.contains("project_discovery"));
        assertTrue(prompt.contains("GeneratedChangeSet"));
    }

    @Test
    void shouldFallbackToDefaultPromptWhenResourceDoesNotExist() {
        AgentProperties properties = new AgentProperties();
        properties.setToolCallingSystemPrompt("classpath:prompts/nao-existe.md");
        ObjectProvider<ChatModel> chatModelProvider = mock();

        SpringAiPromptGenerationClient client = new SpringAiPromptGenerationClient(
                chatModelProvider,
                new DefaultResourceLoader(),
                properties,
                mock(ProjectDiscoveryTool.class),
                mock(FileReadTool.class),
                mock(FileWriteTool.class),
                mock(CompilationTool.class),
                mock(FeedbackTool.class),
                mock(CodeDiffTool.class),
                mock(PlanningAdvisor.class),
                mock(RepairAdvisor.class),
                mock(ScopeAdvisor.class),
                mock(VerificationAdvisor.class)
        );

        String prompt = client.getToolCallingSystemPrompt();

        assertTrue(prompt.contains("project_discovery"));
        assertTrue(prompt.contains("GeneratedChangeSet"));
    }
}



