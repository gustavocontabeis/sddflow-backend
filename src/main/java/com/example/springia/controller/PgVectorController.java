package com.example.springia.controller;

import com.example.springia.dto.VectorDocumentCreateRequest;
import com.example.springia.dto.VectorSearchRequest;
import com.example.springia.dto.VectorSearchResultResponse;
import com.example.springia.service.PgVectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vector-documents")
@RequiredArgsConstructor
public class PgVectorController {

    private final PgVectorService pgVectorService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody VectorDocumentCreateRequest request) {
        Long id = pgVectorService.createDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PostMapping("/search")
    public ResponseEntity<List<VectorSearchResultResponse>> search(@Valid @RequestBody VectorSearchRequest request) {
        return ResponseEntity.ok(pgVectorService.findNearest(request));
    }
}

