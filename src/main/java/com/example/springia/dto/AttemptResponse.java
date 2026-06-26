package com.example.springia.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AttemptResponse(
        Long id,
        Integer number,
        String status,
        String plan,
        String prompt,
        String response,
        String feedback,
        String error,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<ArtifactChangeResponse> changes,
        List<CompilationLogResponse> compilationLogs
) {
}

