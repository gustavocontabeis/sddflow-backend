package com.example.springia.scanner.controller;

import com.example.springia.scanner.model.CodeFile;
import com.example.springia.scanner.model.StackDiscoveryReport;
import com.example.springia.scanner.service.RepositoryScannerService;
import com.example.springia.scanner.service.StackDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para gerenciar o scanning de repositórios.
 */
@RestController
@RequestMapping("/api/scanner")
@RequiredArgsConstructor
public class RepositoryScannerController {

    private final RepositoryScannerService scannerService;
    private final StackDiscoveryService stackDiscoveryService;

    /**
     * Escaneia um repositório e retorna lista de arquivos em chunks.
     *
     * @param repositoryPath caminho do repositório a escanear
     * @param chunkSize tamanho máximo de chunk (padrão: 2000)
     * @return lista de CodeFile processados
     * Exemplo:
     * {@code curl -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp"}
     * {@code curl -X POST "http://localhost:8080/api/scanner/scan?repositoryPath=/home/user/myapp&chunkSize=5000"}
     * Response:
     * {
     *   "success": true,
     *   "totalChunks": 150,
     *   "totalFiles": 25,
     *   "codeFiles": [
     *     {
     *       "path": "src/main/java/com/example/UserController.java",
     *       "language": "JAVA",
     *       "type": "CONTROLLER",
     *       "chunkNumber": 1,
     *       "totalChunks": 2,
     *       "fileSize": 3500,
     *       "lineCount": 95,
     *       "content": "package com.example;..."
     *     }
     *   ]
     * }
     */
    @PostMapping("/scan")
    public ResponseEntity<ScannerResponse> scan(
            @RequestParam String repositoryPath,
            @RequestParam(defaultValue = "2000") int chunkSize) {

        try {
            List<CodeFile> codeFiles = scannerService.scan(repositoryPath, chunkSize);

            ScannerResponse response = ScannerResponse.builder()
                .success(true)
                .totalChunks(codeFiles.size())
                .totalFiles(countUniqueFiles(codeFiles))
                .codeFiles(codeFiles)
                .build();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ScannerResponse response = ScannerResponse.builder()
                .success(false)
                .error(e.getMessage())
                .build();

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            ScannerResponse response = ScannerResponse.builder()
                .success(false)
                .error("Erro ao escanear repositório: " + e.getMessage())
                .build();

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Retorna informações sobre extensões e diretórios suportados.
     *
     * @return configuração do scanner
     * Exemplo:
     * {@code curl -X GET "http://localhost:8080/api/scanner/config"}
     * Response:
     * {
     *   "supportedExtensions": [".java", ".ts", ".js", ".html", ".json", ".xml", ".yml", ".yaml"],
     *   "ignoredDirectories": ["node_modules", "target", "dist", ".git", "build", ...],
     *   "defaultChunkSize": 2000
     * }
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
            "supportedExtensions", RepositoryScannerService.getSupportedExtensions(),
            "ignoredDirectories", RepositoryScannerService.getIgnoredDirectories(),
            "defaultChunkSize", 2000
        ));
    }

    /**
     * Descobre a stack tecnologica do repositorio (linguagem, bibliotecas, frameworks, etc).
     *
     * @param repositoryPath caminho do repositorio a analisar
     * @return relatorio estruturado da stack
     * Exemplo:
     * {@code curl -X GET "http://localhost:8080/api/scanner/stack?repositoryPath=/home/user/myapp"}
     */
    @GetMapping("/stack")
    public ResponseEntity<?> discoverStack(@RequestParam String repositoryPath) {
        try {
            StackDiscoveryReport report = stackDiscoveryService.discover(repositoryPath);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Erro ao descobrir stack: " + e.getMessage()
            ));
        }
    }

    /**
     * Conta número de arquivos únicos na lista de chunks.
     */
    private long countUniqueFiles(List<CodeFile> codeFiles) {
        return codeFiles.stream()
            .map(CodeFile::getPath)
            .distinct()
            .count();
    }

    /**
     * DTO para resposta do scanner.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ScannerResponse {
        private boolean success;
        private Integer totalChunks;
        private Long totalFiles;
        private List<CodeFile> codeFiles;
        private String error;
    }
}

