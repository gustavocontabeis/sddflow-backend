package com.example.springia.service;

import com.example.springia.dto.VectorDocumentCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgVectorServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PgVectorService pgVectorService;

    @BeforeEach
    void setup() {
        pgVectorService = new PgVectorService(jdbcTemplate);
    }

    @Test
    void shouldConvertEmbeddingToPgVectorLiteral() {
        String vectorLiteral = pgVectorService.toVectorLiteral(List.of(1.5, 2.0, -3.25));
        assertEquals("[1.5,2.0,-3.25]", vectorLiteral);
    }

    @Test
    void shouldRejectEmbeddingWithWrongDimension() {
        VectorDocumentCreateRequest request = new VectorDocumentCreateRequest("doc", List.of(0.1, 0.2));
        assertThrows(IllegalArgumentException.class, () -> pgVectorService.createDocument(request));
    }

    @Test
    void shouldCreateDocumentWhenEmbeddingIsValid() {
        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < 1536; i++) {
            embedding.add(0.001);
        }

        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(), any())).thenReturn(10L);

        Long id = pgVectorService.createDocument(new VectorDocumentCreateRequest("doc", embedding));

        assertEquals(10L, id);
    }
}

