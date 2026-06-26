package com.example.springia.dto;

public record ArtifactChangeResponse(
        Long id,
        String filePath,
        String operation,
        String summary
) {
}

