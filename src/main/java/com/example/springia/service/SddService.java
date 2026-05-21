package com.example.springia.service;

import com.example.springia.model.Project;
import com.example.springia.model.UserStory;
import com.example.springia.repository.SpecificationDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SddService {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SpecificationDocumentRepository specificationDocumentRepository;

    @Autowired
    private ProjectService projectService;

    public String createSpec(Long sessionId) {

        Project project = projectService.findById(1L);
        String constitution = project.getConstitution();
        //UserStory
        //Prompt criador de SDD Spec:
        //Crie

        return generateFromSession(sessionId, "Crie um documento SDD em Markdown com objetivo, escopo, requisitos funcionais, requisitos nao funcionais, regras de negocio e criterios de aceite.");
    }

    public String createPlan(Long sessionId) {
        return generateFromSession(sessionId, "Crie um plano de implementacao em Markdown com fases, dependencias, riscos, mitigacoes e entregas por fase.");
    }

    public String createTask(Long sessionId) {
        return generateFromSession(sessionId, "Crie uma lista de tarefas tecnicas em Markdown, com prioridade, descricao, criterio de pronto e estimativa para cada tarefa.");
    }

    public String createImpl(Long sessionId) {
        return generateFromSession(sessionId, "Crie um guia de implementacao em Markdown com arquitetura sugerida, passos de codificacao, validacoes e estrategia de testes.");
    }

    private String generateFromSession(Long sessionId, String instruction) {

        List<UserStory> specifications = specificationDocumentRepository.findByConversationSessionId(sessionId);

        if (specifications.isEmpty()) {
            throw new IllegalArgumentException("Nao ha mensagens para a sessao: " + sessionId);
        }

        UserStory specificationDocument = specifications.get(0);

        String prompt = """
                Voce e um arquiteto de software senior.
                Gere apenas o resultado solicitado em Markdown.

                INSTRUCAO:
                %s

                HISTORICO DA CONVERSA:
                %s
                """.formatted(instruction, specificationDocument.getContent());

        return chatService.chat(prompt);
    }
}

