package com.example.springia.agent.tool.github;

import com.example.springia.agent.tool.Tool;
import com.example.springia.dto.CloneRepositoryRequest;
import com.example.springia.service.GitHubService;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Ferramenta para fazer o discovery de um repositório GitHub:
 * clona o projeto e produz um relatório estruturado do domínio via IA.
 */
@Slf4j
@RequiredArgsConstructor
public class GitHubDiscoveryTool implements Tool {

    private final GitHubService gitHubService;

    @Override
    public String getName() {
        return "github_discovery";
    }

    @Override
    public String getDescription() {
        return "Clona um repositório GitHub e realiza o discovery completo da aplicação: " +
               "identifica entidades de domínio, regras de negócio, endpoints REST e gera " +
               "um relatório em Markdown com diagrama Mermaid";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("owner", "Nome do usuário ou organização no GitHub");
        params.put("repo", "Nome do repositório");
        params.put("branch", "(opcional) Branch a ser analisada. Se omitido, usa a branch padrão");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String owner = params.get("owner");
        String repo = params.get("repo");
        String branch = params.get("branch");

        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("owner é obrigatório");
        if (repo == null || repo.isBlank()) throw new IllegalArgumentException("repo é obrigatório");

        log.info("[TOOL] Iniciando discovery owner={} repo={} branch={}", owner, repo, branch);

        CloneRepositoryRequest request = CloneRepositoryRequest.builder()
                .owner(owner)
                .repo(repo)
                .branch(branch)
                .build();

        gitHubService.discovery(request);

        return "Discovery concluído para %s/%s (branch: %s). Verifique os logs para o relatório completo."
                .formatted(owner, repo, branch != null ? branch : "padrão");
    }

    @dev.langchain4j.agent.tool.Tool(name = "github_discovery", value = "Clona um repositório GitHub e realiza o discovery completo da aplicação")
    public String githubDiscovery(
            @P(value = "Nome do usuário ou organização no GitHub") String owner,
            @P(value = "Nome do repositório") String repo,
            @P(value = "Branch a ser analisada (opcional)") String branch
    ) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("owner", owner == null ? "" : owner);
        params.put("repo", repo == null ? "" : repo);
        params.put("branch", branch == null ? "" : branch);
        return execute(params);
    }
}

