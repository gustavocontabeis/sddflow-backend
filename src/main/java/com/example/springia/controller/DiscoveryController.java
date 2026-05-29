package com.example.springia.controller;

import com.example.springia.dto.UpdateUserStoryRequest;
import com.example.springia.model.ImplSdd;
import com.example.springia.model.UserStory;
import com.example.springia.service.DiscoveryService;
import com.example.springia.service.ImplSddService;
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
    @GetMapping
    public String discobery(@RequestParam String repositoryPath) {
        log.info("[API] GET /discovery");
        return discoveryService.dicovery(Path.of(repositoryPath));
    }



}

