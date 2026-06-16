package com.example.springia.service;

import com.example.springia.model.*;
import com.example.springia.model.enums.MessageRole;
import com.example.springia.repository.ConversationRepository;
import com.example.springia.repository.MessageRepository;
import com.example.springia.repository.ProjectRepository;
import com.example.springia.repository.SpecificationDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ProjectRepository projetctRepository;
    private final ConversationRepository conversationRepository;
    private final ChatClient chatClient;
    private final DiscoveryService discoveryService;

    public MessageService(
            MessageRepository messageRepository,
            ProjectRepository projetctRepository,
            ConversationRepository conversationRepository,
            ChatClient.Builder chatClientBuilder,
            DiscoveryService discoveryService
    ) {
        this.messageRepository = messageRepository;
        this.projetctRepository = projetctRepository;
        this.conversationRepository = conversationRepository;
        this.chatClient = chatClientBuilder.build();
        this.discoveryService = discoveryService;
    }

    @Transactional
    public Message save(Message message) {

        if(message.getConversationSession().getId() == null){
            message.getConversationSession().setCreatedAt(LocalDateTime.now());
            conversationRepository.save(message.getConversationSession());
        }
        message.setTimestamp(LocalDateTime.now());
        messageRepository.save(message);

        List<Message> history = messageRepository.findByConversationSessionIdOrderByTimestampAsc(message.getConversationSession().getId());

        String prompt = buildPrompt(message.getConversationSession(), history, message.getContent());
        log.info("[MESSAGE_SERVICE] - prompt construido: {} ", prompt);

        String response = chatClient.prompt()
                .system("Você é um especialista sênior em engenharia de requisitos.")
                .user(prompt)
                .call()
                .content();

        Message assistant = saveMessage(message.getConversationSession(), MessageRole.ASSISTANT, response);

        return assistant;

    }

    private String buildPrompt(ConversationSession session, List<Message> history, String input) {

        log.info("[MESSAGE_SERVICE] - build prompt -  session {}, Quand Mensagens: {} - input: {}", session.getId(), history.size(), input);
;
        Project project = projetctRepository.findById(session.getProject().getId()).get();
        StringBuilder reposContext = new StringBuilder();
        if (project.getRepos() != null && !project.getRepos().isEmpty()) {
            for (CodeRepo repo : project.getRepos()) {
                reposContext.append("\nREPOSITORIO:\n")
                        .append("Nome: ").append(repo.getName()).append("\n")
                        .append("Tipo: ").append(repo.getType()).append("\n")
                        .append("Diretório: ").append(repo.getPath().replaceAll("\\d+$", "")).append("\n")
                        .append("Branch: ").append(repo.getBranch()).append("\n")
                        .append("Constitution: ").append(repo.getConstitution() != null ? repo.getConstitution() : "[vazio]").append("\n")
                        .append("Structure: ").append(repo.getStructure() != null ? repo.getStructure() : "[vazio]").append("\n");
            }
        }

        String historyText = history.stream()
                .map(m -> " MENSAGEM ID "+m.getId()+" > \n"
                        +m.getRole() + ": " + m.getContent())
                .reduce("", (a, b) -> a + "\n" + b);

        String prompt = """
                Você é um analista de requisitos Sênior.
                Este é um chat com a intenção de refinar uma idéia bruta e transforma-la em uma História de Usuário que define o passo a passo incluindo os arquivos que deverão ser criados e alterados para criar a funcionalidade. 
                Para isso, analise o histórico da conversa e a estrutura atual do projeto 
                - Continue refinando a ideia.
                - Mantenha as regras de negócio definidas nas conversas anteriores.
                - Confirme se não perdeu nenhuma regra de negócio anterior durante as conversas
                - Faça perguntas objetivas.
                - faça no máximo 5 pergundas de cada vez.
                - Coloque apenas uma linha de espaço abaixo de cada pergunta para o usuário poder responder.
                RETORNO: O RETORNO DEVERÁ SER EXATAMENTE NA ESTRUTURA ABAIXO:  
                - Estruture melhor os requisitos em:
                  # Perguntas de refinamento
                  # Especificação Funcional
                  ## Objetivo
                  ## Requisitos Funcionais
                  ### 1. Gerenciamento de Tarefas
                  ### 2. Histórias de Usuário
                  ### 3. Regras de Negócio
                  ### 4. Critérios de Aceite
                  ### 5. Listar como tabela markdown todos os arquivos fonte impactados.
                    colunas:  
                      - Nome do repositório - nome do repositório do projeto em que o arquivo fonte estará
                      - Nome do recurso - Os recursos podem ser classes, métodos, arquivos de back ou front.  
                      - Já existe - Essa coluna diz se o recurso já existe ou será criado   
                      - Ação - Descrever a ação a ser aplicada no arquivo fonte
                =====================
                HISTÓRICO:
                %s
                =====================
                PROJETO:
                %s
                =====================
                ENTRADA:
                %s
                
                """.formatted(
                historyText,
                reposContext,
                input
        );
        return prompt;
    }

    private Message saveMessage(ConversationSession session, MessageRole role, String content) {
        if (session == null || session.getId() == null) {
            throw new IllegalArgumentException("Sessao invalida para salvar mensagem");
        }

        Message msg = new Message();
        msg.setConversationSession(session);
        msg.setRole(role);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        messageRepository.save(msg);
        return msg;
    }

}

