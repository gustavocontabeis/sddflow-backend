package com.example.springia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscoveryRepoDTO {
    private String linguagem;
    private List<String> frameworksBibliotecas;
    private String extensoesDeArquivosFonte;
    private List<String> conexoesComBancoDeDados;
    private List<String> integracoesComOutrosSistemas;
    private List<String> arquivosConfiguracao;
    private String regrasDeNegocio;
    private String descricaoEstruturaDiretorios;
    private String modelo;
    private String strutcture;
}
