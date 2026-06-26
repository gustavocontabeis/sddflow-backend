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

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "lictb005_log_compilacao")
public class CompilationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq005_log_compilacao_seq")
    @SequenceGenerator(name = "licsq005_log_compilacao_seq", sequenceName = "licsq005_log_compilacao_seq", allocationSize = 1)
    @Column(name = "nu_id", nullable = false)
    @NotNull
    private Long nuId;

    @NotNull
    @Column(name = "nu_tentativa_id", nullable = false)
    private Long nuTentativaId;

    @NotBlank
    @Column(name = "co_destino", nullable = false, length = 30)
    private String coDestino;

    @NotBlank
    @Column(name = "co_comando", nullable = false, length = 200)
    private String coComando;

    @NotBlank
    @Column(name = "co_status", nullable = false, length = 30)
    private String coStatus;

    @NotNull
    @Column(name = "ic_sucesso", nullable = false)
    private Boolean icSucesso;

    @NotNull
    @Column(name = "dh_inicio", nullable = false)
    private LocalDateTime dhInicio;

    @Column(name = "dh_fim")
    private LocalDateTime dhFim;

    @Column(name = "de_saida")
    @Lob
    private String deSaida;
}

