package com.example.springia.agent.loop;

import com.example.springia.dto.ProcessBuilderReturnDTO;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.utils.ProcessBuilderUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utilitário para execução de comandos via {@link AgentFinalizationGate}.
 *
 * <p>Exemplo para alterar o nível de log desta classe via Actuator:</p>
 *
 * <pre>
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.loop.AgentFinalizationGate" \
 *   -H "Content-Type: application/json" \
 *   -d '{"configuredLevel":"DEBUG"}'
 * </pre>
 */
@Slf4j
public class AgentFinalizationGate {

    public GateResult validate(Project project) {
        if (project == null || project.getRepos() == null || project.getRepos().isEmpty()) {
            return GateResult.passed("[FINALIZATION GATE] Gate ignorado: projeto sem repositorios configurados para validacao.");
        }

        List<String> detailsRepo = null;
        for (CodeRepo repo : project.getRepos()) {
            detailsRepo = new ArrayList<>();
            if (repo == null || repo.getPath() == null || repo.getPath().isBlank()) {
                detailsAdd(detailsRepo, "[FAIL] Repositorio sem path configurado.");
                return GateResult.failed(joinDetails(detailsRepo));
            }

            Path repoPath = Path.of(repo.getPath()).toAbsolutePath().normalize();
            if (!Files.exists(repoPath.resolve("Dockerfile"))) {
                detailsAdd(detailsRepo, "[FAIL] Repo " + safeRepoName(repo) + ": Dockerfile nao encontrado em " + repoPath + ".");
                return GateResult.failed(joinDetails(detailsRepo));
            }

            String imageTag = buildImageTag(project, repo);

            detailsAdd(detailsRepo, "[INFO] Repo " + safeRepoName(repo) + ": iniciando docker build de validacao com target build.");
            ProcessBuilderReturnDTO build = ProcessBuilderUtils.execute(repoPath.toString(), buildDockerTestCommand(imageTag));
            detailsAdd(detailsRepo, "[BUILD] repo=" + safeRepoName(repo) + " exit=" + build.getExitCode() + "\n" + safeOutput(build.getOutput()));
            if (!build.isOk()) {
//                cleanupImage(repoPath, imageTag, details);
                return GateResult.failed(joinDetails(detailsRepo));
            }

            detailsAdd(detailsRepo, "[INFO] Repo " + safeRepoName(repo) + ": validacao em Docker concluida com sucesso.");
            cleanupImage(repoPath, imageTag, detailsRepo);
        }

        return GateResult.passed(joinDetails(detailsRepo));
    }

    private void detailsAdd(List<String> details, String logStr) {
        log.info("[FINALIZATION GATE] {}", logStr);
        details.add(logStr);
    }

    private String buildImageTag(Project project, CodeRepo repo) {
        String raw = "gate-" + project.getSigla() + "-" + repo.getId() + "-" + repo.getName() + "-" + Instant.now().toEpochMilli();
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "-");
    }

    String[] buildDockerTestCommand(String imageTag) {
        return new String[]{"docker", "build", "--no-cache", "-t", imageTag, "."};
    }

    private void cleanupImage(Path repoPath, String imageTag, List<String> details) {
        ProcessBuilderReturnDTO remove = ProcessBuilderUtils.execute(repoPath.toString(), "docker", "rmi", imageTag);
        if (!remove.isOk()) {
            details.add("[FINALIZATION GATE] [WARN] Falha ao remover imagem temporaria " + imageTag + ":\n" + safeOutput(remove.getOutput()));
        }
    }

    private String safeRepoName(CodeRepo repo) {
        if (repo.getName() == null || repo.getName().isBlank()) {
            return "repo-" + repo.getId();
        }
        return repo.getName();
    }

    private String safeOutput(String output) {
        return output == null ? "" : output;
    }

    private String joinDetails(List<String> details) {
        if (details.isEmpty()) {
            return "Sem detalhes.";
        }
        return String.join("\n\n", details);
    }

    public record GateResult(boolean passed, String report) {
        static GateResult passed(String report) {
            return new GateResult(true, report);
        }

        static GateResult failed(String report) {
            return new GateResult(false, report);
        }
    }
}


