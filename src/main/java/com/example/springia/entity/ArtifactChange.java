package com.example.springia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "lictb004_alteracao_artefato")
public class ArtifactChange {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq004_alteracao_artefato_seq")
    @SequenceGenerator(name = "licsq004_alteracao_artefato_seq", sequenceName = "licsq004_alteracao_artefato_seq", allocationSize = 1)
    @Column(name = "nu_id", nullable = false)
    @NotNull
    private Long nuId;

    @NotNull
    @Column(name = "nu_tentativa_id", nullable = false)
    private Long nuTentativaId;

    @NotBlank
    @Column(name = "co_tipo_acao", nullable = false, length = 30)
    private String coTipoAcao;

    @NotBlank
    @Column(name = "de_caminho_arquivo", nullable = false, length = 500)
    private String deCaminhoArquivo;

    @Column(name = "de_conteudo_anterior")
    @Lob
    private String deConteudoAnterior;

    @Column(name = "de_conteudo_novo")
    @Lob
    private String deConteudoNovo;

    @NotBlank
    @Column(name = "de_resumo", nullable = false)
    @Lob
    private String deResumo;
}

