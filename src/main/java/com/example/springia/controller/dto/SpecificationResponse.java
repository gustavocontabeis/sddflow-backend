package com.example.springia.controller.dto;

import java.time.LocalDateTime;

public record SpecificationResponse(
        Long id,
        String sessionId,
        String content,
        LocalDateTime generatedAt
) {
}

