package com.example.springia.controller;

import com.example.springia.service.SddService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/sdd")
@RequiredArgsConstructor
public class SddController {

    private final SddService sddService;

    /**
     * Cria um documento SDD (Specification Design Document) em Markdown a partir da user story.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/sdd/1/spec
     * }</pre>
     */
     @PostMapping("/{userStoryId}/spec")
     public String createSpec(@PathVariable Long userStoryId) {
        log.info("[API] POST /sdd/{}/spec", userStoryId);
         String spec = sddService.createSpec(userStoryId);
         return spec;
    }

    /**
     * Cria um plano de implementacao em Markdown a partir da user story.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/sdd/1/plan
     * }</pre>
     */
     @PostMapping("/{userStoryId}/plan")
     public String createPlan(@PathVariable Long userStoryId) {
        log.info("[API] POST /sdd/{}/plan", userStoryId);
        return sddService.createPlan(userStoryId);
    }

    /**
     * Cria uma lista de tarefas tecnicas em Markdown a partir da user story.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/sdd/1/task
     * }</pre>
     */
     @PostMapping("/{userStoryId}/task")
     public String createTask(@PathVariable Long userStoryId) {
        log.info("[API] POST /sdd/{}/task", userStoryId);
        return sddService.createTask(userStoryId);
    }

    /**
     * Cria um guia de implementacao em Markdown a partir da user story.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/sdd/1/impl
     * }</pre>
     */
     @PostMapping("/{userStoryId}/impl")
     public String createImpl(@PathVariable Long userStoryId) {
        log.info("[API] POST /sdd/{}/impl", userStoryId);
        return sddService.createImpl(userStoryId);
    }

}