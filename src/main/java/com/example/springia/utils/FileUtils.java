package com.example.springia.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class FileUtils {

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(".idea", "test", "target", "node_modules", "dist", ".git");

    public static List<String> listFilesNames(Path path){
        if (path == null) {
            throw new IllegalArgumentException("Caminho nao pode ser nulo");
        }

        Path root = path.toAbsolutePath().normalize();

        if (!Files.exists(root)) {
            throw new IllegalArgumentException("Caminho nao existe: " + root);
        }

        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Caminho nao e um diretorio: " + root);
        }

        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(p->notInIgnoredDirectory(p))
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .map(candidate -> candidate.toAbsolutePath().normalize().toString())
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao listar arquivos em: " + root, e);
        }
    }

    private static boolean notInIgnoredDirectory(Path candidate) {
        for (Path segment : candidate) {
            if (IGNORED_DIRECTORIES.contains(segment.toString())) {
                return false;
            }
        }
        return true;
    }

    public static String joinFileContents(String[] filePaths) {
        if (filePaths == null || filePaths.length == 0) {
            return "";
        }

        return joinFileContents(java.util.Arrays.stream(filePaths)
                .map(Path::of)
                .toArray(Path[]::new));
    }

    public static String joinFileContents(Path[] filePaths) {

        if (filePaths == null || filePaths.length == 0) {
            return "";
        }

        StringBuilder conteudo = new StringBuilder();

        for (Path filePath : filePaths) {
            if (filePath == null) {
                log.warn("Caminho nulo ignorado");
                continue;
            }

            if (!Files.exists(filePath)) {
                log.warn("Caminho ignorado (nao existe): {}", filePath);
                continue;
            }

            if (Files.isRegularFile(filePath)) {
                appendFileContent(conteudo, filePath);
                continue;
            }

            if (Files.isDirectory(filePath)) {
                try (Stream<Path> stream = Files.walk(filePath)) {
                    stream
                            .filter(FileUtils::notInIgnoredDirectory)
                            .filter(Files::isRegularFile)
                            .sorted(Comparator.comparing(Path::toString))
                            .forEach(path -> appendFileContent(conteudo, path));
                } catch (IOException e) {
                    log.warn("Falha ao percorrer diretorio: {}", filePath, e);
                }
                continue;
            }

            log.warn("Caminho ignorado (nao e arquivo nem diretorio): {}", filePath);
        }

        return conteudo.toString();
    }

    private static void appendFileContent(StringBuilder conteudo, Path filePath) {
        try {
            if (!conteudo.isEmpty()) {
                conteudo.append("\n\n");
            }

            conteudo.append("Arquivo: ")
                    .append(filePath)
                    .append("\n")
                    .append(Files.readString(filePath));
        } catch (IOException e) {
            log.warn("Falha ao ler arquivo de configuracao: {}", filePath, e);
        }
    }



}
