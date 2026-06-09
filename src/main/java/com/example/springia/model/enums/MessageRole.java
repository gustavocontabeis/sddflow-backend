package com.example.springia.model.enums;

public enum MessageRole {
    USER("U", "Usuario"),
    ASSISTANT("A", "Assistente");

    private final String sigla;
    private final String descricao;

    MessageRole(String sigla, String descricao) {
        this.sigla = sigla;
        this.descricao = descricao;
    }

    public String getSigla() {
        return sigla;
    }

    public String getDescricao() {
        return descricao;
    }

    public static MessageRole fromSigla(String sigla) {
        if (sigla == null || sigla.isBlank()) {
            throw new IllegalArgumentException("Sigla de role invalida: " + sigla);
        }

        String normalized = sigla.trim().toUpperCase();
        if ("USER".equals(normalized)) {
            return USER;
        }
        if ("ASSISTANT".equals(normalized)) {
            return ASSISTANT;
        }

        for (MessageRole value : values()) {
            if (value.sigla.equals(normalized)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Sigla de role invalida: " + sigla);
    }
}

