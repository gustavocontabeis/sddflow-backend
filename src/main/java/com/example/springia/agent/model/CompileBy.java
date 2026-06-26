package com.example.springia.agent.model;

public enum CompileBy {
    COMMAND,
    DOCKER;

    public static CompileBy from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("compileBy nao pode ser nulo ou vazio");
        }

        return CompileBy.valueOf(value.trim().toUpperCase());
    }
}

