package com.example.springia.scanner.model;

/**
 * Enumeração dos tipos de arquivo de código detectados pelo scanner.
 */
public enum CodeType {
    CONTROLLER("controller"),
    SERVICE("service"),
    REPOSITORY("repository"),
    ENTITY("entity"),
    UNKNOWN("unknown");

    private final String value;

    CodeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Retorna o CodeType correspondente ao valor string.
     */
    public static CodeType fromValue(String value) {
        for (CodeType type : CodeType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}

