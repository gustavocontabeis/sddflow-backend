package com.example.springia.scanner.util;

import com.example.springia.scanner.model.CodeType;
import com.example.springia.scanner.model.Language;

/**
 * Classifica o tipo de arquivo baseado no conteúdo e linguagem.
 * Detecta anotações Spring como @RestController, @Service, @Repository, @Entity.
 */
public class CodeTypeClassifier {
    
    private CodeTypeClassifier() {
        // Classe utilitária, não deve ser instanciada
    }
    
    /**
     * Classifica o tipo de arquivo baseado no conteúdo.
     *
     * @param content conteúdo do arquivo
     * @param language linguagem do arquivo
     * @return CodeType detectado
     */
    public static CodeType classify(String content, Language language) {
        if (content == null || content.isEmpty()) {
            return CodeType.UNKNOWN;
        }
        
        // Apenas analisa arquivos Java que contêm anotações Spring
        if (language != Language.JAVA) {
            return CodeType.UNKNOWN;
        }
        
        String lowerContent = content.toLowerCase();
        
        // Verificar em ordem de especificidade
        if (containsAnnotation(lowerContent, "@restcontroller", "@controller")) {
            return CodeType.CONTROLLER;
        }
        
        if (containsAnnotation(lowerContent, "@service")) {
            return CodeType.SERVICE;
        }
        
        if (containsAnnotation(lowerContent, "@repository")) {
            return CodeType.REPOSITORY;
        }
        
        if (containsAnnotation(lowerContent, "@entity")) {
            return CodeType.ENTITY;
        }
        
        return CodeType.UNKNOWN;
    }
    
    /**
     * Verifica se o conteúdo contém alguma das anotações especificadas.
     *
     * @param content conteúdo em minúsculas
     * @param annotations anotações a procurar
     * @return true se alguma anotação foi encontrada
     */
    private static boolean containsAnnotation(String content, String... annotations) {
        for (String annotation : annotations) {
            if (content.contains(annotation)) {
                return true;
            }
        }
        return false;
    }
}

