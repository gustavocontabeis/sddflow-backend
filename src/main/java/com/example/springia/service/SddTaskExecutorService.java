package com.example.springia.service;

import com.example.springia.agent.loop.AgentExecution;
import com.example.springia.dto.BuildCodeRepoLogDTO;
import com.example.springia.dto.ProcessBuilderReturnDTO;
import com.example.springia.model.*;
import com.example.springia.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Serviço que integra o ExecutorAgent com o SDD (Spec Driven Development)
 *
 * Permite executar tarefas do Task.md com todo o contexto das camadas SDD:
 * - Spec: Especificação funcional
 * - Plan: Plano de implementação
 * - Task: Tarefas específicas de execução
 * - Impl: Implementação (resultado dos tasks)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SddTaskExecutorService {

    private final ExecutorAgentService executorAgentService;
    private final TaskSddService taskSddService;
    private final UserStoryRepository userStoryRepository;
    private final SpecSddService specSddService;
    private final PlanSddService planSddService;
    private final ImplSddService implSddService;
    private final ChatClient.Builder chatClientBuilder;

    /**
     * Executa uma tarefa do SDD com contexto completo (Spec + Plan + Task)
     *
     * A tarefa será executada com o seguinte contexto:
     * 1. Especificação funcional (do Spec.md)
     * 2. Plano de implementação (do Plan.md)
     * 3. Instruções de tarefa (do Task.md)
     *
     * @param taskSddId ID do TaskSdd a executar
     * @return AgentExecution com detalhes da execução
     */
    public AgentExecution executeTaskWithContext(Long taskSddId) throws Exception {
        log.info("[SDD_TASK_EXECUTOR] Iniciando execução de tarefa SDD id={}", taskSddId);

        // Busca a tarefa
        TaskSdd taskSdd = taskSddService.findById(taskSddId)
                .orElseThrow(() -> new IllegalArgumentException("TaskSdd não encontrada: " + taskSddId));

        return executeTaskWithContext(taskSdd);
    }

    /**
     * Executa uma tarefa do SDD
     */
    public AgentExecution executeTaskWithContext(TaskSdd taskSdd) throws Exception {
        log.info("[SDD_TASK_EXECUTOR] Executando TaskSdd id={}", taskSdd.getId());

        UserStory userStory = taskSdd.getUserStory();
        if (userStory == null) {
            throw new IllegalArgumentException("TaskSdd sem UserStory associada");
        }

        // Monta contexto completo
        String context = buildExecutionContext(userStory, taskSdd);

        log.info("[SDD_TASK_EXECUTOR] Contexto montado: {} bytes", context.length());

        // Executa o agent com o contexto
        AgentExecution execution = executorAgentService.executeTask(context);

        log.info("[SDD_TASK_EXECUTOR] Execução concluída: {} - {} passos",
                execution.getStatus(), execution.getStepCount());

        return execution;
    }

    /**
     * Monta o contexto completo para execução (Spec + Plan + Task)
     */
    private String buildExecutionContext(UserStory userStory, TaskSdd taskSdd) {
        StringBuilder context = new StringBuilder();

        context.append("# CONTEXTO DE EXECUÇÃO DO SDD\n\n");

        // Adiciona informações da UserStory
        context.append("## UserStory\n");
        if (userStory.getContent() != null) {
            context.append("Conteúdo: ").append(userStory.getContent()).append("\n\n");
        }

        // Adiciona a Especificação (Spec)
        Optional<SpecSdd> spec = userStory.getSpec() != null ? Optional.of(userStory.getSpec()) : Optional.empty();
        if (spec.isPresent() && spec.get().getContent() != null) {
            context.append("## ESPECIFICAÇÃO (Spec.md)\n\n");
            context.append(spec.get().getContent()).append("\n\n");
        }

        // Adiciona o Plano (Plan)
        Optional<PlanSdd> plan = userStory.getPlan() != null ? Optional.of(userStory.getPlan()) : Optional.empty();
        if (plan.isPresent() && plan.get().getContent() != null) {
            context.append("## PLANO DE IMPLEMENTAÇÃO (Plan.md)\n\n");
            context.append(plan.get().getContent()).append("\n\n");
        }

        // Adiciona a Tarefa (Task)
        context.append("## TAREFA A EXECUTAR (Task.md)\n\n");
        context.append(taskSdd.getContent()).append("\n\n");

        // Instruções finais
        context.append("""
        # INSTRUÇÕES PARA EXECUÇÃO
        
        1. Analise o contexto acima (Spec, Plan, Task)
        2. Execute as ações descritais na TAREFA
        3. Use as ferramentas disponíveis para:
           - Criar arquivos de código
           - Criar estruturas de diretórios
           - Executar comandos (mvn, gradle, etc)
           - Ler e validar arquivos criados
        4. Termine com "Finalizar: [Resumo do que foi feito]"
        
        Inicie a execução das tarefas agora.
        """);

        return context.toString();
    }

    /**
     * Executa uma tarefa via UserStory ID
     * Busca o TaskSdd associado e o executa
     */
    public AgentExecution executeByUserStory(Long userStoryId) throws Exception {
        log.info("[SDD_TASK_EXECUTOR] Buscando tarefa para UserStory id={}", userStoryId);

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory não encontrada: " + userStoryId));

        TaskSdd taskSdd = userStory.getTask();
        if (taskSdd == null) {
            throw new IllegalArgumentException("UserStory sem TaskSdd associada");
        }

        return executeTaskWithContext(taskSdd);
    }

    /**
     * Executa uma implementação via ImplSdd ID
     * Usa o documento Impl.md como guia principal para criação dos arquivos
     */
    public AgentExecution executeByImpl(Long implId) throws Exception {
        log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] Iniciando execução de implementação id={}", implId);

        ImplSdd implSdd = implSddService.findById(implId)
                .orElseThrow(() -> new IllegalArgumentException("ImplSdd não encontrada: " + implId));

        if (implSdd.getContent() == null || implSdd.getContent().isBlank()) {
            throw new IllegalArgumentException("ImplSdd sem conteúdo para execução");
        }

        UserStory userStory = implSdd.getUserStory();
        if (userStory == null) {
            throw new IllegalArgumentException("ImplSdd sem UserStory associada");
        }

        String context = buildImplExecutionContext(userStory, implSdd);

        log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] Contexto de implementação montado: {} bytes", context.length());

        AgentExecution execution = executorAgentService.executeTask(context);

        List<BuildCodeRepoLogDTO> buildCodeRepoLogDTOS = fixCodeAfterBuildDockerImagesFromRopositories(execution, userStory, context);

        log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] FINALIZADO execução de implementação id={}", implId);

        return execution;
    }

    private List<BuildCodeRepoLogDTO> fixCodeAfterBuildDockerImagesFromRopositories(AgentExecution execution, UserStory userStory, String codigo) throws Exception {

        log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] Execução de implementação concluída: {} - {} passos", execution.getStatus(), execution.getStepCount());

        List<BuildCodeRepoLogDTO> listBuildCodeRepoLogDTO = new ArrayList<>();

        ConversationSession conversationSession = null;
        Project project = null;
        List<CodeRepo> repos = null;

        conversationSession = userStory.getConversationSession();
        project = conversationSession.getProject();
        repos = project.getRepos();

        log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] Projeto: {}-{}/{} - Requisito {} - {} - {}", project.getId(), project.getSigla(), project.getName(), conversationSession.getId(), conversationSession.getName(), repos.size());

        log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] Iniciando pós-validação de build para {} repositório(s)", repos.size());

        for (CodeRepo repo : repos) {

            log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] repositório {}-{} - {} - {}", repo.getId(), repo.getName(), repo.getPath(), repo.getUrl());

            final int maxAttempts = 1;
            int attempt = 0;
            ProcessBuilderReturnDTO executeDockerBuildImage = null;

            do{

                log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] Tentativa {}/{} de build para repositório {}", attempt, maxAttempts, repo.getName());

                executeDockerBuildImage = executeDockerBuildImage(repo.getName());

                if (executeDockerBuildImage.isOk()) {
                    log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] Build validado com sucesso para repositório {} na tentativa {}", repo.getName(), attempt);
                    break;
                }

                log.warn("[SDD_TASK_EXECUTOR_SDD_IMPL] Falha na tentativa {}/{} para repositório {}.", attempt, maxAttempts, repo.getName());
                log.debug("{}", executeDockerBuildImage.getOutput());

                if (!executeDockerBuildImage.isOk()) {
                    log.error("[SDD_TASK_EXECUTOR_SDD_IMPL] Executando a correção baseado no log de erro {}", repo.getName());

                    String retryPrompt = """
                    Você é um arquiteto de software FullStack nível Sênior.
                    O Conteúdo abaixo contem:
                    - CODIGO COM O ERRO gerado pelas especificações SDD-Spec Driven Developement
                    - LOG DE ERRO ao subir o container. 
                    Execute as correções no códido baseado no log de erro.
                    
                    =========== LOG DE ERRO ===========
                    {logErro}
                    =========== CODIGO COM O ERRO ===========
                    {codigoComErro}
                    """;

                    PromptTemplate promptTemplate = new PromptTemplate(retryPrompt);

                    String promptFinal = promptTemplate.create(Map.of(
                            "logErro", executeDockerBuildImage.getOutput(),
                            "codigoComErro", codigo
                    )).getContents();

                    log.debug("[SDD_TASK_EXECUTOR_SDD_IMPL] Prompt do Retry :");
                    log.debug("{}", promptFinal);

                    String codigoCorrigido = chatClientBuilder.build().prompt()
                            .user(promptFinal)
                            .call()
                            .content();

                    log.debug("[SDD_TASK_EXECUTOR_SDD_IMPL] Código corrigido:");
                    log.debug("{}", codigoCorrigido);

                    listBuildCodeRepoLogDTO.add(BuildCodeRepoLogDTO.builder().dodeRepo(repo).ok(executeDockerBuildImage.isOk()).logErro(executeDockerBuildImage.getOutput()).codigoOriginal(codigo).codigoCorrigido(codigoCorrigido).build());

                    codigo = codigoCorrigido;

                    //AgentExecution retryExecution = executorAgentService.executeTask(promptFinal);
                    //log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] Execução de retry : {} - {} passos", retryExecution.getStatus(), execution.getStepCount());
                }

            } while (!executeDockerBuildImage.isOk() && attempt++ < maxAttempts);

        }

        log.info("[SDD_TASK_EXECUTOR_SDD_IMPL] Finalizada a etapa de build das imagens Docker de todos os repositórios");

        return listBuildCodeRepoLogDTO;

    }

    /**
     * Monta contexto para executar o documento de implementação (Impl.md)
     */
    private String buildImplExecutionContext(UserStory userStory, ImplSdd implSdd) {
        StringBuilder context = new StringBuilder();

        context.append("# CONTEXTO DE EXECUÇÃO DO SDD (IMPLEMENTAÇÃO)\n\n");

        if (userStory.getContent() != null) {
            context.append("## USER STORY\n\n");
            context.append(userStory.getContent()).append("\n\n");
        }

        if (userStory.getSpec() != null && userStory.getSpec().getContent() != null) {
            context.append("## ESPECIFICAÇÃO (Spec.md)\n\n");
            context.append(userStory.getSpec().getContent()).append("\n\n");
        }

        if (userStory.getPlan() != null && userStory.getPlan().getContent() != null) {
            context.append("## PLANO (Plan.md)\n\n");
            context.append(userStory.getPlan().getContent()).append("\n\n");
        }

        if (userStory.getTask() != null && userStory.getTask().getContent() != null) {
            context.append("## TAREFAS (Task.md)\n\n");
            context.append(userStory.getTask().getContent()).append("\n\n");
        }

        context.append("## IMPLEMENTAÇÃO A EXECUTAR (Impl.md)\n\n");
        context.append(implSdd.getContent()).append("\n\n");

        context.append("""
        # INSTRUÇÕES PARA EXECUÇÃO

        1. Use a seção "IMPLEMENTAÇÃO A EXECUTAR (Impl.md)" como fonte principal.
        2. Crie os arquivos e diretórios exatamente como descritos.
        3. Quando houver conteúdo de arquivo, grave o conteúdo completo.
        4. Se necessário, consulte Spec/Plan/Task apenas para contexto adicional.
        5. Ao final, valide os arquivos criados e termine com "Finalizar: [Resumo do que foi feito]".

        Inicie a implementação agora.
        """);

        return context.toString();
    }

    /**
     * Simula a execução sem realmente executar (apenas prepara o contexto)
     */
    public String previewExecutionContext(Long taskSddId) {
        log.info("[SDD_TASK_EXECUTOR] Preview do contexto para TaskSdd id={}", taskSddId);

        TaskSdd taskSdd = taskSddService.findById(taskSddId)
                .orElseThrow(() -> new IllegalArgumentException("TaskSdd não encontrada: " + taskSddId));

        UserStory userStory = taskSdd.getUserStory();
        if (userStory == null) {
            throw new IllegalArgumentException("TaskSdd sem UserStory associada");
        }

        return buildExecutionContext(userStory, taskSdd);
    }

    public ProcessBuilderReturnDTO executeDockerBuildImage(String imageName) {

        ProcessBuilderReturnDTO ret = new ProcessBuilderReturnDTO();
        ret.setImageName(imageName);

        try {

            String effectiveCommand = "docker build --no-cache -t "+imageName+" .";
            long startedAt = System.currentTimeMillis();

            log.info("[SDD_TASK_EXECUTOR_DOCKER_IMAGE] Executando comando em /tmp/{}: {}", imageName, effectiveCommand);

            //ret = execute("/tmp/" + imageName, effectiveCommand);

            if(true) {
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("bash", "-lc", effectiveCommand);
                processBuilder.directory(new java.io.File("/tmp/" + imageName));

                // Junta stdout + stderr
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

                StringBuilder output = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info(line); // opcional: ver em tempo real
                        output.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                ret.setExitCode(exitCode);

                log.info("[SDD_TASK_EXECUTOR_DOCKER_IMAGE] Comando finalizado com exitCode={} em {}ms",
                        exitCode, (System.currentTimeMillis() - startedAt));

                if (exitCode != 0) {
                    log.error("[SDD_TASK_EXECUTOR_DOCKER_IMAGE] Erro detectado no comando. Output:\n{}", output);
                    ret.setOutput(output.toString());
                    //return output.toString();
                }
            }

            ProcessBuilderReturnDTO executeRemodeImage = execute("/tmp/"+imageName, "docker", "rmi", imageName);

            if (!executeRemodeImage.isOk()) {
                log.info("[SDD_TASK_EXECUTOR] Log da remoção da imagem docker: {}", executeRemodeImage.getOutput());
            } else {
                log.info("[SDD_TASK_EXECUTOR] Imagem docker removida sem mensagens adicionais.");
            }

        } catch (Exception e) {
            log.error("[SDD_TASK_EXECUTOR] Erro inesperado ao executar comando", e);
            ret.setOutput("Erro inesperado: " + e.getMessage());
            return ret;
        }

        return ret;
    }

    public ProcessBuilderReturnDTO execute(String path, String...command) {

        ProcessBuilderReturnDTO ret = new ProcessBuilderReturnDTO();

        try {

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command);
            processBuilder.directory(new java.io.File(path));

            // Junta stdout + stderr
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line); // opcional: ver em tempo real
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            ret.setExitCode(exitCode);

            log.info("Exit code: " + exitCode);

            if (exitCode != 0) {
                log.info("Erro detectado no build!");
                ret.setOutput(output.toString());
                log.info(ret.getOutput());
                return ret;
            }
        } catch (Exception e) {
            new RuntimeException(e);
        }

        return ret;
    }


    public void xxx(String repoName) {

        ProcessBuilderReturnDTO executeDockerBuildImage = executeDockerBuildImage(repoName);

        if (!executeDockerBuildImage.isOk()) {
            log.error("[SDD_TASK_EXECUTOR_SDD_IMPL] Executando a correção baseado no log de erro {}", repoName);

            String retryPrompt = """
                        Você é um arquiteto de software FullStack nível Sênior.
                        Este é o log de erro ao subir o container. Execute as correções no códido.
                        {logErro}
                        """;

            PromptTemplate promptTemplate = new PromptTemplate(retryPrompt);

            String promptFinal = promptTemplate.create(Map.of(
                    "logErro", executeDockerBuildImage.getOutput()
            )).getContents();

            try {
                AgentExecution retryExecution = executorAgentService.executeTask(promptFinal);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }


    }
}
