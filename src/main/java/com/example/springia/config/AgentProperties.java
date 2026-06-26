package com.example.springia.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String backendRoot = "/tmp/tarefas-backend";
    private String frontendRoot = "/tmp/tarefas-frontend";
    private Integer maxIterations = 100;
    private Duration iterationTimeout = Duration.ofMinutes(10);
    private Duration compilationTimeout = Duration.ofMinutes(10);
    private Integer transientRetries = 10;
    private Boolean llmEnabled = Boolean.TRUE;
    private String toolCallingSystemPrompt = "classpath:prompts/tool-calling-system-prompt.md";
}

