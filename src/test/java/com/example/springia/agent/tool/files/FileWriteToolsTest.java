package com.example.springia.agent.tool.files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileWriteToolsTest {

    @TempDir
    Path tempDir;

    @Test
    void createFileShouldFailWhenTargetAlreadyExists() throws Exception {
        Path file = tempDir.resolve("demo.txt");
        Files.writeString(file, "conteudo original");

        CreateFileTool tool = new CreateFileTool(tempDir.toString());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tool.execute(Map.of("file_path", "demo.txt", "content", "novo conteudo")));

        assertTrue(ex.getMessage().contains("Arquivo já existe"));
        assertEquals("conteudo original", Files.readString(file));
    }

    @Test
    void updateFileShouldReplaceOnlyFirstOccurrenceByDefault() throws Exception {
        Path file = tempDir.resolve("demo.txt");
        Files.writeString(file, "prioridade=2\nprioridade=2\n");

        UpdateFileTool tool = new UpdateFileTool(tempDir.toString());
        String result = tool.execute(Map.of(
                "file_path", "demo.txt",
                "old_text", "prioridade=2",
                "new_text", "prioridade=1"
        ));

        assertTrue(result.contains("1 ocorrência(s)"));
        assertEquals("prioridade=1\nprioridade=2\n", Files.readString(file));
    }

    @Test
    void updateFileShouldReplaceAllOccurrencesWhenRequested() throws Exception {
        Path file = tempDir.resolve("demo.txt");
        Files.writeString(file, "prioridade=2\nprioridade=2\n");

        UpdateFileTool tool = new UpdateFileTool(tempDir.toString());
        String result = tool.execute(Map.of(
                "file_path", "demo.txt",
                "old_text", "prioridade=2",
                "new_text", "prioridade=1",
                "replace_all", "true"
        ));

        assertTrue(result.contains("2 ocorrência(s)"));
        assertEquals("prioridade=1\nprioridade=1\n", Files.readString(file));
    }
}

