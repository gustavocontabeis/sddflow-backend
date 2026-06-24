package com.example.springia.service;

import com.example.springia.dto.VectorDocumentCreateRequest;
import com.example.springia.dto.VectorSearchRequest;
import com.example.springia.dto.VectorSearchResultResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PgVectorService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 50;
    private static final int EMBEDDING_DIMENSION = 1536;

    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public PgVectorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Long createDocument(VectorDocumentCreateRequest request) {
        ensurePgVectorInfrastructure();
        validateEmbedding(request.getEmbedding());

        String vector = toVectorLiteral(request.getEmbedding());
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO tb_vector_document (ds_content, ds_embedding)
                VALUES (?, CAST(? AS vector))
                RETURNING id_vector_document
                """,
                Long.class,
                request.getContent(),
                vector
        );
    }

    @Transactional(readOnly = true)
    public List<VectorSearchResultResponse> findNearest(VectorSearchRequest request) {
        ensurePgVectorInfrastructure();
        validateEmbedding(request.getEmbedding());

        int limit = normalizeLimit(request.getLimit());
        String vector = toVectorLiteral(request.getEmbedding());

        return jdbcTemplate.query(
                """
                SELECT id_vector_document, ds_content, (ds_embedding <-> CAST(? AS vector)) AS nu_distance
                FROM tb_vector_document
                ORDER BY ds_embedding <-> CAST(? AS vector)
                LIMIT ?
                """,
                (rs, rowNum) -> new VectorSearchResultResponse(
                        rs.getLong("id_vector_document"),
                        rs.getString("ds_content"),
                        rs.getDouble("nu_distance")
                ),
                vector,
                vector,
                limit
        );
    }

    String toVectorLiteral(List<Double> embedding) {
        return embedding.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private void ensurePgVectorInfrastructure() {
        if (initialized.compareAndSet(false, true)) {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS tb_vector_document (
                        id_vector_document BIGSERIAL PRIMARY KEY,
                        ds_content TEXT NOT NULL,
                        ds_embedding vector(1536) NOT NULL
                    )
                    """);
        }
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private void validateEmbedding(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalArgumentException("Embedding obrigatorio.");
        }
        if (embedding.size() != EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException("Embedding deve ter exatamente " + EMBEDDING_DIMENSION + " dimensoes.");
        }
        if (embedding.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Embedding nao pode conter valores nulos.");
        }
    }
}

