package com.example.springia.scanner.service;

import com.example.springia.scanner.model.CodeFile;
import com.example.springia.scanner.model.CodeType;
import com.example.springia.scanner.model.Language;
import com.example.springia.scanner.util.Chunker;
import com.example.springia.scanner.util.CodeTypeClassifier;
import com.example.springia.scanner.util.LanguageDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Serviço responsável por escanear um repositório local e retornar uma lista
 * estruturada de arquivos de código em formato de chunks.
 *
 * Suporta:
 * - Varredura recursiva de diretórios
 * - Filtragem de diretórios ignorados (node_modules, target, dist, .git)
 * - Processamento de extensões específicas (.java, .ts, .js, .html, .json, .xml, .yml, .yaml)
 * - Detecção de linguagem baseada em extensão
 * - Classificação de tipo baseada em conteúdo
 * - Divisão de arquivos em chunks
 */
@Service
public class RepositoryScannerService {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryScannerService.class);

    // Diretórios a ignorar durante a varredura
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
        "node_modules",
        "target",
        "dist",
        ".git",
        "__pycache__",
        ".venv",
        "venv",
        ".env",
        "build",
        ".gradle",
        ".mvn",
        ".idea",
        ".vscode"
    );

    // Extensões de arquivo suportadas
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        ".java",
        ".ts",
        ".js",
        ".html",
        ".json",
        ".xml",
        ".yml",
        ".yaml"
    );

    // Tamanho máximo de cada chunk em caracteres
    private static final int DEFAULT_CHUNK_SIZE = 2000;

    /**
     * Escaneia um repositório local e retorna uma lista de CodeFile com chunks.
     *
     * @param rootPath caminho raiz do repositório a escanear
     * @return lista de CodeFile processados em chunks
     * @throws IllegalArgumentException se o caminho não existe ou não é um diretório
     */
    public List<CodeFile> scan(String rootPath) {
        return scan(rootPath, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Escaneia um repositório local com tamanho de chunk customizável.
     *
     * @param rootPath caminho raiz do repositório a escanear
     * @param chunkSize tamanho máximo de cada chunk em caracteres
     * @return lista de CodeFile processados em chunks
     * @throws IllegalArgumentException se o caminho não existe ou não é um diretório
     */
    public List<CodeFile> scan(String rootPath, int chunkSize) {
        Path root = Paths.get(rootPath);

        // Validar entrada
        if (!Files.exists(root)) {
            throw new IllegalArgumentException("Caminho não existe: " + rootPath);
        }

        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Caminho não é um diretório: " + rootPath);
        }

        logger.info("Iniciando scan do repositório: {}", rootPath);

        List<CodeFile> result = new ArrayList<>();

        try {
            scanDirectory(root, root, result, chunkSize);
            logger.info("Scan concluído com sucesso. Total de chunks processados: {}", result.size());
        } catch (IOException e) {
            logger.error("Erro durante o scan do repositório: {}", rootPath, e);
            throw new RuntimeException("Erro ao escanear repositório", e);
        }

        return result;
    }

    /**
     * Recursivamente escaneia um diretório.
     *
     * @param currentPath diretório atual sendo processado
     * @param rootPath caminho raiz para cálculo de caminhos relativos
     * @param result lista acumuladora de resultados
     * @param chunkSize tamanho máximo de chunk
     * @throws IOException se ocorrer erro de I/O
     */
    private void scanDirectory(Path currentPath, Path rootPath, List<CodeFile> result, int chunkSize)
            throws IOException {

        try (Stream<Path> paths = Files.list(currentPath)) {
            paths.forEach(path -> {
                try {
                    if (shouldIgnorePath(path)) {
                        logger.debug("Ignorando: {}", path);
                        return;
                    }

                    if (Files.isDirectory(path)) {
                        scanDirectory(path, rootPath, result, chunkSize);
                    } else if (isSupportedFile(path)) {
                        processFile(path, rootPath, result, chunkSize);
                    }
                } catch (Exception e) {
                    logger.warn("Erro ao processar arquivo: {}", path, e);
                    // Continuar processando outros arquivos
                }
            });
        }
    }

    /**
     * Processa um arquivo de código individual.
     *
     * @param filePath caminho do arquivo
     * @param rootPath caminho raiz para cálculo de caminho relativo
     * @param result lista acumuladora de resultados
     * @param chunkSize tamanho máximo de chunk
     * @throws IOException se ocorrer erro de I/O
     */
    private void processFile(Path filePath, Path rootPath, List<CodeFile> result, int chunkSize)
            throws IOException {

        logger.debug("Processando arquivo: {}", filePath);

        // Ler conteúdo do arquivo
        String content = Files.readString(filePath, StandardCharsets.UTF_8);

        // Informações básicas do arquivo
        String relativePath = rootPath.relativize(filePath).toString();
        Language language = LanguageDetector.detectLanguage(filePath);
        CodeType type = CodeTypeClassifier.classify(content, language);
        long fileSize = Files.size(filePath);
        int lineCount = content.split("\n", -1).length;

        // Dividir em chunks
        List<String> chunks = Chunker.chunk(content, chunkSize);
        int totalChunks = chunks.size();

        logger.debug("Arquivo {} dividido em {} chunks", relativePath, totalChunks);

        // Criar CodeFile para cada chunk
        for (int i = 0; i < chunks.size(); i++) {
            CodeFile codeFile = CodeFile.builder()
                .path(relativePath)
                .language(language)
                .content(chunks.get(i))
                .type(type)
                .chunkNumber(i + 1)
                .totalChunks(totalChunks)
                .fileSize(fileSize)
                .lineCount(lineCount)
                .build();

            result.add(codeFile);
        }
    }

    /**
     * Verifica se um caminho deve ser ignorado.
     *
     * @param path caminho a verificar
     * @return true se deve ser ignorado
     */
    private boolean shouldIgnorePath(Path path) {
        String fileName = path.getFileName().toString();

        // Ignorar diretórios específicos
        if (Files.isDirectory(path)) {
            return IGNORED_DIRECTORIES.contains(fileName) || fileName.startsWith(".");
        }

        // Ignorar arquivos que começam com ponto
        return fileName.startsWith(".");
    }

    /**
     * Verifica se um arquivo possui extensão suportada.
     *
     * @param path caminho do arquivo
     * @return true se a extensão é suportada
     */
    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();

        for (String extension : SUPPORTED_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retorna as extensões de arquivo suportadas.
     *
     * @return set de extensões suportadas
     */
    public static Set<String> getSupportedExtensions() {
        return new HashSet<>(SUPPORTED_EXTENSIONS);
    }

    /**
     * Retorna os diretórios ignorados.
     *
     * @return set de diretórios ignorados
     */
    public static Set<String> getIgnoredDirectories() {
        return new HashSet<>(IGNORED_DIRECTORIES);
    }
}

