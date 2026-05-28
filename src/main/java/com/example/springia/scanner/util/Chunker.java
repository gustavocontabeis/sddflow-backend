package com.example.springia.scanner.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitário para dividir conteúdo em chunks de tamanho máximo.
 */
public class Chunker {
    
    private Chunker() {
        // Classe utilitária, não deve ser instanciada
    }
    
    /**
     * Divide o conteúdo em chunks de tamanho máximo especificado.
     * Tenta manter a divisão limpa em quebras de linha quando possível.
     *
     * @param content conteúdo a ser dividido
     * @param maxSize tamanho máximo em caracteres para cada chunk
     * @return lista de chunks
     */
    public static List<String> chunk(String content, int maxSize) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize deve ser maior que 0");
        }
        
        List<String> chunks = new ArrayList<>();
        int length = content.length();
        
        for (int i = 0; i < length; i += maxSize) {
            int endIndex = Math.min(i + maxSize, length);
            
            // Tenta encontrar a última quebra de linha dentro do chunk
            if (endIndex < length && endIndex > i) {
                int lastNewline = content.lastIndexOf('\n', endIndex - 1);
                if (lastNewline > i) {
                    endIndex = lastNewline + 1;
                }
            }
            
            String chunk = content.substring(i, endIndex).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
        }
        
        return chunks;
    }
    
    /**
     * Retorna o número de chunks necessários para o conteúdo.
     *
     * @param content conteúdo a ser analisado
     * @param maxSize tamanho máximo em caracteres para cada chunk
     * @return número de chunks
     */
    public static int getChunkCount(String content, int maxSize) {
        return chunk(content, maxSize).size();
    }
}

