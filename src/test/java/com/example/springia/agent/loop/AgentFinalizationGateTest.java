package com.example.springia.agent.loop;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AgentFinalizationGateTest {

    private final AgentFinalizationGate gate = new AgentFinalizationGate();

    @Test
    void buildDockerTestCommandShouldUseOnlyDockerArguments() {
        String[] command = gate.buildDockerTestCommand("gate-image");

        assertArrayEquals(new String[]{"docker", "build", "--no-cache", "-t", "gate-image", "."}, command);
    }

    @Test
    void validateShouldRequireDockerfile() throws Exception {
        Path repo = Files.createTempDirectory("gate-maven-wrapper-");

        var project = com.example.springia.model.Project.builder()
                .id(1L)
                .repos(java.util.List.of(com.example.springia.model.CodeRepo.builder()
                        .id(2L)
                        .name("demo")
                        .path(repo.toString())
                        .build()))
                .build();

        var result = gate.validate(project);

        assertFalse(result.passed());
    }
}

