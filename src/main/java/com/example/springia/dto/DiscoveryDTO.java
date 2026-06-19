package com.example.springia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Record representando as configuracoes de discovery no projeto.
 */
public record DiscoveryDTO(

    @JsonProperty("linguagem")
    String linguagem,

    @JsonProperty("frameworksBibliotecas")
    List<String> frameworksBibliotecas,

    @JsonProperty("extensoesDeArquivosFonte")
    String extensoesDeArquivosFonte,

    @JsonProperty("conexoesComBancoDeDados")
    List<String> conexoesComBancoDeDados,

    @JsonProperty("integracoesComOutrosSistemas")
    List<String> integracoesComOutrosSistemas,

    @JsonProperty("arquivosConfiguracao")
    List<String> arquivosConfiguracao,

    @JsonProperty("regrasDeNegocio")
    List<String> regrasDeNegocio

) {
}

