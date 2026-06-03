package com.example.springia.controller;

import com.example.springia.model.Prompt;
import com.example.springia.service.PromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    /**
     * Lista todos os prompts.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/prompts
     * }</pre>
     */
    @GetMapping
    public List<Prompt> findAll() {
        log.info("[API] GET /prompts");
        return promptService.findAll();
    }

    /**
     * Busca um prompt por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/prompts/1
     * }</pre>
     */
    @GetMapping("/{id}")
    public ResponseEntity<Prompt> findById(@PathVariable Long id) {
        log.info("[API] GET /prompts/{}", id);
        return promptService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Busca um prompt pela chave.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/prompts/key/sdd.spec
     * }</pre>
     */
    @GetMapping("/key/{key}")
    public ResponseEntity<Prompt> findByKey(@PathVariable String key) {
        log.info("[API] GET /prompts/key/{}", key);
        return promptService.findByKey(key)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Salva um prompt.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     curl -X POST http://localhost:8080/prompts -H "Content-Type: application/json" -d '{"key":"CREATE_USER_STORY","content":"Conteudo do prompt"}'
     curl -X POST http://localhost:8080/prompts -H "Content-Type: application/json" -d '{"key":"CREATE_SSD_SPEC","content":"Conteudo do prompt"}'
     curl -X POST http://localhost:8080/prompts -H "Content-Type: application/json" -d '{"key":"CREATE_SSD_PLAN","content":"Conteudo do prompt"}'
     curl -X POST http://localhost:8080/prompts -H "Content-Type: application/json" -d '{"key":"CREATE_SSD_TASK","content":"Conteudo do prompt"}'
     curl -X POST http://localhost:8080/prompts -H "Content-Type: application/json" -d '{"key":"CREATE_SSD_IMPL","content":"Conteudo do prompt"}'
     * }</pre>
     */
    @PostMapping
    public ResponseEntity<Prompt> save(@Valid @RequestBody Prompt prompt) {
        log.info("[API] POST /prompts id={} key={}", prompt.getId(), prompt.getKey());
        Prompt saved = promptService.save(prompt);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Remove um prompt por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/prompts/1
     * }</pre>
     */
    //@DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("[API] DELETE /prompts/{}", id);
        return promptService.findById(id)
                .map(existing -> {
                    promptService.delete(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Remove um prompt pela chave.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/prompts/key/CREATE_USER_STORY
     * }</pre>
     */
    @DeleteMapping("/key/{key}")
    public ResponseEntity<Void> deleteByKey(@PathVariable String key) {
        log.info("[API] DELETE /prompts/key/{}", key);
        return promptService.findByKey(key)
                .map(existing -> {
                    promptService.deleteByKey(key);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

