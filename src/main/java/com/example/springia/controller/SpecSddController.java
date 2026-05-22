package com.example.springia.controller;

import com.example.springia.dto.UpdateUserStoryRequest;
import com.example.springia.model.SpecSdd;
import com.example.springia.model.UserStory;
import com.example.springia.service.SpecSddService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/spec-sdds")
@RequiredArgsConstructor
public class SpecSddController {

    private final SpecSddService specSddService;

    /**
     * Lista todos os registros de SpecSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/spec-sdds
     * }</pre>
     */
    @GetMapping
    public List<SpecSdd> findAll() {
        log.info("[API] GET /spec-sdds");
        return specSddService.findAll().stream()
                .map(this::sanitize)
                .toList();
    }

    /**
     * Busca um SpecSdd por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/spec-sdds/1
     * }</pre>
     */
    @GetMapping("/{id}")
    public ResponseEntity<SpecSdd> findById(@PathVariable Long id) {
        log.info("[API] GET /spec-sdds/{}", id);
        return specSddService.findById(id)
                .map(this::sanitize)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Salva um SpecSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/spec-sdds \
     *   -H "Content-Type: application/json" \
     *   -d '{"userStory":{"id":1},"content":"Conteudo spec","status":"IN_PROGRESS"}'
     * }</pre>
     */
    @PostMapping
    public ResponseEntity<SpecSdd> save(@RequestBody SpecSdd specSdd) {
        log.info("[API] POST /spec-sdds id={} userStoryId={}",
                specSdd.getId(),
                specSdd.getUserStory() != null ? specSdd.getUserStory().getId() : null);
        SpecSdd saved = specSddService.save(specSdd);
        return ResponseEntity.status(HttpStatus.CREATED).body(sanitize(saved));
    }

    /**
     * Remove um SpecSdd por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/spec-sdds/1
     * }</pre>
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("[API] DELETE /spec-sdds/{}", id);
        return specSddService.findById(id)
                .map(existing -> {
                    specSddService.delete(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private SpecSdd sanitize(SpecSdd specSdd) {
        if (specSdd.getUserStory() != null) {
            UserStory minimalUserStory = new UserStory();
            minimalUserStory.setId(specSdd.getUserStory().getId());
            specSdd.setUserStory(minimalUserStory);
        }
        return specSdd;
    }

    /**
     * Aprova um SpecSdd alterando seu status para APPROVED.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/spec-sdds/1/approve
     * }</pre>
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<SpecSdd> approve(@PathVariable Long id) {
        log.info("[API] PATCH /spec-sdds/{}/approve", id);
        return specSddService.approve(id)
                .map(this::sanitize)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Atualiza o conteudo de um SpecSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/spec-sdds/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{"content": "Novo conteudo da spec"}'
     * }</pre>
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SpecSdd> updateContent(@PathVariable Long id, @RequestBody UpdateUserStoryRequest request) {
        log.info("[API] PATCH /spec-sdds/{} content={}", id, request.getContent() != null ? request.getContent().length() : 0);
        return specSddService.findById(id)
                .map(specSdd -> {
                    specSdd.setContent(request.getContent());
                    SpecSdd updated = specSddService.save(specSdd);
                    return ResponseEntity.ok(sanitize(updated));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}

