package com.example.springia.repository;

import com.example.springia.model.SpecificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecificationDocumentRepository extends JpaRepository<SpecificationDocument, Long> {
    Optional<SpecificationDocument> findTopBySessionIdOrderByGeneratedAtDesc(String sessionId);

    List<SpecificationDocument> findBySessionId(String sessionId);
}

