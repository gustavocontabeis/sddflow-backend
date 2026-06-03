package com.example.springia.controller;

import com.example.springia.dto.UpdateUserStoryRequest;
import com.example.springia.model.PlanSdd;
import com.example.springia.model.UserStory;
import com.example.springia.service.PlanSddService;
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
@RequestMapping("/plan-sdds")
@RequiredArgsConstructor
public class PlanSddController {

    private final PlanSddService planSddService;

    /**
     * Lista todos os registros de PlanSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/plan-sdds
     * }</pre>
     */
    //@GetMapping
    public List<PlanSdd> findAll() {
        log.info("[API] GET /plan-sdds");
        return planSddService.findAll().stream()
                .map(this::sanitize)
                .toList();
    }

    /**
     * Busca um PlanSdd por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/plan-sdds/1
     * }</pre>
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlanSdd> findById(@PathVariable Long id) {
        log.info("[API] GET /plan-sdds/{}", id);
        return planSddService.findById(id)
                .map(this::sanitize)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Salva um PlanSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/plan-sdds \
     *   -H "Content-Type: application/json" \
     *   -d '{"userStory":{"id":1},"content":"Conteudo plan","status":"IN_PROGRESS"}'
     * }</pre>
     */
    //@PostMapping
    public ResponseEntity<PlanSdd> save(@RequestBody PlanSdd planSdd) {
        log.info("[API] POST /plan-sdds id={} userStoryId={}",
                planSdd.getId(),
                planSdd.getUserStory() != null ? planSdd.getUserStory().getId() : null);
        PlanSdd saved = planSddService.save(planSdd);
        return ResponseEntity.status(HttpStatus.CREATED).body(sanitize(saved));
    }

    /**
     * Remove um PlanSdd por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/plan-sdds/1
     * }</pre>
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("[API] DELETE /plan-sdds/{}", id);
        return planSddService.findById(id)
                .map(existing -> {
                    planSddService.delete(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Aprova um PlanSdd alterando seu status para APPROVED.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/plan-sdds/1/approve
     * }</pre>
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<PlanSdd> approve(@PathVariable Long id) {
        log.info("[API] PATCH /plan-sdds/{}/approve", id);
        return planSddService.approve(id)
                .map(this::sanitize)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Atualiza o conteudo de um PlanSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/plan-sdds/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{"content": "Novo conteudo da plan"}'
     * }</pre>
     */
    @PatchMapping("/{id}")
    public ResponseEntity<PlanSdd> updateContent(@PathVariable Long id, @RequestBody UpdateUserStoryRequest request) {
        log.info("[API] PATCH /plan-sdds/{} content={}", id, request.getContent() != null ? request.getContent().length() : 0);
        return planSddService.findById(id)
                .map(planSdd -> {
                    planSdd.setContent(request.getContent());
                    PlanSdd updated = planSddService.save(planSdd);
                    return ResponseEntity.ok(sanitize(updated));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private PlanSdd sanitize(PlanSdd planSdd) {
        if (planSdd.getUserStory() != null) {
            UserStory minimalUserStory = new UserStory();
            minimalUserStory.setId(planSdd.getUserStory().getId());
            planSdd.setUserStory(minimalUserStory);
        }
        return planSdd;
    }
}

