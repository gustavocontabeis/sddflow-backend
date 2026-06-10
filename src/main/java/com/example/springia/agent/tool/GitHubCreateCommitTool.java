package com.example.springia.agent.tool;

import com.example.springia.dto.CommitRequest;
import com.example.springia.dto.CommitResponse;
import com.example.springia.service.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Ferramenta para criar ou atualizar um arquivo e gerar um commit no GitHub
 */
@Slf4j
@RequiredArgsConstructor
public class GitHubCreateCommitTool implements Tool {

    private final GitHubService gitHubService;

    @Override
    public String getName() {
        return "github_create_commit";
    }

    @Override
    public String getDescription() {
        return "Cria ou atualiza um arquivo em um repositório GitHub e gera um commit com a mensagem informada";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("owner", "Nome do usuário ou organização no GitHub");
        params.put("repo", "Nome do repositório");
        params.put("branch", "Branch onde o commit será criado (ex: main)");
        params.put("message", "Mensagem do commit");
        params.put("file_path", "Caminho do arquivo no repositório (ex: src/main/java/Foo.java)");
        params.put("file_content", "Conteúdo completo do arquivo");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String owner = params.get("owner");
        String repo = params.get("repo");
        String branch = params.get("branch");
        String message = params.get("message");
        String filePath = params.get("file_path");
        String fileContent = params.get("file_content");

        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("owner é obrigatório");
        if (repo == null || repo.isBlank()) throw new IllegalArgumentException("repo é obrigatório");
        if (branch == null || branch.isBlank()) throw new IllegalArgumentException("branch é obrigatório");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message é obrigatório");
        if (filePath == null || filePath.isBlank()) throw new IllegalArgumentException("file_path é obrigatório");
        if (fileContent == null) throw new IllegalArgumentException("file_content é obrigatório");

        log.info("[TOOL] Criando commit owner={} repo={} branch={} path={}", owner, repo, branch, filePath);

        CommitRequest request = new CommitRequest(owner, repo, branch, message, filePath, fileContent);
        CommitResponse response = gitHubService.createCommit(request);

        return "Commit criado com sucesso!\n" +
               "SHA: %s\n".formatted(response.getSha()) +
               "Mensagem: %s\n".formatted(response.getMessage()) +
               "Autor: %s\n".formatted(response.getAuthor()) +
               "URL: %s".formatted(response.getUrl());
    }
}


