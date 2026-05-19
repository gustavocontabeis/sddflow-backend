package com.example.springia.controller;

import com.example.springia.model.Message;
import com.example.springia.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spec")
@RequiredArgsConstructor
public class SpecController {

    private final ChatService chatService;



    /**
     * ler uma História de Usuario
     * Ler a Specification
     * Gerar a spec.md
     * Salvar a Spec.md
     *
     * @param sessionId
     * @param message
     * @return
     */
    @PostMapping
    public String chat(@RequestParam String sessionId,
                       @RequestBody String message) {
        return chatService.chat(sessionId, message);
    }

    /**
     * Cria um chatbot que refina as especificações
     *
     * @param sessionId
     * @return
     */
    @GetMapping
    public List<Message> getchat(@RequestParam String sessionId) {
        return chatService.list(sessionId);
    }

    /**
     * Crie o dodumento de especificação completo
     */
}