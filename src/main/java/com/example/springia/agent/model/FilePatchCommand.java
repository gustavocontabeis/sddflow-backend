package com.example.springia.agent.model;

public record FilePatchCommand(
        String oldText,
        String newText,
        Integer occurrenceIndex
) {
}

