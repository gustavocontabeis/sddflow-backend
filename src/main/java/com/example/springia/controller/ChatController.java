package com.example.springia.controller;

import com.example.springia.dto.CreateSessionRequest;
import com.example.springia.dto.SpecificationResponse;
import com.example.springia.model.Message;
import com.example.springia.model.UserStory;
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

    //@PostMapping("/create-session")
    public Message createSession(@RequestBody CreateSessionRequest request) {
        log.info("[API] POST /chat/create-session projectId={} name={} tamanhoMensagem={}",request.projectId(), request.name(), request.message() != null ? request.message().length() : 0);
        return chatService.createSession(request);
    }

    //@PostMapping
    public Message chat(@RequestParam Long sessionId,
                       @RequestBody String message) {
        log.info("[API] POST /chat sessionId={} tamanhoMensagem={}", sessionId, message != null ? message.length() : 0);
        return chatService.chat(sessionId, null, message);
    }

    @PostMapping("/aprove")
    public Message aprove(@RequestParam Long sessionId) {
        log.info("[API] POST /chat/aprove sessionId={} tamanhoMensagem={}", sessionId);
        return chatService.aprove(sessionId);
    }

    //@GetMapping
    public List<Message> getChat(@RequestParam Long sessionId) {
        log.info("[API] GET /chat sessionId={}", sessionId);
        return chatService.list(sessionId);
    }

    //@DeleteMapping
    public void delete(@RequestParam Long sessionId) {
        log.info("[API] DELETE /chat sessionId={}", sessionId);
        chatService.delete(sessionId);
    }

    //@PostMapping("/specification")
    public SpecificationResponse generateSpecification(@RequestParam Long sessionId) {
        log.info("[API] POST /chat/specification sessionId={}", sessionId);
        UserStory saved = chatService.createUserStory(sessionId);
        return new SpecificationResponse(saved.getId(), saved.getConversationSession().getId(), saved.getContent(), saved.getGeneratedAt());
    }

    //@GetMapping("/specification")
    public ResponseEntity<SpecificationResponse> getLatestSpecification(@RequestParam Long sessionId) {
        log.info("[API] GET /chat/specification sessionId={}", sessionId);
        return chatService.getLatestSpecification(sessionId)
                .map(s -> ResponseEntity.ok(new SpecificationResponse(s.getId(), s.getConversationSession().getId(), s.getContent(), s.getGeneratedAt())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}