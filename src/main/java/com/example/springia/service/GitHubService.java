package com.example.springia.service;

import com.example.springia.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    @Value("${github.token:}")
    private String githubToken;

    private static final String GITHUB_API_URL = "https://api.github.com";

    private RestClient restClient;

    private void initRestClient() {
        if (restClient == null) {
            log.info("[GITHUB] Inicializando RestClient");
            var builder = RestClient.builder().baseUrl(GITHUB_API_URL);
            if (githubToken != null && !githubToken.isEmpty()) {
                builder.defaultHeader("Authorization", "Bearer " + githubToken);
                log.debug("[GITHUB] Token configurado para chamadas autenticadas");
            }
            builder.defaultHeader("Accept", "application/vnd.github.v3+json");
            this.restClient = builder.build();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T get(Object value) {
        return (T) value;
    }

    public ListRepositoriesResponse listRepositories(String owner) {
        log.info("[GITHUB] Listando repositorios owner={}", owner);
        initRestClient();
        List<Map<String, Object>> repos = restClient.get()
                .uri("/users/{owner}/repos", owner)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        List<RepositoryInfoResponse> result = new ArrayList<>();
        if (repos != null) {
            for (Map<String, Object> repo : repos) {
                result.add(new RepositoryInfoResponse(
                        String.valueOf(repo.getOrDefault("name", "")),
                        String.valueOf(repo.getOrDefault("html_url", "")),
                        repo.get("description") != null ? String.valueOf(repo.get("description")) : "",
                        repo.get("stargazers_count") instanceof Number n ? n.intValue() : 0,
                        repo.get("forks_count") instanceof Number n ? n.intValue() : 0
                ));
            }
        }
        log.info("[GITHUB] Repositorios listados owner={} total={}", owner, result.size());
        return new ListRepositoriesResponse(result, result.size());
    }

    public CommitResponse createCommit(CommitRequest request) {
        log.info("[GITHUB] Criando commit owner={} repo={} branch={} path={}", request.owner(), request.repo(), request.branch(), request.filePath());
        initRestClient();
        Map<String, Object> fileInfo = restClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}", request.owner(), request.repo(), request.filePath())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        String sha = String.valueOf(fileInfo.get("sha"));
        String content = java.util.Base64.getEncoder()
                .encodeToString(request.fileContent().getBytes());
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", request.message());
        payload.put("content", content);
        payload.put("sha", sha);
        payload.put("branch", request.branch());
        Map<String, Object> response = restClient.put()
                .uri("/repos/{owner}/{repo}/contents/{path}", request.owner(), request.repo(), request.filePath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        Map<String, Object> commit = get(response.get("commit"));
        Map<String, Object> author = get(commit.get("author"));
        CommitResponse commitResponse = new CommitResponse(
                String.valueOf(commit.get("sha")),
                request.message(),
                String.valueOf(author.get("name")),
                String.valueOf(commit.get("html_url"))
        );
        log.info("[GITHUB] Commit criado owner={} repo={} sha={}", request.owner(), request.repo(), commitResponse.sha());
        return commitResponse;
    }

    public PullRequestResponse createPullRequest(PullRequestRequest request) {
        log.info("[GITHUB] Criando PR owner={} repo={} head={} base={}", request.owner(), request.repo(), request.headBranch(), request.baseBranch());
        initRestClient();
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", request.title());
        payload.put("head", request.headBranch());
        payload.put("base", request.baseBranch());
        payload.put("body", request.description());
        Map<String, Object> response = restClient.post()
                .uri("/repos/{owner}/{repo}/pulls", request.owner(), request.repo())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        PullRequestResponse pullRequestResponse = new PullRequestResponse(
                response.get("number") instanceof Number n ? n.intValue() : 0,
                String.valueOf(response.get("title")),
                String.valueOf(response.get("state")),
                String.valueOf(response.get("html_url"))
        );
        log.info("[GITHUB] PR criada owner={} repo={} numero={}", request.owner(), request.repo(), pullRequestResponse.number());
        return pullRequestResponse;
    }

}
