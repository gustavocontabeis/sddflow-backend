package com.example.springia.agent.model;

import java.util.List;

public record FileChangeCommand(
        String filePath,
        ChangeOperation operation,
        String content,
        String summary,
        String expectedFileSha256,
        Boolean allowFullReplace,
        List<FilePatchCommand> patches
) {

    public FileChangeCommand {
        allowFullReplace = Boolean.TRUE.equals(allowFullReplace);
        patches = patches == null ? List.of() : patches;
    }

    public FileChangeCommand(String filePath, ChangeOperation operation, String content, String summary) {
        this(filePath, operation, content, summary, null, Boolean.FALSE, List.of());
    }
}

