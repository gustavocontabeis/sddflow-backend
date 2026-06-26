package com.example.springia.agent.tool.discovery;

import com.example.springia.config.AgentProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectDiscoveryToolTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDiscoverFilesAndSummarizeProject() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend"));
        Path srcMain = Files.createDirectories(backend.resolve("src/main/java"));
        Files.writeString(srcMain.resolve("App.java"), "class App {}");
        Files.writeString(Files.createDirectories(backend.resolve("src/test/java")).resolve("AppTest.java"), "class AppTest {}");

        AgentProperties props = new AgentProperties();
        props.setBackendRoot(backend.toString());
        props.setFrontendRoot(tempDir.resolve("frontend").toString());

        ProjectDiscoveryTool tool = new ProjectDiscoveryTool(props);
        var report = tool.discover(backend, "backend");

        assertTrue(report.exists());
        assertEquals(1, report.javaFiles().size());
        assertEquals(0, report.testFiles().size());
        assertTrue(report.summary().contains("backend em"));
    }
}


