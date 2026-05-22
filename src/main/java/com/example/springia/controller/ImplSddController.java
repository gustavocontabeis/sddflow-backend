package com.example.springia.controller;

import com.example.springia.dto.UpdateUserStoryRequest;
import com.example.springia.model.ImplSdd;
import com.example.springia.model.UserStory;
import com.example.springia.service.ImplSddService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/impl-sdds")
@RequiredArgsConstructor
public class ImplSddController {

    private final ImplSddService implSddService;

    /**
     * Lista todos os registros de ImplSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/impl-sdds
     * }</pre>
     */
    @GetMapping
    public List<ImplSdd> findAll() {
        log.info("[API] GET /impl-sdds");
        return implSddService.findAll().stream()
                .map(this::sanitize)
                .toList();
    }

    /**
     * Busca um ImplSdd por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/impl-sdds/1
     * }</pre>
     */
    @GetMapping("/{id}")
    public ResponseEntity<ImplSdd> findById(@PathVariable Long id) {
        log.info("[API] GET /impl-sdds/{}", id);
        return implSddService.findById(id)
                .map(this::sanitize)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Salva um ImplSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/impl-sdds \
     *   -H "Content-Type: application/json" \
     *   -d '{"userStory":{"id":1},"content":"Conteudo impl","status":"IN_PROGRESS"}'
     * }</pre>
     */
    @PostMapping
    public ResponseEntity<ImplSdd> save(@RequestBody ImplSdd implSdd) {
        log.info("[API] POST /impl-sdds id={} userStoryId={}",
                implSdd.getId(),
                implSdd.getUserStory() != null ? implSdd.getUserStory().getId() : null);
        ImplSdd saved = implSddService.save(implSdd);
        return ResponseEntity.status(HttpStatus.CREATED).body(sanitize(saved));
    }

    /**
     * Remove um ImplSdd por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/impl-sdds/1
     * }</pre>
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("[API] DELETE /impl-sdds/{}", id);
        return implSddService.findById(id)
                .map(existing -> {
                    implSddService.delete(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Aprova um ImplSdd alterando seu status para APPROVED.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/impl-sdds/1/approve
     * }</pre>
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ImplSdd> approve(@PathVariable Long id) {
        log.info("[API] PATCH /impl-sdds/{}/approve", id);
        return implSddService.approve(id)
                .map(this::sanitize)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Atualiza o conteudo de um ImplSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/impl-sdds/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{"content": "Novo conteudo do impl"}'
     * }</pre>
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ImplSdd> updateContent(@PathVariable Long id, @RequestBody UpdateUserStoryRequest request) {
        log.info("[API] PATCH /impl-sdds/{} content={}", id, request.getContent() != null ? request.getContent().length() : 0);
        return implSddService.findById(id)
                .map(implSdd -> {
                    implSdd.setContent(request.getContent());
                    ImplSdd updated = implSddService.save(implSdd);
                    return ResponseEntity.ok(sanitize(updated));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ImplSdd sanitize(ImplSdd implSdd) {
        if (implSdd.getUserStory() != null) {
            UserStory minimalUserStory = new UserStory();
            minimalUserStory.setId(implSdd.getUserStory().getId());
            implSdd.setUserStory(minimalUserStory);
        }
        return implSdd;
    }
}

