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
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "lictb002_execucao")
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq002_execucao_seq")
    @SequenceGenerator(name = "licsq002_execucao_seq", sequenceName = "licsq002_execucao_seq", allocationSize = 1)
    @Column(name = "nu_id", nullable = false)
    @NotNull
    private Long nuId;

    @NotNull
    @Column(name = "nu_status_id", nullable = false)
    private Long nuStatusId;

    @NotBlank
    @Column(name = "co_status", nullable = false, length = 30)
    private String coStatus;

    @Positive
    @Column(name = "nu_tentativa_atual", nullable = false)
    private Integer nuTentativaAtual;

    @Positive
    @Column(name = "nu_max_tentativas", nullable = false)
    private Integer nuMaxTentativas;

    @NotNull
    @Column(name = "dh_inicio", nullable = false)
    private LocalDateTime dhInicio;

    @Column(name = "dh_fim")
    private LocalDateTime dhFim;

    @NotBlank
    @Column(name = "co_compile_by", nullable = false, length = 30)
    private String coCompileBy;

    @NotBlank
    @Column(name = "de_solicitacao", nullable = false)
    @Lob
    private String deSolicitacao;

    @NotBlank
    @Column(name = "de_contexto", nullable = false)
    @Lob
    private String deContexto;

    @Column(name = "de_resultado")
    @Lob
    private String deResultado;

    @NotBlank
    @Column(name = "de_backend_path", nullable = false)
    private String deBackendPath;

    @NotBlank
    @Column(name = "de_frontend_path", nullable = false)
    private String deFrontendPath;
}


