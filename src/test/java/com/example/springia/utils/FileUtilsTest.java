package com.example.springia.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void listFilesNamesShouldThrowWhenPathIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FileUtils.listFilesNames(null)
        );

        assertEquals("Caminho nao pode ser nulo", ex.getMessage());
    }

    @Test
    void listFilesNamesShouldThrowWhenPathDoesNotExist() {
        Path missing = tempDir.resolve("nao-existe");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FileUtils.listFilesNames(missing)
        );

        assertTrue(ex.getMessage().startsWith("Caminho nao existe:"));
    }

    @Test
    void listFilesNamesShouldThrowWhenPathIsNotDirectory() throws IOException {
        Path file = tempDir.resolve("arquivo.txt");
        Files.writeString(file, "conteudo");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> FileUtils.listFilesNames(file)
        );

        assertTrue(ex.getMessage().startsWith("Caminho nao e um diretorio:"));
    }

    @Test
    void listFilesNamesShouldReturnSortedFilesIgnoringConfiguredDirectories() throws IOException {
        Path src = Files.createDirectories(tempDir.resolve("src"));
        Path nested = Files.createDirectories(src.resolve("nested"));
        Path gitDir = Files.createDirectories(tempDir.resolve(".git"));
        Path targetDir = Files.createDirectories(tempDir.resolve("target"));

        Path bFile = src.resolve("b.txt");
        Path aFile = nested.resolve("a.txt");
        Path gitFile = gitDir.resolve("ignored.txt");
        Path targetFile = targetDir.resolve("ignored.log");

        Files.writeString(bFile, "b");
        Files.writeString(aFile, "a");
        Files.writeString(gitFile, "git");
        Files.writeString(targetFile, "target");

        List<String> files = FileUtils.listFilesNames(tempDir);

        assertEquals(List.of(
                bFile.toAbsolutePath().normalize().toString(),
                aFile.toAbsolutePath().normalize().toString()
        ), files);
    }

    @Test
    void joinFileContentsShouldReturnEmptyWhenInputIsNullOrEmpty() {
        assertEquals("", FileUtils.joinFileContents((Path[]) null));
        assertEquals("", FileUtils.joinFileContents(new Path[0]));
    }

    @Test
    void joinFileContentsShouldCombineExistingFilesAndIgnoreMissingOnes() throws IOException {
        Path one = tempDir.resolve("one.txt");
        Path two = tempDir.resolve("two.txt");
        Path missing = tempDir.resolve("missing.txt");

        Files.writeString(one, "linha-1");
        Files.writeString(two, "linha-2");

        String result = FileUtils.joinFileContents(new Path[]{one, missing, two});

        assertTrue(result.contains("Arquivo: " + one));
        assertTrue(result.contains("linha-1"));
        assertTrue(result.contains("Arquivo: " + two));
        assertTrue(result.contains("linha-2"));
        assertTrue(result.contains("\n\nArquivo: "));
        assertFalse(result.contains("missing.txt\n"));
    }

    @Test
    void joinFileContentsShouldSupportStringPaths() throws IOException {
        Path one = tempDir.resolve("app.properties");
        Path two = tempDir.resolve("pom.xml");

        Files.writeString(one, "server.port=8080");
        Files.writeString(two, "<project></project>");

        String result = FileUtils.joinFileContents(new String[]{
                one.toString(),
                two.toString()
        });

        assertTrue(result.contains("Arquivo: " + one));
        assertTrue(result.contains("server.port=8080"));
        assertTrue(result.contains("Arquivo: " + two));
        assertTrue(result.contains("<project></project>"));
    }
}



