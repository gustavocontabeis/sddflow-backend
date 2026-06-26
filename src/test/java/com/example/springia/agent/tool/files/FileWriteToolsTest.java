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

        CreateFileTool tool = new CreateFileTool();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tool.execute(Map.of("file_path", file.toString(), "content", "novo conteudo")));

        assertTrue(ex.getMessage().contains("Arquivo já existe"));
        assertEquals("conteudo original", Files.readString(file));
    }

    @Test
    void updateFileShouldReplaceOnlyFirstOccurrenceByDefault() throws Exception {
        Path file = tempDir.resolve("demo.txt");
        Files.writeString(file, "prioridade=2\nprioridade=2\n");

        UpdateFileTool tool = new UpdateFileTool();
        String result = tool.execute(Map.of(
                "file_path", file.toString(),
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

        UpdateFileTool tool = new UpdateFileTool();
        String result = tool.execute(Map.of(
                "file_path", file.toString(),
                "old_text", "prioridade=2",
                "new_text", "prioridade=1",
                "replace_all", "true"
        ));

        assertTrue(result.contains("2 ocorrência(s)"));
        assertEquals("prioridade=1\nprioridade=1\n", Files.readString(file));
    }

    @Test
    void updateFileShouldMatchOldTextWithDifferentWhitespace() throws Exception {
        Path file = tempDir.resolve("demo.txt");
        Files.writeString(file, "if (this.tarefa.prioridade === 0) {\n    alert('Informe uma prioridade');\n    return;\n}\n");

        UpdateFileTool tool = new UpdateFileTool();
        String result = tool.execute(Map.of(
                "file_path", file.toString(),
                "old_text", "if (this.tarefa.prioridade === 0) { alert('Informe uma prioridade'); return; }",
                "new_text", "if (this.tarefa.prioridade === 0) {\n    this.erro = 'Informe uma prioridade';\n    return;\n}"
        ));

        assertTrue(result.contains("1 ocorrência(s)"));
        assertTrue(Files.readString(file).contains("this.erro = 'Informe uma prioridade'"));
    }

    @Test
    void updateFileShouldReturnHelpfulMessageWhenOldTextDoesNotExist() throws Exception {
        Path file = tempDir.resolve("demo.txt");
        Files.writeString(file, "conteudo existente\n");

        UpdateFileTool tool = new UpdateFileTool();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tool.execute(Map.of(
                        "file_path", file.toString(),
                        "old_text", "trecho inexistente",
                        "new_text", "novo"
                )));

        assertTrue(ex.getMessage().contains("Use read_file"));
        assertTrue(ex.getMessage().contains("Prévia do início do arquivo"));
    }
}

