package com.example.springia.controller;

import com.example.springia.dto.ChatMessageRequest;
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

/**
 * Endpoint REST para chat por sessão.
 * <p>{@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.controller.ChatController" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}</p>
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * CRIAR NOVO CHAT
      curl -X POST "http://localhost:8080/chat/chat-message" \
        -H "Content-Type: application/json" \
        -d '{"sessionId":null,"projectId":1,"messageId":null, "message":"Existe alguma listagem de tarefas?","sessionName":"Ordenação de Lista de Tarefas","role":"USER","timestamp":"2026-07-08T10:30:00"}'
     *
     * CONTINUAR CONVERSA EXISTENTE
        curl -X POST "http://localhost:8080/chat/chat-message" \
        -H "Content-Type: application/json" \
        -d '{"sessionId":1,"projectId":1,"messageId":null,"message":"A Listagem de tarefas deverá ser ordenada por prioridade","sessionName":"Ordenação de Lista de Tarefas","role":"USER","timestamp":"2026-07-08T10:31:00"}'
     * @param request
     * @return
     */
    @PostMapping("/chat-message")
    public List<ChatMessageRequest> chatMessage(@RequestBody ChatMessageRequest request) {
        return chatService.chatMessage(request);
    }

    @GetMapping("/chat-message/{sessionId}")
    public List<ChatMessageRequest> getChatMessage(@PathVariable Long sessionId) {
        return chatService.listBySession(sessionId);
    }

    /**
     *  curl -X POST "http://localhost:8080/chat/create-session" -H "Content-Type: application/json" -d '{"projectId":1,"name":"sessao requisitos","message":"Quero um sistema de tarefas com prioridade e prazo"}'
     * @param request
     * @return
     */
    @PostMapping("/create-session")
    public Message createSession(@RequestBody CreateSessionRequest request) {
        log.info("[CREATE_SESSION] POST /chat/create-session projectId={} name={} tamanhoMensagem={}",request.projectId(), request.name(), request.message() != null ? request.message().length() : 0);
        return chatService.createSession(request);
    }

    /**
     * curl -X POST "http://localhost:8080/chat?sessionId=1" -H "Content-Type: text/plain" --data "Quais requisitos nao funcionais estao faltando?"
     */
    @PostMapping
    public Message chat(@RequestParam Long sessionId,
                        @RequestBody String message) {
        log.info("[CHAT] POST /chat sessionId={} tamanhoMensagem={}", sessionId, message != null ? message.length() : 0);
        return chatService.chat(sessionId, null, message);
    }

    @PostMapping("/aprove")
    public Message aprove(@RequestParam Long sessionId) {
        log.info("[APROVE] POST /chat/aprove sessionId={}", sessionId);
        return chatService.aprove(sessionId);
    }

    @PostMapping("/gerar-historia-de-usuario")
    public UserStory gerarHistoriaDeUsuario(@RequestParam Long sessionId) {
        log.info("[APROVE] POST /chat/aprove sessionId={}", sessionId);
        return chatService.gerarHistoriaDeUsuario(sessionId);
    }

    @GetMapping
    public List<Message> getChat(@RequestParam Long sessionId) {
        log.info("[GET_CHAT] GET /chat sessionId={}", sessionId);
        return chatService.list(sessionId);
    }

    @DeleteMapping
    public void delete(@RequestParam Long sessionId) {
        log.info("[DELETE] DELETE /chat sessionId={}", sessionId);
        chatService.delete(sessionId);
    }

    @PostMapping("/specification")
    public SpecificationResponse generateSpecification(@RequestParam Long sessionId) {
        log.info("[GEN_SPEC] POST /chat/specification sessionId={}", sessionId);
        UserStory saved = chatService.createUserStory(sessionId);
        return new SpecificationResponse(saved.getId(), saved.getConversationSession().getId(), saved.getContent(), saved.getGeneratedAt());
    }

    @GetMapping("/specification")
    public ResponseEntity<SpecificationResponse> getLatestSpecification(@RequestParam Long sessionId) {
        log.info("[GET_SPEC] GET /chat/specification sessionId={}", sessionId);
        return chatService.getLatestSpecification(sessionId)
                .map(s -> ResponseEntity.ok(new SpecificationResponse(s.getId(), s.getConversationSession().getId(), s.getContent(), s.getGeneratedAt())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}