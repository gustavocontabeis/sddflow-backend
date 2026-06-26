package com.example.springia.controller;

import com.example.springia.dto.ExecutionRequest;
import com.example.springia.dto.ExecutionResponse;
import com.example.springia.service.AgentExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe o endpoint REST síncrono do agente.
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.controller.ExecutionController" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 */
@Slf4j
@RestController
@RequestMapping("/executor-agent")
@RequiredArgsConstructor
public class ExecutionController {

    private final AgentExecutionService agentExecutionService;

    @PostMapping("/execute")
    public ExecutionResponse execute(@Valid @RequestBody ExecutionRequest request) {
        log.info("{[EXEC_CTRL]} recebendo requisicao de execucao");
        ExecutionResponse execute = agentExecutionService.execute(request);
        log.info("{[EXEC_CTRL]} FINALIZADO");
        return execute;
    }

    @GetMapping("/health/{id}")
    public String health(@PathVariable Long id) {
        log.info("{[EXEC_CTRL]} consulta de health para {}", id);
        return "OK";
    }
}

