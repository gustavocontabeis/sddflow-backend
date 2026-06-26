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
@Entity(name = "lictb003_tentativa_execucao")
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq003_tentativa_execucao_seq")
    @SequenceGenerator(name = "licsq003_tentativa_execucao_seq", sequenceName = "licsq003_tentativa_execucao_seq", allocationSize = 1)
    @Column(name = "nu_id", nullable = false)
    @NotNull
    private Long nuId;

    @NotNull
    @Column(name = "nu_execucao_id", nullable = false)
    private Long nuExecucaoId;

    @Positive
    @Column(name = "nu_numero", nullable = false)
    private Integer nuNumero;

    @NotBlank
    @Column(name = "co_status", nullable = false, length = 30)
    private String coStatus;

    @NotNull
    @Column(name = "dh_inicio", nullable = false)
    private LocalDateTime dhInicio;

    @Column(name = "dh_fim")
    private LocalDateTime dhFim;

    @Column(name = "de_plano")
    @Lob
    private String dePlano;

    @NotBlank
    @Column(name = "de_prompt", nullable = false)
    @Lob
    private String dePrompt;

    @Column(name = "de_resposta")
    @Lob
    private String deResposta;

    @Column(name = "de_feedback")
    @Lob
    private String deFeedback;

    @Column(name = "de_erro")
    @Lob
    private String deErro;
}

