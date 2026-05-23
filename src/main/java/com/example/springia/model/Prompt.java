package com.example.springia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
@Entity
public class Prompt {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "prompt_key", nullable = false, unique = true, length = 100)
    private String key;

    @Lob
    @NotBlank
    @Column(name = "content", nullable = false)
    private String content;

}
