package com.example.springia.service;

import com.example.springia.agent.loop.AgentExecution;
import com.example.springia.model.*;
import com.example.springia.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

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
}


