package com.example.springia.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;

public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

    private JsonUtils() {
        // Classe utilitaria
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao serializar objeto para JSON", e);
        }
    }

    public static String toJsonFormated(Object value) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao serializar objeto para JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON nao pode ser nulo ou vazio");
        }

        if (type == null) {
            throw new IllegalArgumentException("Tipo nao pode ser nulo");
        }

        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao desserializar JSON para tipo informado", e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON nao pode ser nulo ou vazio");
        }

        if (typeReference == null) {
            throw new IllegalArgumentException("TypeReference nao pode ser nulo");
        }

        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao desserializar JSON para TypeReference informado", e);
        }
    }

    public static JsonNode toTree(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON nao pode ser nulo ou vazio");
        }

        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao converter JSON para arvore", e);
        }
    }

    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }

    public static <T> T toObject(String arguments, Class<T> classe) {
        if (arguments == null) {
            return null;
        }

        if (classe == null) {
            throw new IllegalArgumentException("Classe nao pode ser nula");
        }

        try {
            if (String.class.equals(classe)) {
                if (arguments.startsWith("\"") && arguments.endsWith("\"")) {
                    return OBJECT_MAPPER.readValue(arguments, classe);
                }

                return classe.cast(arguments);
            }

            return OBJECT_MAPPER.readValue(arguments, classe);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao converter JSON para tipo informado", e);
        }
    }
}
