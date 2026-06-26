package com.example.springia.agent.tool.files;

import com.example.springia.config.AgentProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileReadToolTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadExistingFileUsingStringPath() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend"));
        Path file = backend.resolve("src/main/java/App.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class App {}\n");

        AgentProperties props = new AgentProperties();
        props.setBackendRoot(backend.toString());
        props.setFrontendRoot(tempDir.resolve("frontend").toString());

        FileReadTool tool = new FileReadTool(props);

        String content = tool.readFile(file.toString());

        assertEquals("class App {}\n", content);
    }

    @Test
    void shouldRejectPathOutsideAllowedRoots() {
        Path backend = tempDir.resolve("backend");
        Path outside = tempDir.resolve("outside/App.java");

        AgentProperties props = new AgentProperties();
        props.setBackendRoot(backend.toString());
        props.setFrontendRoot(tempDir.resolve("frontend").toString());

        FileReadTool tool = new FileReadTool(props);

        assertThrows(IllegalArgumentException.class, () -> tool.readFile(outside.toString()));
    }
}

