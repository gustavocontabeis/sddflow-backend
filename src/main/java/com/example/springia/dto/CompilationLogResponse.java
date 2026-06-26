package com.example.springia.dto;

import java.time.LocalDateTime;

public record CompilationLogResponse(
        Long id,
        String target,
        String command,
        boolean success,
        boolean timedOut,
        int exitCode,
        String output,
        String errorOutput,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}

