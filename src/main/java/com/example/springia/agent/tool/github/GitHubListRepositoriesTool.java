package com.example.springia.agent.tool;

import com.example.springia.dto.ListRepositoriesResponse;
import com.example.springia.service.GitHubService;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ferramenta para listar repositórios de um owner no GitHub
 */
@Slf4j
@RequiredArgsConstructor
public class GitHubListRepositoriesTool implements Tool {

    private final GitHubService gitHubService;

    @Override
    public String getName() {
        return "github_list_repositories";
    }

    @Override
    public String getDescription() {
        return "Lista todos os repositórios públicos de um owner (usuário ou organização) no GitHub";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("owner", "Nome do usuário ou organização no GitHub (ex: octocat)");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String owner = params.get("owner");

        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("owner é obrigatório");
        }

        log.info("[TOOL] Listando repositórios do owner={}", owner);

        ListRepositoriesResponse response = gitHubService.listRepositories(owner);

        String repos = response.getRepositories().stream()
                .map(r -> "- %s (%s) | ⭐ %d | 🍴 %d%s".formatted(
                        r.getName(),
                        r.getUrl(),
                        r.getStars(),
                        r.getForks(),
                        r.getDescription() != null && !r.getDescription().isBlank()
                                ? " | " + r.getDescription()
                                : ""))
                .collect(Collectors.joining("\n"));

        return "Total de repositórios: %d\n\n%s".formatted(response.getTotal(), repos);
    }

    @dev.langchain4j.agent.tool.Tool(name = "github_list_repositories", value = "Lista todos os repositórios públicos de um owner no GitHub")
    public String githubListRepositories(
            @P(value = "Nome do usuário ou organização no GitHub") String owner
    ) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("owner", owner == null ? "" : owner);
        return execute(params);
    }
}



