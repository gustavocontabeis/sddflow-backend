package com.example.springia.service;

import com.example.springia.dto.CloneRepositoryRequest;
import com.example.springia.dto.CloneRepositoryResponse;
import com.example.springia.dto.CommitRequest;
import com.example.springia.dto.CommitResponse;
import com.example.springia.dto.ListRepositoriesResponse;
import com.example.springia.dto.PullRequestRequest;
import com.example.springia.dto.PullRequestResponse;
import com.example.springia.dto.RepositoryInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    @Value("${github.token:}")
    private String githubToken;

    private static final String GITHUB_API_URL = "https://api.github.com";

    private RestClient restClient;
    private final ChatService chatService;

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
    private <T> T cast(Object value) {
        return (T) value;
    }

    public ListRepositoriesResponse listRepositories(String owner) {
        log.info("[GITHUB] Listando repositorios owner={}", owner);
        initRestClient();

        List<Map<String, Object>> repos = restClient.get()
                .uri("/users/{owner}/repos", owner)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

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
        log.info("[GITHUB] Criando commit owner={} repo={} branch={} path={}", request.getOwner(), request.getRepo(), request.getBranch(), request.getFilePath());
        initRestClient();

        Map<String, Object> fileInfo = restClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}", request.getOwner(), request.getRepo(), request.getFilePath())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        String sha = fileInfo != null ? String.valueOf(fileInfo.get("sha")) : "";
        String content = java.util.Base64.getEncoder().encodeToString(request.getFileContent().getBytes());

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", request.getMessage());
        payload.put("content", content);
        payload.put("sha", sha);
        payload.put("branch", request.getBranch());

        Map<String, Object> response = restClient.put()
                .uri("/repos/{owner}/{repo}/contents/{path}", request.getOwner(), request.getRepo(), request.getFilePath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        Map<String, Object> commit = cast(response != null ? response.get("commit") : new HashMap<>());
        Map<String, Object> author = cast(commit.getOrDefault("author", new HashMap<>()));

        CommitResponse commitResponse = new CommitResponse(
                String.valueOf(commit.getOrDefault("sha", "")),
                request.getMessage(),
                String.valueOf(author.getOrDefault("name", "")),
                String.valueOf(commit.getOrDefault("html_url", ""))
        );
        log.info("[GITHUB] Commit criado owner={} repo={} sha={}", request.getOwner(), request.getRepo(), commitResponse.getSha());
        return commitResponse;
    }

    public PullRequestResponse createPullRequest(PullRequestRequest request) {
        log.info("[GITHUB] Criando PR owner={} repo={} head={} base={}", request.getOwner(), request.getRepo(), request.getHeadBranch(), request.getBaseBranch());
        initRestClient();

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", request.getTitle());
        payload.put("head", request.getHeadBranch());
        payload.put("base", request.getBaseBranch());
        payload.put("body", request.getDescription());

        Map<String, Object> response = restClient.post()
                .uri("/repos/{owner}/{repo}/pulls", request.getOwner(), request.getRepo())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        PullRequestResponse pullRequestResponse = new PullRequestResponse(
                response != null && response.get("number") instanceof Number n ? n.intValue() : 0,
                String.valueOf(response != null ? response.getOrDefault("title", "") : ""),
                String.valueOf(response != null ? response.getOrDefault("state", "") : ""),
                String.valueOf(response != null ? response.getOrDefault("html_url", "") : "")
        );
        log.info("[GITHUB] PR criada owner={} repo={} numero={}", request.getOwner(), request.getRepo(), pullRequestResponse.getNumber());
        return pullRequestResponse;
    }

    public CloneRepositoryResponse cloneRepository(CloneRepositoryRequest request) {
        log.info("[GITHUB] Iniciando clone owner={} repo={} branch={}", request.getOwner(), request.getRepo(), request.getBranch());
        long start = System.currentTimeMillis();

        try {
            Path tempDir = Files.createTempDirectory(request.getRepo());
            log.debug("[GITHUB] Diretorio temporario criado path={}", tempDir);

            String sufixoNumerico = tempDir.getFileName().toString().replaceAll("\\D+(\\d+)$", "$1");

            String cloneUrl = buildCloneUrl(request.getOwner(), request.getRepo());
            log.debug("[GITHUB] URL de clone montada url={}", cloneUrl.replaceAll("//[^@]+@", "//<token>@"));

            ProcessBuilder pb = buildCloneCommand(cloneUrl, request.getBranch(), tempDir);
            pb.redirectErrorStream(true);

            log.info("[GITHUB] Executando git clone destino={}", tempDir);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("[GITHUB] Falha no git clone exitCode={} output={}", exitCode, output);
                throw new RuntimeException("git clone falhou (exitCode=" + exitCode + "): " + output);
            }

            long durationMs = System.currentTimeMillis() - start;
            log.info("[GITHUB] Clone concluido owner={} repo={} path={} duracaoMs={}", request.getOwner(), request.getRepo(), tempDir, durationMs);

            return new CloneRepositoryResponse(
                    request.getOwner(),
                    request.getRepo(),
                    request.getBranch() != null ? request.getBranch() : "default",
                    tempDir.toAbsolutePath().toString(),
                    durationMs,
                    sufixoNumerico
            );

        } catch (IOException | InterruptedException e) {
            log.error("[GITHUB] Erro ao clonar repositorio owner={} repo={}", request.getOwner(), request.getRepo(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Erro ao clonar repositorio: " + e.getMessage(), e);
        }
    }

    private String buildCloneUrl(String owner, String repo) {
        if (githubToken != null && !githubToken.isEmpty()) {
            return "https://" + githubToken + "@github.com/" + owner + "/" + repo + ".git";
        }
        return "https://github.com/" + owner + "/" + repo + ".git";
    }

    private ProcessBuilder buildCloneCommand(String cloneUrl, String branch, Path destDir) {
        if (branch != null && !branch.isBlank()) {
            return new ProcessBuilder("git", "clone", "--branch", branch, "--depth", "1", cloneUrl, destDir.toString());
        }
        return new ProcessBuilder("git", "clone", "--depth", "1", cloneUrl, destDir.toString());
    }

    public void discovery(CloneRepositoryRequest request) {

        log.info("[GITHUB] Fazendo clone do projeto");

        CloneRepositoryResponse response = cloneRepository(request);

        Path root = Path.of(response.getClonedPath());

        log.info("[GITHUB] clone OK em {}", root.toFile().getAbsolutePath());

        try (var paths = Files.walk(root)) {

            List<String> entries = paths
                    .map(root::relativize)
                    .map(Path::toString)
                    .filter(s -> !s.isBlank() && !s.startsWith(".git")) // remove a raiz vazia
                    .collect(Collectors.toList());

            log.info("[GITHUB] Descoberta concluida path={} totalItens={}", root, entries.size());

            String fileNames = entries.stream()
                    .map(root::resolve)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .sorted()
                    .collect(Collectors.joining(System.lineSeparator()));

            log.info("[GITHUB] Lista de arquivos gerada path={} totalCaracteres={}", root, fileNames.length());
            log.info("[GITHUB] Arquivos:\n{}", fileNames);

            entries.forEach(item -> log.info("[GITHUB] {}", item));

            String prompt = """
            Voce recebeu uma lista de arquivos de um sistema.
            Objetivo:
            Identificar os diretorios de classes de dominio.
            Identificar os diretórios de classes de regra de negócio
            Identificar os diretórios dos endpoints REST
            Responda **EXCLUSIVAMENTE** em JSON valido neste formato
            {
             "domainDirs":[],
             "businessDirs":[],
             "restEndpointsDirs":[],
             "notes":"",
            }
            """ + fileNames;

            String respons = chatService.chat(prompt);

            /*
            Concatenar o conteúdo dos arquivos
            PRMOMPT:
            A partir do conteúdo, faça um discovery da aplicação em Markdown (português-BR), incluindo:
            - Descrição de cada entidade
            - Estrutura dos atributos
            - Relacionamento entre Entidades
            - Diagrama lógico (Mermaid textual)
            - Visão consolidada do domínio do sistema
            - Regras de negócio
             */

            log.info("{}", respons);

        } catch (IOException e) {
            log.error("[GITHUB] Erro ao listar arquivos do clone path={}", root, e);
            throw new RuntimeException("Erro ao listar arquivos do clone: " + e.getMessage(), e);
        }
    }
}
