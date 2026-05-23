package com.example.springia.controller;

import com.example.springia.dto.UpdateUserStoryRequest;
import com.example.springia.model.UserStory;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import com.example.springia.repository.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user-stories")
@RequiredArgsConstructor
public class UserStoryController {

    private final UserStoryRepository userStoryRepository;

    /**
     * Lista todas as user stories ordenadas pela data de geracao descrescente.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/user-stories
     * }</pre>
     */
    @GetMapping
    public List<UserStory> findAll() {
        log.info("[API] GET /user-stories");
        return userStoryRepository.findAllByOrderByGeneratedAtDesc().stream()
                .map(this::sanitize)
                .toList();
    }

    /**
     * Busca uma user story pelo identificador.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/user-stories/1
     * }</pre>
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserStory> findById(@PathVariable Long id) {
        log.info("[API] GET /user-stories/{}", id);
        return userStoryRepository.findById(id)
                .map(this::sanitize)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Busca a user story associada a uma sessao de conversa.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/user-stories/conversation/1
     * }</pre>
     */
    @GetMapping("/conversation/{sessionId}")
    public ResponseEntity<UserStory> findByConversationSessionId(@PathVariable Long sessionId) {
        log.info("[API] GET /user-stories/conversation/{}", sessionId);
        return userStoryRepository.findByConversationSessionId(sessionId)
                .map(this::sanitize)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Lista user stories por status.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/user-stories/status/IN_PROGRESS
     * }</pre>
     */
    @GetMapping("/status/{status}")
    public List<UserStory> findByStatus(@PathVariable SpecificationDocumentStatus status) {
        log.info("[API] GET /user-stories/status/{}", status);
        return userStoryRepository.findByStatus(status).stream()
                .map(this::sanitize)
                .toList();
    }

    /**
     * Atualiza o conteudo de uma user story.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/user-stories/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{"content": "Novo conteudo da user story"}'
     * }</pre>
     */
    @PatchMapping("/{id}")
    public ResponseEntity<UserStory> updateContent(@PathVariable Long id, @RequestBody UpdateUserStoryRequest request) {
        log.info("[API] PATCH /user-stories/{} content={}", id, request.getContent() != null ? request.getContent().length() : 0);
        return userStoryRepository.findById(id)
                .map(userStory -> {
                    userStory.setContent(request.getContent());
                    UserStory updated = userStoryRepository.save(userStory);
                    return ResponseEntity.ok(sanitize(updated));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Remove uma user story por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/user-stories/1
     * }</pre>
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("[API] DELETE /user-stories/{}", id);
        return userStoryRepository.findById(id)
                .map(existing -> {
                    if (existing.getConversationSession() != null) {
                        existing.getConversationSession().setUserStory(null);
                        existing.setConversationSession(null);
                    }
                    userStoryRepository.delete(existing);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private UserStory sanitize(UserStory userStory) {
        if (userStory.getConversationSession() != null) {
            userStory.getConversationSession().setUserStory(null);
            userStory.getConversationSession().setMessages(null);
            userStory.getConversationSession().setProject(null);
        }

        if (userStory.getSpec() != null) {
            userStory.getSpec().setUserStory(null);
        }

        if (userStory.getPlan() != null) {
            userStory.getPlan().setUserStory(null);
        }

        if (userStory.getTask() != null) {
            userStory.getTask().setUserStory(null);
        }

        if (userStory.getImpl() != null) {
            userStory.getImpl().setUserStory(null);
        }

        return userStory;
    }
}

