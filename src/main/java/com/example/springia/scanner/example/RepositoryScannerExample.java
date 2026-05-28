package com.example.springia.scanner.example;

import com.example.springia.scanner.model.CodeFile;
import com.example.springia.scanner.model.CodeType;
import com.example.springia.scanner.model.Language;
import com.example.springia.scanner.service.RepositoryScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exemplo de uso completo do RepositoryScannerService.
 *
 * Demonstra como utilizar o scanner para:
 * 1. Escanear um repositório
 * 2. Classificar e filtrar arquivos
 * 3. Gerar estatísticas
 * 4. Processar conteúdo
 */
@Component
public class RepositoryScannerExample {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryScannerExample.class);

    @Autowired
    private RepositoryScannerService scannerService;

    /**
     * Exemplo básico: escanear um repositório
     */
    public void exampleBasicScan(String repositoryPath) {
        logger.info("=== Exemplo 1: Scan Básico ===");

        List<CodeFile> files = scannerService.scan(repositoryPath);

        logger.info("Total de chunks processados: {}", files.size());
        logger.info("Arquivos únicos: {}", files.stream()
            .map(CodeFile::getPath)
            .distinct()
            .count());

        files.stream().limit(5).forEach(file ->
            logger.info("  - {} ({}) - Tipo: {}",
                        file.getPath(),
                        file.getLanguage(),
                        file.getType())
        );
    }

    /**
     * Exemplo 2: Filtrar apenas controllers
     */
    public void exampleFindControllers(String repositoryPath) {
        logger.info("=== Exemplo 2: Encontrar Controllers ===");

        List<CodeFile> files = scannerService.scan(repositoryPath);

        List<CodeFile> controllers = files.stream()
            .filter(f -> f.getType() == CodeType.CONTROLLER)
            .collect(Collectors.toList());

        logger.info("Controllers encontrados: {}", controllers.size());
        controllers.forEach(file ->
            logger.info("  Arquivo: {} ({} bytes, {} linhas)",
                       file.getPath(),
                       file.getFileSize(),
                       file.getLineCount())
        );
    }

    /**
     * Exemplo 3: Estatísticas por linguagem
     */
    public void exampleStatisticsByLanguage(String repositoryPath) {
        logger.info("=== Exemplo 3: Estatísticas por Linguagem ===");

        List<CodeFile> files = scannerService.scan(repositoryPath);

        Map<Language, Long> byLanguage = files.stream()
            .map(CodeFile::getPath)
            .distinct()
            .map(path -> files.stream()
                .filter(f -> f.getPath().equals(path))
                .findFirst())
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get())
            .collect(Collectors.groupingBy(CodeFile::getLanguage,
                                          Collectors.counting()));

        logger.info("Arquivos por linguagem:");
        byLanguage.forEach((lang, count) ->
            logger.info("  {}: {} arquivos", lang.getValue(), count)
        );
    }

    /**
     * Exemplo 4: Estatísticas por tipo
     */
    public void exampleStatisticsByType(String repositoryPath) {
        logger.info("=== Exemplo 4: Estatísticas por Tipo ===");

        List<CodeFile> files = scannerService.scan(repositoryPath);

        Map<CodeType, Long> byType = files.stream()
            .map(CodeFile::getPath)
            .distinct()
            .map(path -> files.stream()
                .filter(f -> f.getPath().equals(path))
                .findFirst())
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get())
            .collect(Collectors.groupingBy(CodeFile::getType,
                                          Collectors.counting()));

        logger.info("Arquivos por tipo:");
        byType.forEach((type, count) ->
            logger.info("  {}: {} arquivos", type.getValue(), count)
        );
    }

    /**
     * Exemplo 5: Encontrar arquivos grandes
     */
    public void exampleFindLargeFiles(String repositoryPath, long sizeThreshold) {
        logger.info("=== Exemplo 5: Arquivos Grandes (> {} bytes) ===", sizeThreshold);

        List<CodeFile> files = scannerService.scan(repositoryPath);

        files.stream()
            .map(CodeFile::getPath)
            .distinct()
            .map(path -> files.stream()
                .filter(f -> f.getPath().equals(path))
                .findFirst())
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get())
            .filter(f -> f.getFileSize() != null && f.getFileSize() > sizeThreshold)
            .sorted((a, b) -> Long.compare(b.getFileSize(), a.getFileSize()))
            .forEach(file ->
                logger.info("  {} - {} bytes", file.getPath(), file.getFileSize())
            );
    }

    /**
     * Exemplo 6: Processar cada arquivo
     */
    public void exampleProcessEachFile(String repositoryPath) {
        logger.info("=== Exemplo 6: Processar Cada Arquivo ===");

        List<CodeFile> files = scannerService.scan(repositoryPath);

        files.stream()
            .map(CodeFile::getPath)
            .distinct()
            .limit(3)
            .forEach(path -> {
                logger.info("Processando: {}", path);

                List<CodeFile> chunks = files.stream()
                    .filter(f -> f.getPath().equals(path))
                    .collect(Collectors.toList());

                chunks.forEach(chunk ->
                    logger.info("  Chunk {}/{}: {} caracteres",
                               chunk.getChunkNumber(),
                               chunk.getTotalChunks(),
                               chunk.getContent().length())
                );
            });
    }

    /**
     * Exemplo 7: Análise de complexidade (aproximada)
     */
    public void exampleComplexityAnalysis(String repositoryPath) {
        logger.info("=== Exemplo 7: Análise de Complexidade ===");

        List<CodeFile> files = scannerService.scan(repositoryPath);

        Map<CodeType, Long> linesOfCode = files.stream()
            .map(CodeFile::getPath)
            .distinct()
            .map(path -> files.stream()
                .filter(f -> f.getPath().equals(path))
                .findFirst())
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get())
            .collect(Collectors.groupingBy(CodeFile::getType,
                                          Collectors.summingLong(f -> f.getLineCount() != null ? f.getLineCount() : 0)));

        logger.info("Linhas de código por tipo:");
        linesOfCode.forEach((type, lines) ->
            logger.info("  {}: {} linhas", type.getValue(), lines)
        );

        long totalLines = linesOfCode.values().stream()
            .mapToLong(Long::longValue)
            .sum();
        logger.info("Total: {} linhas", totalLines);
    }

    /**
     * Exemplo 8: Encontrar arquivos com padrão específico
     */
    public void exampleFindByPatternInName(String repositoryPath, String pattern) {
        logger.info("=== Exemplo 8: Encontrar arquivos com padrão '{}' no nome ===", pattern);

        List<CodeFile> files = scannerService.scan(repositoryPath);

        files.stream()
            .map(CodeFile::getPath)
            .distinct()
            .filter(path -> path.contains(pattern))
            .forEach(path -> logger.info("  {}", path));
    }

    /**
     * Exemplo 9: Pipeline completo de processamento
     */
    public void exampleCompletePipeline(String repositoryPath) {
        logger.info("=== Exemplo 9: Pipeline Completo ===");

        // 1. Scan
        logger.info("1. Escaneando repositório...");
        List<CodeFile> files = scannerService.scan(repositoryPath);

        // 2. Estatísticas
        logger.info("2. Calculando estatísticas...");
        long totalChunks = files.size();
        long uniqueFiles = files.stream()
            .map(CodeFile::getPath)
            .distinct()
            .count();
        long totalSize = files.stream()
            .map(CodeFile::getPath)
            .distinct()
            .map(path -> files.stream()
                .filter(f -> f.getPath().equals(path))
                .findFirst())
            .filter(opt -> opt.isPresent())
            .mapToLong(opt -> opt.get().getFileSize() != null ? opt.get().getFileSize() : 0)
            .sum();

        logger.info("   Total de chunks: {}", totalChunks);
        logger.info("   Arquivos únicos: {}", uniqueFiles);
        logger.info("   Tamanho total: {} KB", totalSize / 1024);

        // 3. Filtrar Java
        logger.info("3. Filtrando arquivos Java...");
        long javaFiles = files.stream()
            .filter(f -> f.getLanguage() == Language.JAVA)
            .map(CodeFile::getPath)
            .distinct()
            .count();
        logger.info("   Arquivos Java: {}", javaFiles);

        // 4. Encontrar Services
        logger.info("4. Encontrando Services...");
        long services = files.stream()
            .filter(f -> f.getType() == CodeType.SERVICE)
            .map(CodeFile::getPath)
            .distinct()
            .count();
        logger.info("   Services encontrados: {}", services);

        logger.info("Pipeline concluído!");
    }
}

