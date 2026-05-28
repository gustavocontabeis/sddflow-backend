package com.example.springia.scanner.model;

/**
 * Enumeração das linguagens de programação suportadas.
 */
public enum Language {
    JAVA("java"),
    TYPESCRIPT("typescript"),
    JAVASCRIPT("javascript"),
    HTML("html"),
    JSON("json"),
    XML("xml"),
    YAML("yaml"),
    UNKNOWN("unknown");

    private final String value;

    Language(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Retorna a Language correspondente ao valor string.
     */
    public static Language fromValue(String value) {
        for (Language lang : Language.values()) {
            if (lang.value.equalsIgnoreCase(value)) {
                return lang;
            }
        }
        return UNKNOWN;
    }
}

