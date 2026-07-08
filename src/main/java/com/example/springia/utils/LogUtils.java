package com.example.springia.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogUtils {

    private static final DateTimeFormatter FILE_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    public static String saveLog(String context) {
        return saveLog(context, "", "txt");
    }

    /**
     * Exemplo:
     * log.info("[CHAT] prompt CREATE_USER_STORY [file:{}]: {} ", LogUtils.saveLog(prompt, "prompt-criar-historia-usuario", "md"), prompt.length());
     * @param content - conteúdo do log
     * @param name - nome do arquivo
     * @param extension - Extensão do arquivo. ex: "md", ou "json", ou "txt"
     * @return
     */
    public static String saveLog(String content, String name, String extension) {
        try {
            Path logDir = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
            Files.createDirectories(logDir);

            String fileName = "log-" + LocalDateTime.now().format(FILE_TS_FORMAT) + "-" + name + "." + extension;
            Path logFile = logDir.resolve(fileName).toAbsolutePath().normalize();
            content = content == null ? "" : content;

            Files.writeString(logFile, content, StandardCharsets.UTF_8);
            return logFile.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Nao foi possivel salvar o log em arquivo", ex);
        }
    }
}
