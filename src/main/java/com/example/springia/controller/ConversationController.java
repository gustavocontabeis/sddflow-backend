package com.example.springia.controller;

import com.example.springia.dto.ConversationSummaryResponse;
import com.example.springia.dto.MessageResponse;
import com.example.springia.model.ConversationSession;
import com.example.springia.model.Message;
import com.example.springia.repository.ConversationRepository;
import com.example.springia.repository.MessageRepository;
import com.example.springia.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageRepository messageRepository;

    //@GetMapping
    public List<ConversationSession> listAll() {
        List<ConversationSession> allByOrderByCreatedAtDesc = conversationService.findAllByOrderByCreatedAtDesc();
        allByOrderByCreatedAtDesc.stream().forEach(a->a.getUserStory().setConversationSession(null));
        return allByOrderByCreatedAtDesc;
    }

    //@GetMapping("/{id}")
    public ResponseEntity<ConversationSession> getById(@PathVariable Long id) {
        return conversationService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/messages/latest")
    public ResponseEntity<Message> getLatestMessage(@PathVariable Long id) {
        log.info("[API] GET /conversations/{}/messages/latest", id);

        if (!conversationService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        Message message = messageRepository.findByLastMessage(id).orElse(null);
        ConversationSession conversationSession = message.getConversationSession();
        conversationSession.setMessages(null);
        message.getConversationSession().setProject(null);

        return ResponseEntity.ok(message);
    }

    @GetMapping("/summary")
    public List<ConversationSummaryResponse> findAllSummary() {
        log.info("[API] GET /conversations/summary");
        return conversationService.findAllByOrderByCreatedAtDesc().stream()
                .map(c -> new ConversationSummaryResponse(
                        c.getId(),
                        c.getUserStory() != null ? c.getUserStory().getId() : null,
                        c.getUserStory() != null && c.getUserStory().getSpec() != null ? c.getUserStory().getSpec().getId() : null,
                        c.getUserStory() != null && c.getUserStory().getPlan() != null ? c.getUserStory().getPlan().getId() : null,
                        c.getUserStory() != null && c.getUserStory().getTask() != null ? c.getUserStory().getTask().getId() : null,
                        c.getUserStory() != null && c.getUserStory().getImpl() != null ? c.getUserStory().getImpl().getId() : null,
                        c.getProject() != null ? c.getProject().getId() : null,
                        c.getProject() != null ? c.getProject().getSigla() : null,
                        c.getName(),
                        c.getStatus(),
                        c.getUserStory() != null ? c.getUserStory().getStatus() : null,
                        c.getUserStory() != null && c.getUserStory().getSpec() != null ? c.getUserStory().getSpec().getStatus() : null,
                        c.getUserStory() != null && c.getUserStory().getPlan() != null ? c.getUserStory().getPlan().getStatus() : null,
                        c.getUserStory() != null && c.getUserStory().getTask() != null ? c.getUserStory().getTask().getStatus() : null,
                        c.getUserStory() != null && c.getUserStory().getImpl() != null ? c.getUserStory().getImpl().getStatus() : null,
                        c.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }


    /**
     * Deletar conversa por ID (endpoint do trecho enviado)
     * curl -i -X DELETE "http://localhost:8080/conversations/1"
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Transactional(readOnly = false)
    public ResponseEntity<ConversationSession> delete(@PathVariable Long id) {
        conversationService.delete(id);
        return ResponseEntity.status(204).build();
    }

    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversationSession() != null ? message.getConversationSession().getId() : null,
                message.getRole(),
                message.getContent(),
                message.getTimestamp()
        );
    }
}

