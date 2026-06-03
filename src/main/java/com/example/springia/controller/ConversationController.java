package com.example.springia.controller;

import com.example.springia.dto.ConversationSummaryResponse;
import com.example.springia.dto.MessageResponse;
import com.example.springia.model.ConversationSession;
import com.example.springia.model.Message;
import com.example.springia.repository.ConversationRepository;
import com.example.springia.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    //@GetMapping
    public List<ConversationSession> listAll() {
        List<ConversationSession> allByOrderByCreatedAtDesc = conversationRepository.findAllByOrderByCreatedAtDesc();
        allByOrderByCreatedAtDesc.stream().forEach(a->a.getUserStory().setConversationSession(null));
        return allByOrderByCreatedAtDesc;
    }

    //@GetMapping("/{id}")
    public ResponseEntity<ConversationSession> getById(@PathVariable Long id) {
        return conversationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/messages/latest")
    public ResponseEntity<Message> getLatestMessage(@PathVariable Long id) {
        log.info("[API] GET /conversations/{}/messages/latest", id);

        if (!conversationRepository.existsById(id)) {
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
        return conversationRepository.findAllByOrderByCreatedAtDesc().stream()
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

