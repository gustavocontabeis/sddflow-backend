package com.example.springia.service;

import com.example.springia.model.PlanSdd;
import com.example.springia.model.SpecSdd;
import com.example.springia.model.UserStory;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import com.example.springia.repository.UserStoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SddService {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private SpecSddService specSddService;

    @Autowired
    private PlanSddService planSddService;

    public String createSpec(Long userStoryId) {
        log.info("Iniciando geracao de SPEC para userStoryId={}", userStoryId);

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

        String instruction = "Crie um documento SDD em Markdown com objetivo, escopo, requisitos funcionais, regras de negocio e criterios de aceite.";
        String projectConstitution = userStory.getConversationSession().getProject().getConstitution();

        if (!projectConstitution.isBlank()) {
            log.debug("Aplicando constitution na SPEC para userStoryId={}, constitutionLength={}", userStoryId, projectConstitution.length());
            instruction += "\n\nCONSTITUTION DO PROJETO (siga estritamente estas diretrizes):\n" + projectConstitution;
        } else {
            log.debug("Projeto sem constitution para userStoryId={}", userStoryId);
        }

        String spec = generateFromUserStory(userStory, instruction);

        specSddService.saveSpec(userStory, spec);
        log.info("SPEC gerada e salva com sucesso para userStoryId={}, specLength={}", userStoryId, spec.length());

        return spec;
    }

    public String createPlan(Long userStoryId) {
        log.info("Iniciando geracao de PLAN para userStoryId={}", userStoryId);
        //Busca a constitution do projeto através da userStory
        //Busca o Spec desta User Story
        //Gera o Plan
        //Grava o Plan usando o PlanSddService
        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

        String instruction = "Crie um plano de implementacao em Markdown com fases, dependencias, riscos, mitigacoes e entregas por fase.";
        String projectConstitution = userStory.getConversationSession().getProject().getConstitution();

        if (!projectConstitution.isBlank()) {
            log.debug("Aplicando constitution no PLAN para userStoryId={}, constitutionLength={}", userStoryId, projectConstitution.length());
            instruction += "\n\nCONSTITUTION DO PROJETO (siga estritamente estas diretrizes):\n" + projectConstitution;
        } else {
            log.debug("Projeto sem constitution para userStoryId={}", userStoryId);
        }

        SpecSdd specSdd = specSddService.findById(userStory.getSpec().getId()).orElse(null);
        if (specSdd == null) {
            log.error("Nao foi possivel gerar PLAN: SPEC nao encontrada para userStoryId={}, specId={}", userStoryId, userStory.getSpec().getId());
            throw new IllegalStateException("SPEC nao encontrada para userStoryId: " + userStoryId);
        }
        log.debug("SPEC carregada para PLAN em userStoryId={}, specId={}", userStoryId, userStory.getSpec().getId());
        instruction += "\n\nSPEC DO PROJETO (siga estritamente estas diretrizes):\n" + specSdd.getContent();

        String plan = generateFromUserStory(userStory, instruction);

        PlanSdd save = planSddService.save(PlanSdd.builder().id(null).userStory(userStory).status(SpecificationDocumentStatus.IN_PROGRESS).build());
        log.info("PLAN gerado para userStoryId={}, planLength={}, planSddId={}", userStoryId, plan.length(), save.getId());

        return plan;
    }

    public String createTask(Long userStoryId) {
        log.info("Iniciando geracao de TASK para userStoryId={}", userStoryId);
        return generateFromUserStory(userStoryId, "Crie uma lista de tarefas tecnicas em Markdown, com prioridade, descricao, criterio de pronto e estimativa para cada tarefa.");
    }

    public String createImpl(Long userStoryId) {
        log.info("Iniciando geracao de IMPL para userStoryId={}", userStoryId);
        return generateFromUserStory(userStoryId, "Crie um guia de implementacao em Markdown com arquitetura sugerida, passos de codificacao, validacoes e estrategia de testes.");
    }

    private String generateFromUserStory(Long userStoryId, String instruction) {
        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

        return generateFromUserStory(userStory, instruction);
    }

    private String generateFromUserStory(UserStory userStory, String instruction) {
        log.debug("Montando prompt para userStoryId={}, instructionLength={}", userStory.getId(), instruction.length());

        String prompt = """
                Voce e um arquiteto de software senior.
                Gere apenas o resultado solicitado em Markdown.

                INSTRUCAO:
                %s

                HISTORICO DA CONVERSA:
                %s
                """.formatted(instruction, userStory.getContent());

        String response = chatService.chat(prompt);
        log.debug("Resposta recebida do chatService para userStoryId={}, responseLength={}", userStory.getId(), response.length());
        return response;
    }

}

