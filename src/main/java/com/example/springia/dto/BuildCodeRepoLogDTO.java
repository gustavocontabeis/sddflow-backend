package com.example.springia.dto;

import com.example.springia.model.CodeRepo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class BuildCodeRepoLogDTO {
    private CodeRepo dodeRepo;
    private boolean ok;
    private String logErro;
    private String codigoOriginal;
    private String codigoCorrigido;
}
