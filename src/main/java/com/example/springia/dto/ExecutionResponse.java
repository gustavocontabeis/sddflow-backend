package com.example.springia.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ExecutionResponse(
        Long id,
        String status,
        String taskDescription,
        String compileBy,
        Integer currentAttempt,
        Integer maxAttempts,
        String backendPath,
        String frontendPath,
        String summary,
        String result,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<AttemptResponse> attempts
) {
}

