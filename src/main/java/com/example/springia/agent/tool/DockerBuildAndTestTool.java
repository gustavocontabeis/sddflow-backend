package com.example.springia.agent.tool;

import com.example.springia.agent.responseapi.request.RequestToolDefinition;
import com.example.springia.agent.responseapi.request.RequestToolParameters;
import com.example.springia.agent.responseapi.request.RequestToolProperty;
import com.example.springia.dto.DockerBuildAndTestToolRepo;
import com.example.springia.dto.DockerBuildAndTestToolReturn;
import com.example.springia.dto.ProcessBuilderReturnDTO;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.repository.ProjectRepository;
import com.example.springia.utils.ProcessBuilderUtils;
import dev.langchain4j.agent.tool.P;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ferramenta para validar build de repositórios usando Docker.
 *
 * Esta é uma ferramenta essencial do gate de finalização que valida
 * o código gerado pelo agente antes de aceitar a finalização.
 *
 * Fluxo para cada repositório:
 * 1. Determina o tipo (BACKEND ou FRONTEND)
 * 2. Executa apenas o build Docker (sem subir container)
 * 3. Captura logs
 * 4. Valida sucesso/falha
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.DockerBuildAndTestTool" -H "Content-Type: application/json" -d '{"configuredLevel":"TRACE"}'}
 */
@Slf4j
@Component
public class DockerBuildAndTestTool implements Tool {

    private Project project;

    @Autowired
    private ProjectRepository projectRepository;

    private static final int MAX_LOG_LENGTH = 2000;

    public static RequestToolDefinition createTool(){
        return RequestToolDefinition.builder()
                .type("function")
                .name("docker_build_and_test")
                .description("Compila e testa TODOS os repositórios do projeto usando Docker quando houver Dockerfile. " +
                        "Para BACKEND e FRONTEND, usa o comando de compilação do repositório, com fallback por tipo. " +
                        "CRÍTICO: Esta ferramenta deve ser usada ANTES de finalizar (Finalizar:) para validar o código gerado.")
                .parameters(RequestToolParameters.builder()
                        .type("object")
                        .properties(Map.of(
                                "validate_all_repos", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Se 'true', valida todos os repositórios. Se 'false', executa validação rápida no primeiro repo.")
                                        .build(),
                                "id_projeto", RequestToolProperty.builder()
                                        .type("string")
                                        .description("Numero do ID do projeto ('id_projeto' no system prompt).")
                                        .build()
                        ))
                        .required(List.of("validate_all_repos", "id_projeto"))
                        .additionalProperties(false)
                        .build())
                .strict(true)
                .build();
    }

    @Override
    public String getName() {
        log.info("[GET_NAME] Retornando nome da ferramenta");
        return "docker_build_and_test";
    }

    @Override
    public String getDescription() {
        log.info("[GET_DESCRIPTION] Retornando descrição da ferramenta");
        return "Compila e testa TODOS os repositórios do projeto usando Docker quando houver Dockerfile. " +
                "Para BACKEND e FRONTEND, usa o comando de compilação do repositório, com fallback por tipo. " +
                "CRÍTICO: Esta ferramenta deve ser usada ANTES de finalizar (Finalizar:) para validar o código gerado.";
    }

    @Override
    public Map<String, String> getParameters() {
        log.info("[GET_PARAMETERS] Montando parâmetros da ferramenta");
        Map<String, String> params = new HashMap<>();
        params.put("validate_all_repos", "Se 'true', valida todos os repositórios. Se 'false', executa validação rápida no primeiro repo");
        return params;
    }

    public DockerBuildAndTestToolReturn execute2(Map<String, String> params) throws Exception {
        log.info("[EXECUTE] Iniciando execução com parâmetros: {}", params);

        DockerBuildAndTestToolReturn ret = new DockerBuildAndTestToolReturn();
        ret.setBuilds(new ArrayList<>());

        if(StringUtils.isNotBlank(params.get("id_projeto")) && projectRepository != null){
            Long idProjeto = Long.parseLong(params.get("id_projeto"));
            project = projectRepository.findById(idProjeto).orElse(null);
            log.debug("[EXECUTE] Projeto carregado: {}", project != null ? project.getName() : "<não encontrado>");
        }

        if(project == null) {
            log.error("[EXECUTE] Projeto não foi definido. Use projectRepository ou passe via construtor");
            throw new IllegalStateException("Projeto não foi definido. Forneça projectRepository ou passe projeto via construtor");
        }

        List<CodeRepo> repos = project.getRepos();
        StringBuilder results = new StringBuilder();
        results.append("Iniciando validação (build/test) de ").append(repos.size()).append(" repositório(s):\n");

        boolean allSuccess = true;
        int successCount = 0;
        int failureCount = 0;

        for (CodeRepo repo : repos) {
            log.trace("[EXECUTE] Iterando repositório: {}", repo.getName());
            results.append("\n--- Repositório: ").append(repo.getName()).append(" (Tipo: ").append(repo.getType()).append(") ---\n");

            try {
                String buildResult = "";
                DockerBuildAndTestToolRepo retRepo = buildAndTestRepository(repo);
                ret.getBuilds().add(retRepo);
                results.append(buildResult).append("\n");

                if (!retRepo.isSuccess()) {
                    failureCount++;
                    allSuccess = false;
                    results.append("  Status: ❌ FALHOU\n");
                    log.trace("[EXECUTE]   Status: ❌ FALHOU");
                } else {
                    successCount++;
                    results.append("  Status: ✅ PASSOU\n");
                    log.trace("[EXECUTE]   Status: ✅ PASSOU");
                }
            } catch (Exception e) {
                failureCount++;
                allSuccess = false;
                results.append("ERRO ao processar repositório: ").append(e.getMessage()).append("\n");
                log.error("[EXECUTE] Erro ao validar repo {}", repo.getName(), e);
            }
        }

        // Resumo final
        results.append("\n=== RESUMO DA VALIDAÇÃO ===\n");
        results.append("Total: ").append(repos.size()).append(" | ✅ Sucesso: ").append(successCount)
               .append(" | ❌ Falha: ").append(failureCount).append("\n");

        if (allSuccess) {
            results.append("\n✅ VALIDAÇÃO COMPLETA: Todos os repositórios foram compilados e testados com sucesso!");
            results.append("\nO código está pronto para ser aceito em produção.");
        } else {
            results.append("\n❌ VALIDAÇÃO FALHOU: Um ou mais repositórios apresentaram problemas.");
            results.append("\nCorreja os erros acima antes de finalizar.");
        }

        return ret;
    }

    public String execute(Map<String, String> params) throws Exception {
        DockerBuildAndTestToolReturn dockerBuildAndTestToolReturn = execute2(params);
        if(dockerBuildAndTestToolReturn.isAllSuccess()){
            return "Passou!";
        }
        return dockerBuildAndTestToolReturn.toString();
    }

    /**
     * Compila e testa um repositório específico usando Docker
     */
    private DockerBuildAndTestToolRepo buildAndTestRepository(CodeRepo repo) {
        log.debug("[BUILD_TEST_REPO] Iniciando validação para repo: {} (tipo: {})", repo.getName(), repo.getType());

        DockerBuildAndTestToolRepo ret = new DockerBuildAndTestToolRepo();
        ret.setRepoName(repo.getName());
        StringBuilder output = new StringBuilder();
        output.append("Validando repo: ").append(repo.getName()).append("\n");

        File repoDir = new File(repo.getPath());
        if (!repoDir.isDirectory()) {
            throw new RuntimeException("ERRO: Caminho do repositório inválido ou inexistente: " + repo.getPath());
        }

        String buildCommand = resolveBuildCommand(repo);
        if (StringUtils.isNotBlank(buildCommand)) {
            String[] command = {"bash", "-c", buildCommand};
            ProcessBuilderReturnDTO execute = ProcessBuilderUtils.execute(repo.getPath(), command);
            if(execute.isOk()){
                ret.setSuccess(true);
                ret.setLog(execute.getOutput());
                return ret;
            }else{
                ret.setSuccess(false);
                ret.setLogError(execute.getOutput());
                return ret;
            }
        }

        output.append("Comando: ").append(buildCommand).append("\n");

        try {
            boolean hasDockerfile = new File(repoDir, "Dockerfile").isFile();
            if (hasDockerfile && isDockerAvailable() && false) {
                ProcessExecutionResult dockerResult = buildAndTestWithDocker(repoDir, repo.getName());
                if (dockerResult.exitCode == 0) {
                    output.append("✓ Docker Build SUCESSO (exit code: 0)\n");
                    log.trace("[BUILD_TEST_REPO] ✓ Docker Build SUCESSO (exit code: 0)");
                    appendTailLines(output, dockerResult.output, 5);
                    ret.setSuccess(true);
                    ret.setLog(output.toString());
                    return ret;
                }

                output.append("ERRO: Build falhou (exit code: ").append(dockerResult.exitCode).append(")\n");
                log.trace("[BUILD_TEST_REPO]  ERRO: Build falhou (exit code: {}):", dockerResult.exitCode);
                output.append("Log do erro:\n");
                appendTrimmedLog(output, dockerResult.output);
                log.trace("[BUILD_TEST_REPO]  Log do erro: {}:", dockerResult.output);
                ret.setSuccess(false);
                ret.setLogError(output.toString());
                return ret;
            }

            if (hasDockerfile) {
                output.append("AVISO: Dockerfile encontrado, mas Docker indisponível. Executando fallback local.\n");
            }

            ProcessExecutionResult localResult = runLocalCommand(repoDir, buildCommand);
            if (localResult.exitCode == 0) {
                output.append("✓ Build + Test SUCESSO (exit code: 0)\n");
                log.trace("[BUILD_TEST_REPO]  ✓ Build + Test SUCESSO (exit code: 0)");
                appendTailLines(output, localResult.output, 5);
                ret.setSuccess(true);
                ret.setLog(output.toString());
                return ret;
            }

            output.append("ERRO: Build falhou (exit code: ").append(localResult.exitCode).append(")\n");
            output.append("Log do erro:\n");

            log.trace("[BUILD_TEST_REPO]  ERRO: Build falhou (exit code: {}):", localResult.exitCode);
            log.trace("[BUILD_TEST_REPO]  Log do erro: {}:", localResult.output);

            appendTrimmedLog(output, localResult.output);
            //ret.setSuccess(true);
            //ret.setLog(output.toString());
            return ret;

        } catch (Exception e) {
            output.append("ERRO ao executar build: ").append(e.getMessage()).append("\n");
            log.error("[BUILD_TEST_REPO] ERRO ao executar build: {}", e.getMessage());
            ret.setSuccess(false);
            ret.setLogError(output.toString());
            return ret;
        }
    }

    /**
     * Determina o comando de build/test baseado no tipo de repositório
     */
    private String resolveBuildCommand(CodeRepo repo) {
        log.debug("[RESOLVE_BUILD_CMD] Resolvendo comando para repo {} (tipo: {})", repo.getName(), repo.getType());
        if (StringUtils.isNotBlank(repo.getComandoCompilacao())) {
            return repo.getComandoCompilacao();
        }
        return null;
    }

    private ProcessExecutionResult buildAndTestWithDocker(File repoDir, String repoName) throws Exception {
        log.debug("[BUILD_WITH_DOCKER] Executando apenas docker build para repo {}", repoName);
        String imageName = buildImageName(repoName);

        ProcessExecutionResult buildResult = runProcess(
                List.of("docker", "build", "--no-cache", "-t", imageName, "."),
                repoDir
        );

        ProcessExecutionResult cleanupResult = runProcess(
                List.of("docker", "rmi", "-f", imageName),
                repoDir
        );
        if (cleanupResult.exitCode != 0) {
            log.warn("[BUILD_WITH_DOCKER] Falha ao remover imagem {}: {}", imageName, cleanupResult.output);
        }

        return buildResult;
    }

    private ProcessExecutionResult runLocalCommand(File repoDir, String buildCommand) throws Exception {
        log.debug("[RUN_LOCAL_CMD] Executando fallback local em {}", repoDir.getAbsolutePath());
        return runProcess(List.of("bash", "-lc", buildCommand), repoDir);
    }

    private ProcessExecutionResult runProcess(List<String> command, File directory) throws Exception {
        log.debug("[RUN_PROCESS] Executando comando {} no diretório {}", command, directory.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(directory);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder processLog = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.trace("[RUN_PROCESS] Linha de saída capturada: {}", line);
                processLog.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        log.debug("[RUN_PROCESS] Processo finalizado com exit code {}", exitCode);
        return new ProcessExecutionResult(exitCode, processLog.toString());
    }

    private boolean isDockerAvailable() {
        log.debug("[IS_DOCKER_AVAIL] Verificando disponibilidade do Docker");
        try {
            ProcessExecutionResult result = runProcess(List.of("bash", "-lc", "command -v docker >/dev/null 2>&1"), new File("."));
            return result.exitCode == 0;
        } catch (Exception e) {
            log.debug("[IS_DOCKER_AVAIL] Docker indisponível por exceção: {}", e.getMessage());
            return false;
        }
    }

    private String buildImageName(String repoName) {
        log.debug("[BUILD_IMAGE_NAME] Gerando nome da imagem para repo {}", repoName);
        String normalized = repoName == null ? "repo" : repoName.toLowerCase().replaceAll("[^a-z0-9._-]", "-");
        return "springia-build-" + normalized;
    }

    private void appendTrimmedLog(StringBuilder output, String logContent) {
        log.debug("[APPEND_TRIM_LOG] Aplicando corte de log com limite {}", MAX_LOG_LENGTH);
        if (logContent == null || logContent.isBlank()) {
            output.append("<sem saída de log>\n");
            return;
        }

        if (logContent.length() > MAX_LOG_LENGTH) {
            output.append(logContent.substring(logContent.length() - MAX_LOG_LENGTH));
        } else {
            output.append(logContent);
        }
    }

    private void appendTailLines(StringBuilder output, String logContent, int lastLines) {
        log.debug("[APPEND_TAIL_LINES] Extraindo últimas {} linhas", lastLines);
        if (logContent == null || logContent.isBlank()) {
            return;
        }

        String[] logLines = logContent.split("\n");
        int startIdx = Math.max(0, logLines.length - lastLines);
        output.append("Últimas linhas do build:\n");
        for (int i = startIdx; i < logLines.length; i++) {
            log.trace("[APPEND_TAIL_LINES] Iterando linha de índice {}", i);
            if (!logLines[i].isBlank()) {
                output.append("  ").append(logLines[i]).append("\n");
            }
        }
    }

    private static class ProcessExecutionResult {
        private final int exitCode;
        private final String output;

        private ProcessExecutionResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    public void setProjetc(Project projetc){
        log.info("[SET_PROJECT] Atualizando projeto para {}", projetc != null ? projetc.getName() : "<null>");
        this.project = projetc;
    }

    public void setProject(Project project){
        log.info("[SET_PROJECT] Atualizando projeto para {}", project != null ? project.getName() : "<null>");
        this.project = project;
    }

    @dev.langchain4j.agent.tool.Tool(
            name = "docker_build_and_test",
            value = "Compila e testa TODOS os repositórios do projeto usando Docker quando houver Dockerfile"
    )
    public String dockerBuildAndTest(
            @P(value = "Se true valida todos os repositórios; se false executa validação rápida") String validateAllRepos,
            @P(value = "ID numérico do projeto") String idProjeto
    ) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("validate_all_repos", validateAllRepos == null ? "" : validateAllRepos);
        params.put("id_projeto", idProjeto == null ? "" : idProjeto);
        return execute(params);
    }
}


