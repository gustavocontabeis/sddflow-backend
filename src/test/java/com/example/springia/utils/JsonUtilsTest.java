package com.example.springia.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilsTest {

    @Test
    void toJsonAndFromJsonShouldSerializeAndDeserializeObject() {
        TestPayload payload = new TestPayload("gustavo", 3);

        String json = JsonUtils.toJson(payload);
        TestPayload result = JsonUtils.fromJson(json, TestPayload.class);

        assertTrue(json.contains("gustavo"));
        assertEquals(payload.name(), result.name());
        assertEquals(payload.count(), result.count());
    }

    @Test
    void fromJsonShouldSupportTypeReference() {
        String json = "[\"a\",\"b\",\"c\"]";

        List<String> result = JsonUtils.fromJson(json, new TypeReference<List<String>>() {});

        assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    void fromJsonShouldThrowWhenJsonIsBlank() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> JsonUtils.fromJson("   ", TestPayload.class)
        );

        assertEquals("JSON nao pode ser nulo ou vazio", ex.getMessage());
    }

    private record TestPayload(String name, int count) {
    }
}

