package com.example.springia.agent.tool.github;

import com.example.springia.agent.tool.Tool;
import com.example.springia.dto.PullRequestRequest;
import com.example.springia.dto.PullRequestResponse;
import com.example.springia.service.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Ferramenta para abrir um Pull Request no GitHub
 */
@Slf4j
@RequiredArgsConstructor
public class GitHubCreatePullRequestTool implements Tool {

    private final GitHubService gitHubService;

    @Override
    public String getName() {
        return "github_create_pull_request";
    }

    @Override
    public String getDescription() {
        return "Abre um Pull Request em um repositório GitHub, mesclando uma branch de origem em uma branch de destino";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("owner", "Nome do usuário ou organização no GitHub");
        params.put("repo", "Nome do repositório");
        params.put("title", "Título do Pull Request");
        params.put("description", "(opcional) Descrição/corpo do Pull Request");
        params.put("head_branch", "Branch de origem (com as alterações)");
        params.put("base_branch", "Branch de destino (onde o PR será mergeado, ex: main)");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String owner = params.get("owner");
        String repo = params.get("repo");
        String title = params.get("title");
        String description = params.getOrDefault("description", "");
        String headBranch = params.get("head_branch");
        String baseBranch = params.get("base_branch");

        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("owner é obrigatório");
        if (repo == null || repo.isBlank()) throw new IllegalArgumentException("repo é obrigatório");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title é obrigatório");
        if (headBranch == null || headBranch.isBlank()) throw new IllegalArgumentException("head_branch é obrigatório");
        if (baseBranch == null || baseBranch.isBlank()) throw new IllegalArgumentException("base_branch é obrigatório");

        log.info("[TOOL] Criando PR owner={} repo={} head={} base={}", owner, repo, headBranch, baseBranch);

        PullRequestRequest request = new PullRequestRequest(owner, repo, title, description, headBranch, baseBranch);
        PullRequestResponse response = gitHubService.createPullRequest(request);

        return "Pull Request criado com sucesso!\n" +
               "Número: #%d\n".formatted(response.getNumber()) +
               "Título: %s\n".formatted(response.getTitle()) +
               "Estado: %s\n".formatted(response.getState()) +
               "URL: %s".formatted(response.getHtmlUrl());
    }
}

