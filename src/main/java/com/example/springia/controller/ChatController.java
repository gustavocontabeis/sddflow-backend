package com.example.springia.controller;

import com.example.springia.controller.dto.SpecificationResponse;
import com.example.springia.model.Message;
import com.example.springia.model.SpecificationDocument;
import com.example.springia.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Cria um chatbot que refina as especificações
     * @param sessionId
     * @param message
     * @return
     */
    @PostMapping
    public String chat(@RequestParam String sessionId,
                       @RequestBody String message) {
        log.info("[API] POST /chat sessionId={} tamanhoMensagem={}", sessionId, message != null ? message.length() : 0);
        return chatService.chat(sessionId, message);
    }

    /**
     * Lista a conversa com o chatbot
     * @param sessionId
     * @return
     */
    @GetMapping
    public List<Message> getChat(@RequestParam String sessionId) {
        log.info("[API] GET /chat sessionId={}", sessionId);
        return chatService.list(sessionId);
    }

    /**
     * Exclui a conversa com o chatbot
     * @param sessionId
     * @return
     */
    @DeleteMapping
    public void delete(@RequestParam String sessionId) {
        log.info("[API] DELETE /chat sessionId={}", sessionId);
        chatService.delete(sessionId);
    }

    /**
     * Crie o dodumento de especificação completo
     */
    @PostMapping("/specification")
    public SpecificationResponse generateSpecification(@RequestParam String sessionId) {
        log.info("[API] POST /chat/specification sessionId={}", sessionId);
        SpecificationDocument saved = chatService.generateAndSaveSpecification(sessionId);
        return new SpecificationResponse(saved.getId(), saved.getSessionId(), saved.getContent(), saved.getGeneratedAt());
    }

    @GetMapping("/specification")
    public ResponseEntity<SpecificationResponse> getLatestSpecification(@RequestParam String sessionId) {
        log.info("[API] GET /chat/specification sessionId={}", sessionId);
        return chatService.getLatestSpecification(sessionId)
                .map(s -> ResponseEntity.ok(new SpecificationResponse(s.getId(), s.getSessionId(), s.getContent(), s.getGeneratedAt())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}