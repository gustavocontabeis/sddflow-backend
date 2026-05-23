package com.example.springia.service;

import com.example.springia.dto.CreateSessionRequest;
import com.example.springia.model.ConversationSession;
import com.example.springia.model.Message;
import com.example.springia.model.Project;
import com.example.springia.model.UserStory;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import com.example.springia.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final SpecificationDocumentRepository specificationDocumentRepository;
    private final UserStoryRepository userStoryRepository;
    private final PromptRepository promptRepository;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ProjectRepository projectRepository,
            SpecificationDocumentRepository specificationDocumentRepository,
            UserStoryRepository userStoryRepository,
            PromptRepository promptRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.projectRepository = projectRepository;
        this.specificationDocumentRepository = specificationDocumentRepository;
        this.userStoryRepository = userStoryRepository;
        this.promptRepository = promptRepository;
    }

    public Message createSession(CreateSessionRequest request) {
        return chat(null, request.projectId(), request.name(), request.message());
    }

    public Message chat(Long sessionId, Long projectId, String userInput) {
        return chat(sessionId, projectId, null, userInput);
    }

    private Message chat(Long sessionId, Long projectId, String sessionName, String userInput) {
        log.info("[CHAT] Iniciando processamento da sessao={}", sessionId);

        ConversationSession session = resolveSession(sessionId, projectId, sessionName);
        Long effectiveSessionId = session.getId();
        log.debug("[CHAT] Sessao carregada id={} stage={}", effectiveSessionId, session.getStatus());

        saveMessage(session, "USER", userInput);
        log.debug("[CHAT] Mensagem USER salva sessao={} tamanho={}", effectiveSessionId, userInput != null ? userInput.length() : 0);

        List<Message> history = messageRepository.findByConversationSessionIdOrderByTimestampAsc(effectiveSessionId);
        log.debug("[CHAT] Historico carregado sessao={} totalMensagens={}", effectiveSessionId, history.size());

        String prompt = buildPrompt(session, history, userInput);
        log.trace("[CHAT] Prompt montado sessao={} tamanho={}", effectiveSessionId, prompt.length());
        log.info("[CHAT] PROMPT: {}", prompt);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("[CHAT] RESPONSE: {}", prompt);
        Message assistant = saveMessage(session, "ASSISTANT", response);
        log.debug("[CHAT] Mensagem ASSISTANT salva sessao={} tamanho={}", effectiveSessionId, response != null ? response.length() : 0);
        log.info("[CHAT] Processamento finalizado sessao={}", effectiveSessionId);
        assistant.setConversationSession(null);
        return assistant;
    }

    public String chat(String userInput) {
        return chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }

    private ConversationSession resolveSession(Long sessionId, Long projectId, String sessionName) {
        if (sessionId != null) {
            return conversationRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada: " + sessionId));
        }

        return createConversationSession(projectId, sessionName);
    }

    private ConversationSession createConversationSession(Long projectId, String sessionName) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId e obrigatorio para criar uma nova sessao");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projeto nao encontrado: " + projectId));

        log.info("[CHAT] Criando nova sessao projectId={}", projectId);
        ConversationSession session = new ConversationSession();
        //session.setId(null);
        session.setName(sessionName);
        session.setStatus(SpecificationDocumentStatus.IN_PROGRESS);
        session.setCreatedAt(LocalDateTime.now());
        session.setProject(project);

        return conversationRepository.save(session);
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
        - Estruture melhor os requisitos em:
        ```markdown
          # Perguntas de refinamento
          # Especificação Funcional do Sistema de Controle de Tarefas
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

    public List<Message> list(Long sessionId) {
        List<Message> messages = messageRepository.findByConversationSessionId(sessionId);
        log.info("[CHAT] Consulta de historico sessao={} totalMensagens={}", sessionId, messages.size());
        return messages;
    }

    public UserStory createUserStory(Long sessionId) {
        log.info("[SPEC] Geracao iniciada sessao={}", sessionId);
        ConversationSession session = conversationRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada: " + sessionId));

        List<Message> history = messageRepository.findByConversationSessionIdOrderByTimestampAsc(sessionId);
        log.debug("[SPEC] Historico para geracao sessao={} totalMensagens={}", sessionId, history.size());
        if (history.isEmpty()) {
            log.warn("[SPEC] Nao ha mensagens para gerar especificacao sessao={}", sessionId);
            throw new IllegalArgumentException("Nao ha mensagens para gerar especificacao nesta sessao");
        }

        String historyText = history.stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .reduce("", (a, b) -> a + "\n" + b);

        String prompt = """
        Gere uma especificacao funcional completa em portugues, em formato Markdown.
        Use o historico da conversa para montar uma especificacao clara e estruturada.

        Estrutura obrigatoria:
        - Titulo
        - Objetivo
        - Escopo
        - Requisitos funcionais
        - Requisitos nao funcionais
        - Regras de negocio
        - Critérios de aceite

        ESTAGIO: %s
        HISTORICO:
        %s
        """.formatted(session.getStatus(), historyText);

        String generated = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (generated == null || generated.isBlank()) {
            generated = "# Especificacao\n\nNao foi possivel gerar o documento.";
        }

        UserStory specification = new UserStory();
        specification.setConversationSession(session);
        specification.setContent(generated.trim());
        specification.setGeneratedAt(LocalDateTime.now());
        UserStory saved = specificationDocumentRepository.save(specification);
        log.info("[SPEC] Especificacao salva id={} sessao={} tamanho={}", saved.getId(), sessionId, saved.getContent().length());
        return saved;
    }

    public Optional<UserStory> getLatestSpecification(Long sessionId) {
        Optional<UserStory> specification = specificationDocumentRepository.findTopByConversationSessionIdOrderByGeneratedAtDesc(sessionId);
        log.info("[SPEC] Consulta ultima especificacao sessao={} encontrada={}", sessionId, specification.isPresent());
        return specification;
    }

    @Transactional
    public void delete(Long sessionId) {
        log.info("[CHAT] Exclusao iniciada sessao={}", sessionId);
        List<UserStory> specifications = specificationDocumentRepository.findByConversationSessionId(sessionId);
        List<Message> messages = messageRepository.findByConversationSessionId(sessionId);
        specificationDocumentRepository.deleteAll(specifications);
        messageRepository.deleteAll(messages);
        conversationRepository.deleteById(sessionId);
        log.info("[CHAT] Exclusao finalizada sessao={} specsRemovidas={} mensagensRemovidas={}", sessionId, specifications.size(), messages.size());
    }

    public Message aprove(Long sessionId) {

        Message message = messageRepository.findByLastMessage(sessionId).orElse(null);

        ConversationSession conversationSession = message.getConversationSession();
        conversationSession.setStatus(SpecificationDocumentStatus.APPROVED);
        conversationRepository.save(conversationSession);

        String prompt = promptRepository.findByKey("CREATE_USER_STORY").orElse(null).getContent();

        String promptCompleto = prompt.concat(":\n\n\n")
                .concat("---------------------------\n")
                .concat(message.getContent())
                .concat("\n---------------------------\n");

        log.info("[CHAT] prompt CREATE_USER_STORY [{}]: {} ", promptCompleto.length(), promptCompleto);

        String response = chatClient.prompt()
                .user(promptCompleto)
                .call()
                .content();

        log.info("[CHAT] conteudo da User Story: {} ", promptCompleto);

        userStoryRepository.save(UserStory.builder()
                .conversationSession(conversationSession)
                .content(response)
                .status(SpecificationDocumentStatus.APPROVED)
                .generatedAt(LocalDateTime.now())
                .build());
        return message;
    }
}
