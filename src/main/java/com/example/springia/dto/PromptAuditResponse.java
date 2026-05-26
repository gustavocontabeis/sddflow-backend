package com.example.springia.dto;

public record PromptAuditResponse(
        Long userStoryId,
        String question,
        String source,
        String promptKey,
        Integer confidence,
        String justification,
        String excerpt,
        String rawResponse
) {
}

