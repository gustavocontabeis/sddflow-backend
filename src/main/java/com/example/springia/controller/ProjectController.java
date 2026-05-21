package com.example.springia.controller;

import com.example.springia.dto.ProjectCreateRequest;
import com.example.springia.dto.ProjectResponse;
import com.example.springia.model.Project;
import com.example.springia.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}

