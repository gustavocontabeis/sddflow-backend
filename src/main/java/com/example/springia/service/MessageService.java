package com.example.springia.service;

import com.example.springia.model.ConversationSession;
import com.example.springia.model.Message;
import com.example.springia.model.Project;
import com.example.springia.model.UserStory;
import com.example.springia.repository.ConversationRepository;
import com.example.springia.repository.MessageRepository;
import com.example.springia.repository.SpecificationDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ChatClient chatClient;

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            ChatClient.Builder chatClientBuilder
    ) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.chatClient = chatClientBuilder.build();
    }

    @Transactional
    public Message save(Message message) {

        if(message.getConversationSession().getId() == null){
            message.getConversationSession().setCreatedAt(LocalDateTime.now());
            conversationRepository.save(message.getConversationSession());
        }

        messageRepository.save(message);

        List<Message> history = messageRepository.findByConversationSessionIdOrderByTimestampAsc(message.getConversationSession().getId());

        String prompt = buildPrompt(message.getConversationSession(), history, message.getContent());

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        Message assistant = saveMessage(message.getConversationSession(), "ASSISTANT", response);

        return assistant;

    }

    private String buildPrompt(ConversationSession session, List<Message> history, String input) {

        String historyText = history.stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .reduce("", (a, b) -> a + "\n" + b);

        return """
        Você é um especialista sênior em engenharia de requisitos.

        ESTÁGIO: %s

        HISTÓRICO:
        %s

        ENTRADA:
        %s

        AÇÃO:
        - Continue refinando a ideia
        - Faça perguntas objetivas
        - faça no máximo 5 pergundas de cada vez.
        - Estruture melhor os requisitos em:
        ```markdown
          # Perguntas de refinamento
          
            - Coloque apenas uma linha de espaço abaixo de cada pergunta para o usuário poder responder  
          
          # Especificação Funcional
          ## Objetivo
          ## Escopo
          ## Requisitos Funcionais
          ### 1. Gerenciamento de Tarefas
          ### 2. Histórias de Usuário
          ### 3. Regras de Negócio
          ### 4. Critérios de Aceite
        ```
        """.formatted(
                session.getStatus(),
                historyText,
                input
        );
    }

    private Message saveMessage(ConversationSession session, String role, String content) {
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

