package com.example.springia.controller;

import com.example.springia.dto.AskProjectQuestionRequest;
import com.example.springia.dto.DiscoveryRepoDTO;
import com.example.springia.dto.UpdateUserStoryRequest;
import com.example.springia.model.ImplSdd;
import com.example.springia.model.UserStory;
import com.example.springia.service.DiscoveryService;
import com.example.springia.service.ImplSddService;
import com.example.springia.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    /**
     * Executa a descoberta no repositório informado.
     *
     * @param repositoryPath caminho absoluto do repositório local
     * @return resultado da descoberta
     * Exemplo:
     * {@code curl -X GET "http://localhost:8080/discovery?repositoryPath=/home/user/projeto"}
     */
    //@GetMapping
    public String discovery(@RequestParam Long idRepo, @RequestParam String repositoryPath) {
        log.info("[API] GET /discovery");
        DiscoveryRepoDTO dicovery = discoveryService.dicovery(idRepo, Path.of(repositoryPath));
        return JsonUtils.toJson(dicovery);
    }

    /**
     * Responde uma pergunta sobre um projeto com base na constitution e nos dados dos repositorios.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/discovery/ask \
     *   -H "Content-Type: application/json" \
     *   -d '{"projectId":1,"question":"Quais sao as principais regras de negocio?"}'
     * }</pre>
     */
    @PostMapping("/ask")
    public ResponseEntity<String> askProject(@RequestBody AskProjectQuestionRequest request) {
        if (request == null || request.getProjectId() == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new IllegalArgumentException("projectId e question sao obrigatorios");
        }

        String answer = discoveryService.answerProjectQuestion(request.getProjectId(), request.getQuestion());
        if (answer == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(answer);
    }



}

