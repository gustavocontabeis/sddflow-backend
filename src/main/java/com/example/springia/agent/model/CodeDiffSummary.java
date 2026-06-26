package com.example.springia.agent.model;

public record CodeDiffSummary(
        String filePath,
        int beforeLines,
        int afterLines,
        int addedLines,
        int removedLines,
        String summary
) {
}

