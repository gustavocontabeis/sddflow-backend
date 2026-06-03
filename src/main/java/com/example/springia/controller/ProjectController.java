package com.example.springia.controller;

import com.example.springia.dto.ProjectCreateRequest;
import com.example.springia.dto.ProjectResponse;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<Project>> findAll() {
        return ResponseEntity.status(HttpStatus.OK).body(projectService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> findById(@PathVariable Long id) {
        Project project = projectService.findById(id);
        return project != null ? ResponseEntity.ok(project) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@RequestBody ProjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @PutMapping
    public ResponseEntity<ProjectResponse> update(@RequestBody ProjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(projectService.updade(request));
    }

    /**
     * Remove um projeto por ID.
     *
     * <p>Exemplo de execucao:</p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/api/projects/1
     * }</pre>
     */
    //@DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("[API] DELETE /api/projects/{}", id);
        Project project = projectService.findById(id);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Atualiza a estrutura do repositorio e salva em constitution.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/api/projects/update-constitution-structure/1
     * }</pre>
     */
    @PostMapping("/update-constitution-structure/{id}")
    public ResponseEntity<Project> updateStructureInConstitution(@PathVariable Long id) {
        Project project = projectService.updateStructureInConstitution(id);
        if (project != null) {
            return ResponseEntity.ok(project);
        }
        return ResponseEntity.notFound().build();
    }



}

