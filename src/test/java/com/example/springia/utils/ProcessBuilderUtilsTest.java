package com.example.springia.utils;

import com.example.springia.dto.ProcessBuilderReturnDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessBuilderUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void executeShouldRunMavenCleanPackageInWorkingDirectory() throws IOException {
        Files.writeString(tempDir.resolve("pom.xml"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>process-builder-test</artifactId>
                    <version>1.0.0</version>
                </project>
                """);

        ProcessBuilderReturnDTO result = ProcessBuilderUtils.execute(
                tempDir.toString(),
                "mvn",
                "-DskipTests",
                "clean",
                "package"
        );

        assertEquals(0, result.getExitCode(), result.getOutput());
        assertTrue(result.isOk());
        assertTrue(Files.exists(tempDir.resolve("target")));
    }

    @Test
    void executeShouldFailWhenDirectoryDoesNotExist() {
        Path missingDir = tempDir.resolve("nao-existe");

        ProcessBuilderReturnDTO result = ProcessBuilderUtils.execute(
                missingDir.toString(),
                "mvn",
                "clean",
                "package"
        );

        assertEquals(-1, result.getExitCode());
        assertTrue(result.getOutput().contains("diretório inválido"));
    }
}

