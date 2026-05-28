package com.example.springia.scanner.util;

import com.example.springia.scanner.model.Language;
import java.nio.file.Path;

/**
 * Detecta a linguagem de programação baseado na extensão do arquivo.
 */
public class LanguageDetector {
    
    private LanguageDetector() {
        // Classe utilitária, não deve ser instanciada
    }
    
    /**
     * Detecta a linguagem baseado na extensão do arquivo.
     *
     * @param filePath caminho do arquivo
     * @return Language detectada ou UNKNOWN se não reconhecida
     */
    public static Language detectLanguage(Path filePath) {
        return detectLanguage(filePath.getFileName().toString());
    }
    
    /**
     * Detecta a linguagem baseado no nome ou extensão do arquivo.
     *
     * @param filename nome do arquivo
     * @return Language detectada ou UNKNOWN se não reconhecida
     */
    public static Language detectLanguage(String filename) {
        if (filename == null || filename.isEmpty()) {
            return Language.UNKNOWN;
        }
        
        String lowerFilename = filename.toLowerCase();
        String extension = getExtension(lowerFilename);
        
        return switch (extension) {
            case "java" -> Language.JAVA;
            case "ts" -> Language.TYPESCRIPT;
            case "js" -> Language.JAVASCRIPT;
            case "html" -> Language.HTML;
            case "json" -> Language.JSON;
            case "xml" -> Language.XML;
            case "yml", "yaml" -> Language.YAML;
            default -> Language.UNKNOWN;
        };
    }
    
    /**
     * Extrai a extensão do arquivo.
     *
     * @param filename nome do arquivo
     * @return extensão sem o ponto, ou string vazia se não houver extensão
     */
    private static String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot <= 0) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}

