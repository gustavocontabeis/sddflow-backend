package com.example.springia.scanner.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Relatorio estruturado da stack tecnologica de um repositorio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StackDiscoveryReport {
    private String rootPath;
    private Integer totalFiles;
    private Integer totalChunks;

    private String primaryLanguage;
    private Map<String, Long> languageFileCount;
    private Map<String, Long> codeTypeFileCount;

    private String javaVersion;
    private String springBootVersion;

    private List<String> manifestsFound;
    private List<String> buildTools;
    private List<String> packageManagers;

    private List<String> frameworks;
    private List<String> libraries;
    private List<String> testLibraries;
    private List<String> databases;
    private List<String> cloudServices;
}

