package com.example.springia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorDocumentCreateRequest {

    @NotBlank
    private String content;

    @NotEmpty
    private List<Double> embedding;
}

