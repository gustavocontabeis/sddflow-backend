package com.example.springia.dto;

import com.example.springia.agent.model.CompileBy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExecutionRequest(
        @NotBlank String taskDescription,
        @NotNull CompileBy compileBy
) {
}

