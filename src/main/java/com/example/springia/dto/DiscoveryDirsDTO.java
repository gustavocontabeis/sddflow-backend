package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscoveryDirsDTO {

    private String linguagem;
    private String[] pacotesDeDominio;
    private String[] pacotesRegrasNegocio;
    private String[] pacotesEndpointsRest;
    private String[] arquivosConfiguracao;
    private String descricaoEstruturaDiretorios;

}
