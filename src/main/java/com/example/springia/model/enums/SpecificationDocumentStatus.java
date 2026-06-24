package com.example.springia.model.enums;

public enum SpecificationDocumentStatus {
    IN_PROGRESS("P", "Em andamento"),
    APPROVED("A", "Aprovado"),
    REJECTED("R", "Rejeitado");

    private final String sigla;
    private final String descricao;

    SpecificationDocumentStatus(String sigla, String descricao) {
        this.sigla = sigla;
        this.descricao = descricao;
    }

    public String getSigla() {
        return sigla;
    }

    public String getDescricao() {
        return descricao;
    }

    public static SpecificationDocumentStatus fromSigla(String sigla) {
        if (sigla == null || sigla.isBlank()) {
            throw new IllegalArgumentException("Sigla de status invalida: " + sigla);
        }

        String normalized = sigla.trim().toUpperCase();
        for (SpecificationDocumentStatus value : values()) {
            if (value.sigla.equals(normalized)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Sigla de status invalida: " + sigla);
    }
}
