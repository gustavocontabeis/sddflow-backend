package com.example.springia.agent.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum ChangeOperation {
    CREATE,
    UPDATE,
    DELETE;

    @JsonCreator
    public static ChangeOperation fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("operation nao pode ser nulo ou vazio");
        }

        return Arrays.stream(values())
                .filter(operation -> operation.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("operation invalido: " + value));
    }
}

