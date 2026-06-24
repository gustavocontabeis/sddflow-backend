package com.example.springia.model.enums;

public enum CodeRepoType {
    BACKEND("B", "Backend"),
    FRONTEND("F", "Frontend"),
    DOCUMENTATION("D", "Documentacao");

    private final String sigla;
    private final String descricao;

    CodeRepoType(String sigla, String descricao) {
        this.sigla = sigla;
        this.descricao = descricao;
    }

    public String getSigla() {
        return sigla;
    }

    public String getDescricao() {
        return descricao;
    }

    public static CodeRepoType fromSigla(String sigla) {
        if (sigla == null || sigla.isBlank()) {
            throw new IllegalArgumentException("Sigla de tipo de repositorio invalida: " + sigla);
        }

        String normalized = sigla.trim().toUpperCase();
        if ("BACKEND".equals(normalized)) {
            return BACKEND;
        }
        if ("FRONTEND".equals(normalized)) {
            return FRONTEND;
        }
        if ("DOCUMENTATION".equals(normalized)) {
            return DOCUMENTATION;
        }

        for (CodeRepoType value : values()) {
            if (value.sigla.equals(normalized)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Sigla de tipo de repositorio invalida: " + sigla);
    }
}
