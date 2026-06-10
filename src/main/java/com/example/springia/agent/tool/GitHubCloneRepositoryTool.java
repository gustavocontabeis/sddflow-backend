package com.example.springia.agent.tool;

import com.example.springia.dto.CloneRepositoryRequest;
import com.example.springia.dto.CloneRepositoryResponse;
import com.example.springia.service.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Ferramenta para clonar um repositório do GitHub em diretório temporário
 */
@Slf4j
@RequiredArgsConstructor
public class GitHubCloneRepositoryTool implements Tool {

    private final GitHubService gitHubService;

    @Override
    public String getName() {
        return "github_clone_repository";
    }

    @Override
    public String getDescription() {
        return "Clona um repositório do GitHub em um diretório temporário e retorna o caminho clonado";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("owner", "Nome do usuário ou organização no GitHub (ex: octocat)");
        params.put("repo", "Nome do repositório (ex: hello-world)");
        params.put("branch", "(opcional) Branch a ser clonada. Se omitido, usa a branch padrão");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String owner = params.get("owner");
        String repo = params.get("repo");
        String branch = params.get("branch");

        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("owner é obrigatório");
        }
        if (repo == null || repo.isBlank()) {
            throw new IllegalArgumentException("repo é obrigatório");
        }

        log.info("[TOOL] Clonando owner={} repo={} branch={}", owner, repo, branch);

        CloneRepositoryRequest request = CloneRepositoryRequest.builder()
                .owner(owner)
                .repo(repo)
                .branch(branch)
                .build();

        CloneRepositoryResponse response = gitHubService.cloneRepository(request);

        return "Repositório clonado com sucesso!\n" +
               "Owner: %s\n".formatted(response.getOwner()) +
               "Repo: %s\n".formatted(response.getRepo()) +
               "Branch: %s\n".formatted(response.getBranch()) +
               "Caminho local: %s\n".formatted(response.getClonedPath()) +
               "Duração: %dms".formatted(response.getDurationMs());
    }
}

