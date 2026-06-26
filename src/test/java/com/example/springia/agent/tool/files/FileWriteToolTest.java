package com.example.springia.agent.tool.files;

import com.example.springia.agent.model.ChangeOperation;
import com.example.springia.agent.model.FileChangeCommand;
import com.example.springia.agent.model.FilePatchCommand;
import com.example.springia.agent.tool.diff.CodeDiffTool;
import com.example.springia.config.AgentProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileWriteToolTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldApplyTargetedPatchAndCreateBackup() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend"));
        Path file = backend.resolve("src/main/java/App.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class App {\n    int oldValue;\n}\n");

        AgentProperties props = new AgentProperties();
        props.setBackendRoot(backend.toString());
        props.setFrontendRoot(tempDir.resolve("frontend").toString());

        FileWriteTool tool = new FileWriteTool(props, new CodeDiffTool());
        var command = new FileChangeCommand(
                file.toString(),
                ChangeOperation.UPDATE,
                null,
                "update",
                sha256(Files.readString(file)),
                false,
                List.of(new FilePatchCommand("int oldValue;", "int newValue;", null))
        );
        var results = tool.write(List.of(command), tempDir.resolve("backups").toString());

        assertEquals(1, results.size());
        assertTrue(results.get(0).written());
        assertTrue(Files.readString(file).contains("int newValue"));
    }

    @Test
    void shouldFailUpdateWhenFullReplaceNotExplicitlyAllowed() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend-full-replace"));
        Path file = backend.resolve("src/main/java/App.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class App {}\n");

        AgentProperties props = new AgentProperties();
        props.setBackendRoot(backend.toString());
        props.setFrontendRoot(tempDir.resolve("frontend-full-replace").toString());

        FileWriteTool tool = new FileWriteTool(props, new CodeDiffTool());
        var command = new FileChangeCommand(file.toString(), ChangeOperation.UPDATE, "class App { int x; }\n", "update");

        assertThrows(IllegalArgumentException.class, () -> tool.write(List.of(command), tempDir.resolve("backups").toString()));
    }

    @Test
    void shouldFailWhenPatchIsAmbiguousWithoutOccurrenceIndex() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend-ambiguous"));
        Path file = backend.resolve("src/main/java/App.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class App {\n    int value;\n    int value;\n}\n");

        AgentProperties props = new AgentProperties();
        props.setBackendRoot(backend.toString());
        props.setFrontendRoot(tempDir.resolve("frontend-ambiguous").toString());

        FileWriteTool tool = new FileWriteTool(props, new CodeDiffTool());
        var command = new FileChangeCommand(
                file.toString(),
                ChangeOperation.UPDATE,
                null,
                "update",
                null,
                false,
                List.of(new FilePatchCommand("int value;", "int changed;", null))
        );

        assertThrows(IllegalStateException.class, () -> tool.write(List.of(command), tempDir.resolve("backups").toString()));
    }

    @Test
    void shouldFailWhenExpectedHashDiffers() throws Exception {
        Path backend = Files.createDirectories(tempDir.resolve("backend-hash"));
        Path file = backend.resolve("src/main/java/App.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class App {}\n");

        AgentProperties props = new AgentProperties();
        props.setBackendRoot(backend.toString());
        props.setFrontendRoot(tempDir.resolve("frontend-hash").toString());

        FileWriteTool tool = new FileWriteTool(props, new CodeDiffTool());
        var command = new FileChangeCommand(
                file.toString(),
                ChangeOperation.UPDATE,
                null,
                "update",
                "invalid-hash",
                false,
                List.of(new FilePatchCommand("class App {}", "class App { int ok; }", null))
        );

        assertThrows(IllegalStateException.class, () -> tool.write(List.of(command), tempDir.resolve("backups").toString()));
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}

