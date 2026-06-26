package com.example.springia.agent.model;

public record FileWriteResult(
        String filePath,
        ChangeOperation operation,
        boolean written,
        String backupPath,
        CodeDiffSummary diffSummary,
        String message
) {
}

