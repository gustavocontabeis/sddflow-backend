package com.example.springia.scanner.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa um arquivo de código processado pelo scanner.
 * Contém metadados e conteúdo do arquivo em forma de chunks.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeFile {
    
    /**
     * Caminho relativo do arquivo no repositório.
     */
    private String path;
    
    /**
     * Linguagem de programação detectada.
     */
    private Language language;
    
    /**
     * Conteúdo do arquivo (ou chunk, se dividido).
     */
    private String content;
    
    /**
     * Tipo de arquivo detectado (controller, service, repository, entity, unknown).
     */
    private CodeType type;
    
    /**
     * Número do chunk (para arquivos divididos em múltiplos chunks).
     * 0 indica que o arquivo não foi dividido.
     */
    @Builder.Default
    private Integer chunkNumber = 0;
    
    /**
     * Total de chunks para este arquivo.
     * 1 indica que o arquivo não foi dividido.
     */
    @Builder.Default
    private Integer totalChunks = 1;
    
    /**
     * Tamanho total do arquivo original em bytes.
     */
    private Long fileSize;
    
    /**
     * Número de linhas do arquivo original.
     */
    private Integer lineCount;
}

