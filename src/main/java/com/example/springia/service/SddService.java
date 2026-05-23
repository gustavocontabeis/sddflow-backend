package com.example.springia.service;

import com.example.springia.model.*;
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

    @Autowired
    private TaskSddService taskSddService;

    @Autowired
    private PromptService promptService;

    public String createSpec(Long userStoryId) {
        log.info("Iniciando geracao de SPEC para userStoryId={}", userStoryId);

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

        String prompt = promptService.findByKey("CREATE_SSD_SPEC").orElse(null).getContent();

        String projectConstitution = userStory.getConversationSession().getProject().getConstitution();

        prompt = prompt
                .replace("{{CONSTITUTION}}", projectConstitution)
                .replace("{{USER_STORY}}", userStory.getContent());

        log.info("SPEC promptLength={}, prompt:={}", prompt.length(), prompt);

        String spec = chatService.chat(prompt);

        specSddService.saveSpec(userStory, spec);

        log.info("SPEC gerada e salva com sucesso para userStoryId={}, specLength={}", userStoryId, spec.length());

        return spec;
    }

    public String createPlan(Long userStoryId) {
        log.info("Iniciando geracao de PLAN para userStoryId={}", userStoryId);

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

        String prompt = promptService.findByKey("CREATE_SSD_PLAN").orElse(null).getContent();

        ConversationSession conversationSession = userStory.getConversationSession();
        String projectConstitution = conversationSession.getProject().getConstitution();

        prompt = prompt
                .replace("{{CONSTITUTION}}", projectConstitution)
                .replace("{{USER_STORY}}", userStory.getContent())
                .replace("{{SDD_SPEC}}", userStory.getSpec().getContent());

        log.info("PLAN promptLength={}, prompt:={}", prompt.length(), prompt);

        String plan = chatService.chat(prompt);

        planSddService.save(PlanSdd.builder().status(SpecificationDocumentStatus.IN_PROGRESS).userStory(userStory).content(plan).build());

        log.info("PLAN gerada e salva com sucesso para userStoryId={}, specLength={}", userStoryId, plan.length());

        return plan;
    }

    public String createTask(Long userStoryId) {
        log.info("Iniciando geracao de TASK para userStoryId={}", userStoryId);

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

        String prompt = promptService.findByKey("CREATE_SSD_TASK").orElse(null).getContent();

        ConversationSession conversationSession = userStory.getConversationSession();
        String projectConstitution = conversationSession.getProject().getConstitution();

        prompt = prompt
                .replace("{{CONSTITUTION}}", projectConstitution)
                .replace("{{SDD_SPEC}}", userStory.getSpec().getContent())
                .replace("{{SDD_PLAN}}", userStory.getPlan().getContent());

        log.info("TASK promptLength={}, promptLenght:={}", prompt.split(" ").length, prompt);

        String content = chatService.chat(prompt);

        taskSddService.save(TaskSdd.builder().status(SpecificationDocumentStatus.IN_PROGRESS).userStory(userStory).content(content).build());

        log.info("TASK gerada e salva com sucesso para userStoryId={}, tokenLength={}", userStoryId, content.split(" ").length);

        return content;
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

