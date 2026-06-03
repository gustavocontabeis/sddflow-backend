package com.example.springia.controller;

import com.example.springia.dto.UpdateUserStoryRequest;
import com.example.springia.model.TaskSdd;
import com.example.springia.model.UserStory;
import com.example.springia.service.TaskSddService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/task-sdds")
@RequiredArgsConstructor
public class TaskSddController {

    private final TaskSddService taskSddService;

    /**
     * Lista todos os registros de TaskSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/task-sdds
     * }</pre>
     */
    //@GetMapping
    public List<TaskSdd> findAll() {
        log.info("[API] GET /task-sdds");
        return taskSddService.findAll().stream()
                .map(this::sanitize)
                .toList();
    }

    /**
     * Busca um TaskSdd por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/task-sdds/1
     * }</pre>
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskSdd> findById(@PathVariable Long id) {
        log.info("[API] GET /task-sdds/{}", id);
        return taskSddService.findById(id)
                .map(this::sanitize)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Salva um TaskSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/task-sdds \
     *   -H "Content-Type: application/json" \
     *   -d '{"userStory":{"id":1},"content":"Conteudo task","status":"IN_PROGRESS"}'
     * }</pre>
     */
    //@PostMapping
    public ResponseEntity<TaskSdd> save(@RequestBody TaskSdd taskSdd) {
        log.info("[API] POST /task-sdds id={} userStoryId={}",
                taskSdd.getId(),
                taskSdd.getUserStory() != null ? taskSdd.getUserStory().getId() : null);
        TaskSdd saved = taskSddService.save(taskSdd);
        return ResponseEntity.status(HttpStatus.CREATED).body(sanitize(saved));
    }

    /**
     * Remove um TaskSdd por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/task-sdds/1
     * }</pre>
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("[API] DELETE /task-sdds/{}", id);
        return taskSddService.findById(id)
                .map(existing -> {
                    taskSddService.delete(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Aprova um TaskSdd alterando seu status para APPROVED.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/task-sdds/1/approve
     * }</pre>
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<TaskSdd> approve(@PathVariable Long id) {
        log.info("[API] PATCH /task-sdds/{}/approve", id);
        return taskSddService.approve(id)
                .map(this::sanitize)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Atualiza o conteudo de um TaskSdd.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/task-sdds/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{"content": "Novo conteudo da task"}'
     * }</pre>
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TaskSdd> updateContent(@PathVariable Long id, @RequestBody UpdateUserStoryRequest request) {
        log.info("[API] PATCH /task-sdds/{} content={}", id, request.getContent() != null ? request.getContent().length() : 0);
        return taskSddService.findById(id)
                .map(taskSdd -> {
                    taskSdd.setContent(request.getContent());
                    TaskSdd updated = taskSddService.save(taskSdd);
                    return ResponseEntity.ok(sanitize(updated));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private TaskSdd sanitize(TaskSdd taskSdd) {
        if (taskSdd.getUserStory() != null) {
            UserStory minimalUserStory = new UserStory();
            minimalUserStory.setId(taskSdd.getUserStory().getId());
            taskSdd.setUserStory(minimalUserStory);
        }
        return taskSdd;
    }
}

