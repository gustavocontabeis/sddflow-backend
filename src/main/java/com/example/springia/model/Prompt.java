package com.example.springia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "lictb001_prompt")
public class Prompt {

    @Id
    @SequenceGenerator(name = "licsq001_prompt", sequenceName = "licsq001_prompt", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq001_prompt")
    @Column(name = "nu_prompt", nullable = false)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "co_chave_prompt", nullable = false, unique = true, length = 100)
    private String key;

    @NotBlank
    @Size(max = 5000)
    @Column(name = "de_prompt", nullable = false, length = 5000, columnDefinition = "TEXT")
    private String content;

}
