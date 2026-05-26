package com.example.springia.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptAuditRequest(@NotBlank String question) {
}

