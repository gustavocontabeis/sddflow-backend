package com.example.springia.controller;

import com.example.springia.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubController {
    private final com.example.springia.service.GitHubService githubService;

    /**
     * curl http://localhost:8080/api/github/repos/gustavocontabeis
     * @param owner
     * @return
     * @throws Exception
     */
    @GetMapping("/repos/{owner}")
    public ResponseEntity<ListRepositoriesResponse> listRepositories(@PathVariable String owner) throws Exception {
        return ResponseEntity.ok(githubService.listRepositories(owner));
    }

    /**
     * curl -X POST http://localhost:8080/api/github/commit \
     *   -H 'Content-Type: application/json' \
     *   -d '{"owner":"seu-user","repo":"seu-repo","branch":"main","message":"fix","filePath":"README.md","fileContent":"# novo"}'
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/commit")
    public ResponseEntity<CommitResponse> createCommit(@RequestBody CommitRequest request) throws Exception {
        return ResponseEntity.ok(githubService.createCommit(request));
    }

    /**
     * curl -X POST http://localhost:8080/api/github/pull-request \
     *   -H 'Content-Type: application/json' \
     *   -d '{"owner":"seu-user","repo":"seu-repo","title":"Feature","description":"desc","headBranch":"feature","baseBranch":"main"}'
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/pull-request")
    public ResponseEntity<PullRequestResponse> createPullRequest(@RequestBody PullRequestRequest request) throws Exception {
        return ResponseEntity.ok(githubService.createPullRequest(request));
    }
}
