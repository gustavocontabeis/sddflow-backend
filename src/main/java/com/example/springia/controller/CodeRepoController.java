package com.example.springia.controller;

import com.example.springia.dto.UpdateCodeRepoConstitutionsRequest;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.enums.CodeRepoType;
import com.example.springia.service.CodeRepoService;
import com.example.springia.service.DiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/coderepos")
@RequiredArgsConstructor
public class CodeRepoController {

    private final CodeRepoService codeRepoService;

    /**
     * Lista todos os repositórios de código.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/coderepos
     * }</pre>
     */
    //@GetMapping
    public ResponseEntity<List<CodeRepo>> findAll() {
        return ResponseEntity.ok(codeRepoService.findAll());
    }

    /**
     * Busca um repositório de código por ID.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/coderepos/1
     * }</pre>
     */
    @GetMapping("/{id}")
    public ResponseEntity<CodeRepo> findById(@PathVariable Long id) {
        return codeRepoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cria um novo repositório de código.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/api/coderepos \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"repo1","path":"/caminho","branch":"main","type":"GIT","project":{"id":1}}'
     * }</pre>
     */
    //@PostMapping
    public ResponseEntity<CodeRepo> create(@RequestBody CodeRepo codeRepo) {
        CodeRepo saved = codeRepoService.save(codeRepo);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Atualiza completamente um repositório de código existente.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X PUT http://localhost:8080/api/coderepos/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"repo-atualizado","path":"/novo/caminho","branch":"develop","type":"GIT","project":{"id":1}}'
     * }</pre>
     */
    @PutMapping("/{id}")
    public ResponseEntity<CodeRepo> update(@PathVariable Long id, @RequestBody CodeRepo codeRepo) {
        if (!codeRepoService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        codeRepo.setId(id);
        CodeRepo updated = codeRepoService.save(codeRepo);
        return ResponseEntity.ok(updated);
    }

    /**
     * Atualiza parcialmente um repositório de código existente (patch).
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/api/coderepos/update-structure/1 \
     *   -H "Content-Type: application/json" \
     * }</pre>
     */
    @PatchMapping("/update-structure/{id}")
    public ResponseEntity<CodeRepo> updateStructure(@PathVariable Long id) throws IOException {
        CodeRepo codeRepo = codeRepoService.updateStructure(id);
        if(codeRepo != null){
            return ResponseEntity.ok(codeRepo);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Atualiza apenas os atributos constitution e structure de um repositório.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X PATCH http://localhost:8080/api/coderepos/update-constituctions/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{"constitution":"nova-constituicao","structure":"nova-estrutura"}'
     * }</pre>
     *
     * @param id ID do repositório de código
     * @param request payload com os campos constitution e structure
     * @return Repositório atualizado
     */
    @PatchMapping("/update-constituctions/{id}")
    public ResponseEntity<CodeRepo> updateConstitutions(
            @PathVariable Long id,
            @RequestBody UpdateCodeRepoConstitutionsRequest request
    ) {
        CodeRepo codeRepo = codeRepoService.updateConstitutions(id, request);
        if(codeRepo != null){
            return ResponseEntity.ok(codeRepo);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Remove um repositório de código por ID.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/api/coderepos/1
     * }</pre>
     */
    //@DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!codeRepoService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        codeRepoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lista repositórios de código por ID de projeto.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/coderepos/project/1
     * }</pre>
     */
    //@GetMapping("/project/{projectId}")
    public ResponseEntity<List<CodeRepo>> findByProjectId(@PathVariable Long projectId) {
        return ResponseEntity.ok(codeRepoService.findByProjectId(projectId));
    }

    /**
     * Lista repositórios de código por ID de projeto e tipo.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/coderepos/project/1/type/GIT
     * }</pre>
     */
    //@GetMapping("/project/{projectId}/type/{type}")
    public ResponseEntity<List<CodeRepo>> findByProjectIdAndType(@PathVariable Long projectId, @PathVariable CodeRepoType type) {
        return ResponseEntity.ok(codeRepoService.findByProjectIdAndType(projectId, type));
    }

    /**
     * Busca repositório de código por ID de projeto e nome.
     *
     * <p>Exemplo de uso:</p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/coderepos/project/1/name/repo1
     * }</pre>
     */
    //@GetMapping("/project/{projectId}/name/{name}")
    public ResponseEntity<CodeRepo> findByProjectIdAndName(@PathVariable Long projectId, @PathVariable String name) {
        return codeRepoService.findByProjectIdAndName(projectId, name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}

