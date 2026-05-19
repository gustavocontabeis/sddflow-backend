package com.example.springia.service;

import com.example.springia.model.ConversationSession;
import com.example.springia.model.Message;
import com.example.springia.model.SpecificationDocument;
import com.example.springia.model.enums.Stage;
import com.example.springia.repository.ConversationRepository;
import com.example.springia.repository.MessageRepository;
import com.example.springia.repository.SpecificationDocumentRepository;
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
    private final SpecificationDocumentRepository specificationDocumentRepository;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            SpecificationDocumentRepository specificationDocumentRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.specificationDocumentRepository = specificationDocumentRepository;
    }

    public String chat(String sessionId, String userInput) {
        log.info("[CHAT] Iniciando processamento da sessao={}", sessionId);

        ConversationSession session = conversationRepository.findById(sessionId)
                .orElseGet(() -> createSession(sessionId));
        log.debug("[CHAT] Sessao carregada id={} stage={}", session.getId(), session.getStage());

        saveMessage(sessionId, "USER", userInput);
        log.debug("[CHAT] Mensagem USER salva sessao={} tamanho={}", sessionId, userInput != null ? userInput.length() : 0);

        List<Message> history = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        log.debug("[CHAT] Historico carregado sessao={} totalMensagens={}", sessionId, history.size());

        String prompt = buildPrompt(session, history, userInput);
        log.trace("[CHAT] Prompt montado sessao={} tamanho={}", sessionId, prompt.length());
        log.info("[CHAT] PROMPT: {}", prompt);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("[CHAT] RESPONSE: {}", prompt);
        saveMessage(sessionId, "ASSISTANT", response);
        log.debug("[CHAT] Mensagem ASSISTANT salva sessao={} tamanho={}", sessionId, response != null ? response.length() : 0);
        log.info("[CHAT] Processamento finalizado sessao={}", sessionId);

        return response;
    }

    private ConversationSession createSession(String sessionId) {
        log.info("[CHAT] Criando nova sessao id={}", sessionId);
        ConversationSession session = new ConversationSession();
        session.setId(sessionId);
        session.setStage(Stage.IDEA);
        session.setContextJson("{}");
        session.setCreatedAt(LocalDateTime.now());
        return conversationRepository.save(session);
    }

    private void saveMessage(String sessionId, String role, String content) {
        Message msg = new Message();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        messageRepository.save(msg);
    }

    private String buildPrompt(ConversationSession session, List<Message> history, String input) {

        String historyText = history.stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .reduce("", (a, b) -> a + "\n" + b);

        return """
        Você é um especialista sênior em engenharia de requisitos.

        ESTÁGIO: %s

        CONTEXTO:
        %s

        HISTÓRICO:
        %s

        ENTRADA:
        %s

        AÇÃO:
        - Continue refinando a ideia
        - Faça perguntas objetivas
        - Estruture melhor os requisitos
        """.formatted(
                session.getStage(),
                session.getContextJson(),
                historyText,
                input
        );
    }

    public List<Message> list(String sessionId) {
        List<Message> messages = messageRepository.findBySessionId(sessionId);
        log.info("[CHAT] Consulta de historico sessao={} totalMensagens={}", sessionId, messages.size());
        return messages;
    }

    public SpecificationDocument generateAndSaveSpecification(String sessionId) {
        log.info("[SPEC] Geracao iniciada sessao={}", sessionId);
        ConversationSession session = conversationRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada: " + sessionId));

        List<Message> history = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
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
        CONTEXTO DA SESSAO: %s
        HISTORICO:
        %s
        """.formatted(session.getStage(), session.getContextJson(), historyText);

        String generated = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (generated == null || generated.isBlank()) {
            generated = "# Especificacao\n\nNao foi possivel gerar o documento.";
        }

        SpecificationDocument specification = new SpecificationDocument();
        specification.setSessionId(sessionId);
        specification.setContent(generated.trim());
        specification.setGeneratedAt(LocalDateTime.now());
        SpecificationDocument saved = specificationDocumentRepository.save(specification);
        log.info("[SPEC] Especificacao salva id={} sessao={} tamanho={}", saved.getId(), sessionId, saved.getContent().length());
        return saved;
    }

    public Optional<SpecificationDocument> getLatestSpecification(String sessionId) {
        Optional<SpecificationDocument> specification = specificationDocumentRepository.findTopBySessionIdOrderByGeneratedAtDesc(sessionId);
        log.info("[SPEC] Consulta ultima especificacao sessao={} encontrada={}", sessionId, specification.isPresent());
        return specification;
    }

    @Transactional
    public void delete(String sessionId) {
        log.info("[CHAT] Exclusao iniciada sessao={}", sessionId);
        List<SpecificationDocument> specifications = specificationDocumentRepository.findBySessionId(sessionId);
        List<Message> messages = messageRepository.findBySessionId(sessionId);
        specificationDocumentRepository.deleteAll(specifications);
        messageRepository.deleteAll(messages);
        conversationRepository.deleteById(sessionId);
        log.info("[CHAT] Exclusao finalizada sessao={} specsRemovidas={} mensagensRemovidas={}", sessionId, specifications.size(), messages.size());
    }
}
