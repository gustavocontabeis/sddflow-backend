package com.example.springia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Record representando as configuracoes de discovery no projeto.
 */
public record DiscoveryDTO(

    String linguagem,

    @JsonProperty("frameworksBibliotecas")
    List<String> frameworksBibliotecas,

    @JsonProperty("conexoesComBancoDeDados")
    List<Map<String, Object>> conexoesComBancoDeDados,

    @JsonProperty("integracoesComOutrosSistemas")
    List<Map<String, Object>> integracoesComOutrosSistemas,

    @JsonProperty("arquivosConfiguracao")
    List<String> arquivosConfiguracao
) {
}

