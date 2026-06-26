package com.example.springia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Entity(name = "lictb001_status_execucao")
public class ExecutionStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq001_status_execucao_seq")
    @SequenceGenerator(name = "licsq001_status_execucao_seq", sequenceName = "licsq001_status_execucao_seq", allocationSize = 1)
    @Column(name = "nu_id", nullable = false)
    @NotNull
    private Long nuId;

    @NotBlank
    @Column(name = "co_codigo", nullable = false, length = 30, unique = true)
    private String coCodigo;

    @NotBlank
    @Column(name = "de_descricao", nullable = false, length = 100)
    private String deDescricao;

    @NotNull
    @Column(name = "ic_ativo", nullable = false)
    private Boolean icAtivo;
}

