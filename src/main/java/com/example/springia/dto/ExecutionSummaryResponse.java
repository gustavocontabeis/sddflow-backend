package com.example.springia.dto;

import java.time.LocalDateTime;

public record ExecutionSummaryResponse(
        Long id,
        String status,
        String taskDescription,
        String compileBy,
        Integer attempts,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}

