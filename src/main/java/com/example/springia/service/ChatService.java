package com.example.springia.service;

import com.example.springia.agent.advisors.LogAdvisor;
import com.example.springia.agent.client.CodeGeneratorOpenAiAgent;
import com.example.springia.agent.tool.discovery.ProjectTool;
import com.example.springia.agent.tool.files.*;
import com.example.springia.model.*;
import com.example.springia.utils.LogUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.springia.dto.ChatMessageRequest;
import com.example.springia.dto.CreateSessionRequest;
import com.example.springia.model.enums.MessageRole;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import com.example.springia.utils.OpenAIUtils;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.*;
import com.example.springia.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Serviço de chat para sessão conversacional e geração de requisitos.
 * <p>{@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.service.ChatService" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}</p>
 */
@Slf4j
@Service
public class ChatService {

    private static final String FIXED_REQUIREMENTS_INSTRUCTIONS = """
            Você é um Analista de Requisitos sênior.

            Objetivo:
            - Levantar requisitos funcionais e não funcionais.
            - Buscar informações do sistema baseado em dados do projeto ou buscar nos arquivos fontes caso necessário.
            - Identificar ambiguidades e informar somente se existir.
            - Sugerir melhorias se necessário.
            - Estruturar requisitos em formato claro e validável.

            Regras:
            - Sempre faça perguntas quando houver dúvida.
            - **NUNCA** presuma o nome de algum arquivo ou funcionalidade sem buscar a informação direto no arquivo. Use as tools para isso. Set tiver dificuldade para usar alguma tools retorne o problema.
            - Não peça permissão para usar as tools. Elas estão a disposição.
            - Primeiro exiba os requisitos e faça as perguntas no final.
            - Seja objetivo e estruturado e a resposta nao pode ser maior que 4000 caracteres
            - Use a tool 'project_tool' para buscar dados do projeto e seus repositórios.
            - Use a tool 'grep_files' para buscar arquivos que atendem ao critério de busca.
            - Use a tool 'read_file' para buscar o contendo completo de um arquivo.
            """;

    private final ChatClient chatClient;
    private final CodeGeneratorOpenAiAgent codeGeneratorOpenApiAgent;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProjectService projectService;
    private final SpecificationDocumentRepository specificationDocumentRepository;
    private final UserStoryRepository userStoryRepository;
    private final PromptRepository promptRepository;
    private final LogAdvisor logAdvisor;
    private final ProjectTool projectTool;
    private final GrepFilesTool grepFilesTool;
    private final ReadFileTool readFileTool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentMap<Long, SessionMemory> sessionMemoryById = new ConcurrentHashMap<>();

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            CodeGeneratorOpenAiAgent codeGeneratorOpenApiAgent,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ProjectService projectService,
            SpecificationDocumentRepository specificationDocumentRepository,
            UserStoryRepository userStoryRepository,
            PromptRepository promptRepository,
            LogAdvisor logAdvisor,
            ProjectTool projectTool,
            GrepFilesTool grepFilesTool,
            ReadFileTool readFileTool
    ) {
        this.chatClient = chatClientBuilder.build();
        this.codeGeneratorOpenApiAgent = codeGeneratorOpenApiAgent;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.projectService = projectService;
        this.specificationDocumentRepository = specificationDocumentRepository;
        this.userStoryRepository = userStoryRepository;
        this.promptRepository = promptRepository;
        this.logAdvisor = logAdvisor;
        this.projectTool = projectTool;
        this.grepFilesTool = grepFilesTool;
        this.readFileTool = readFileTool;
    }

    public List<ChatMessageRequest> chatMessage(ChatMessageRequest request) {
        log.info("[CHAT] Iniciando processamento da sessao={}", request.getSessionId());

        ConversationSession session = resolveSession(request.getSessionId(), request.getProjectId(), request.getSessionName());

        Long effectiveSessionId = session.getId();
        request.setSessionId(effectiveSessionId);
        request.setProjectId(session.getProject().getId());

        log.debug("[CHAT] Sessao carregada id={} stage={}", effectiveSessionId, session.getStatus());

        String userInput = request.getMessage();

        saveMessage(session, MessageRole.USER, userInput);
        log.debug("[CHAT] Mensagem USER salva sessao={} tamanho={}", effectiveSessionId, userInput != null ? userInput.length() : 0);

        SessionMemory sessionMemory = resolveSessionMemory(session, request.getProjectId());
        String response = callResponsesApi(sessionMemory, userInput, effectiveSessionId);

        if (response.isBlank()) {
            response = "Nao foi possivel gerar uma resposta no momento.";
        }

        log.info("[CHAT] RESPONSE: {}", response);
        Message assistant = saveMessage(session, MessageRole.ASSISTANT, response);
        log.debug("[CHAT] Mensagem ASSISTANT salva sessao={} tamanho={}", effectiveSessionId, response.length());
        log.info("[CHAT] Processamento finalizado sessao={}", effectiveSessionId);
        assistant.getConversationSession().setMessages(null);
        assistant.getConversationSession().setProject(null);

        return messageRepository.findByConversationSessionId(request.getSessionId()).stream().map(m->ChatMessageRequest.builder()
                .message(m.getContent())
                .messageId(m.getId())
                .sessionName(m.getConversationSession().getName())
                .sessionId(m.getConversationSession().getId())
                .timestamp(m.getTimestamp())
                .role(m.getRole())
                .build()).collect(Collectors.toList());
    }



    public Message createSession(CreateSessionRequest request) {
        return chat(null, request.projectId(), request.name(), request.message());    }

    public Message chat(Long sessionId, Long projectId, String userInput) {
        return chat(sessionId, projectId, null, userInput);
    }

    private Message chat(Long sessionId, Long projectId, String sessionName, String userInput) {
        log.info("[CHAT] Iniciando processamento da sessao={}", sessionId);

        ConversationSession session = resolveSession(sessionId, projectId, sessionName);
        Long effectiveSessionId = session.getId();
        log.debug("[CHAT] Sessao carregada id={} stage={}", effectiveSessionId, session.getStatus());

        saveMessage(session, MessageRole.USER, userInput);
        log.debug("[CHAT] Mensagem USER salva sessao={} tamanho={}", effectiveSessionId, userInput != null ? userInput.length() : 0);

        SessionMemory sessionMemory = resolveSessionMemory(session, projectId);
        String response = callResponsesApi(sessionMemory, userInput, effectiveSessionId);

        if (response.isBlank()) {
            response = "Nao foi possivel gerar uma resposta no momento.";
        }

        log.info("[CHAT] RESPONSE: {}", response);
        Message assistant = saveMessage(session, MessageRole.ASSISTANT, response);
        log.debug("[CHAT] Mensagem ASSISTANT salva sessao={} tamanho={}", effectiveSessionId, response.length());
        log.info("[CHAT] Processamento finalizado sessao={}", effectiveSessionId);
        assistant.getConversationSession().setMessages(null);
        assistant.getConversationSession().setProject(null);
        return assistant;
    }

    public String chat(String userInput) {
        log.info("[CHAT_TEXT] Iniciando chat avulso");
        String response = callWithAdvisors(userInput, null);

        if (response == null || response.isBlank()) {
            return "Nao foi possivel gerar uma resposta no momento.";
        }

        return response;
    }

    public String chat(String userInput, Class<?> dto) {
        log.info("[CHAT_TEXT_DTO] Iniciando chat avulso com dto={}", dto != null ? dto.getSimpleName() : null);
        String response = callWithAdvisors(userInput, null);

        if (response == null || response.isBlank()) {
            return "Nao foi possivel gerar uma resposta no momento.";
        }

        return response;
    }

    /**
     * Método centralizado para chamadas ao modelo IA com os Advisors aplicados.
     * Todos os fluxos de geração passam por aqui.
     */
    private String callWithAdvisors(String input, Long sessionId) {
        log.debug("[CHAT] callWithAdvisors sessao={}", sessionId);
        return chatClient.prompt()
                .advisors(logAdvisor)
                .user(input)
                .call()
                .content();
    }

    public ChatClient getChatClient() {
        return chatClient;
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

        Project project = projectService.findById(projectId);

        log.info("[CHAT] Criando nova sessao projectId={}", projectId);
        ConversationSession session = new ConversationSession();
        //session.setId(null);
        session.setName(sessionName);
        session.setStatus(SpecificationDocumentStatus.IN_PROGRESS);
        session.setCreatedAt(LocalDateTime.now());
        session.setProject(project);

        return conversationRepository.save(session);
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

    public List<Message> list(Long sessionId) {
        log.info("[LIST] Consultando historico sessao={}", sessionId);
        List<Message> messages = messageRepository.findByConversationSessionId(sessionId);
        log.info("[CHAT] Consulta de historico sessao={} totalMensagens={}", sessionId, messages.size());
        return messages;
    }

    public UserStory createUserStory(Long sessionId) {
        log.info("[CREATE_USER_STORY] Gerando user story da sessao={}", sessionId);
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

        String generated = callWithAdvisors(prompt, sessionId);

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
        log.info("[GET_LATEST_SPEC] Consultando ultima especificacao sessao={}", sessionId);
        Optional<UserStory> specification = specificationDocumentRepository.findTopByConversationSessionIdOrderByGeneratedAtDesc(sessionId);
        log.info("[SPEC] Consulta ultima especificacao sessao={} encontrada={}", sessionId, specification.isPresent());
        return specification;
    }

    @Transactional
    public void delete(Long sessionId) {
        log.info("[DELETE] Iniciando limpeza da sessao={}", sessionId);
        log.info("[CHAT] Exclusao iniciada sessao={}", sessionId);
        List<UserStory> specifications = specificationDocumentRepository.findByConversationSessionId(sessionId);
        List<Message> messages = messageRepository.findByConversationSessionId(sessionId);
        specificationDocumentRepository.deleteAll(specifications);
        messageRepository.deleteAll(messages);
        conversationRepository.deleteById(sessionId);
        sessionMemoryById.remove(sessionId);
        log.info("[CHAT] Exclusao finalizada sessao={} specsRemovidas={} mensagensRemovidas={}", sessionId, specifications.size(), messages.size());
    }

    public Message aprove(Long sessionId) {
        log.info("[APROVE] Aprovando conteudo da sessao={}", sessionId);

        Message message = messageRepository.findByLastMessage(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Nao ha mensagens na sessao: " + sessionId));

        ConversationSession conversationSession = message.getConversationSession();
        if (conversationSession == null) {
            throw new IllegalStateException("Mensagem sem sessao associada para sessaoId=" + sessionId);
        }
        conversationSession.setStatus(SpecificationDocumentStatus.APPROVED);
        conversationRepository.save(conversationSession);

        String prompt = promptRepository.findByKey("CREATE_USER_STORY")
                .orElseThrow(() -> new IllegalArgumentException("Prompt CREATE_USER_STORY nao encontrado"))
                .getContent();
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalStateException("Prompt CREATE_USER_STORY sem conteudo");
        }

        String promptCompleto = prompt.concat(":\n\n\n")
                .concat("---------------------------\n")
                .concat(message.getContent())
                .concat("\n---------------------------\n");

        log.info("[CHAT] prompt CREATE_USER_STORY [{}]: {} ", promptCompleto.length(), promptCompleto);

        String response = callWithAdvisors(promptCompleto, sessionId);

        if (response == null || response.isBlank()) {
            response = "Nao foi possivel gerar a User Story.";
        }

        log.info("[CHAT] conteudo da User Story: {} ", response);

        userStoryRepository.save(UserStory.builder()
                .conversationSession(conversationSession)
                .content(response)
                .status(SpecificationDocumentStatus.APPROVED)
                .generatedAt(LocalDateTime.now())
                .build());
        return message;
    }

    public UserStory gerarHistoriaDeUsuario(Long sessionId) {
        log.info("[APROVE] Aprovando conteudo da sessao={}", sessionId);

        List<Message> messages = messageRepository.findByConversationSessionIdOrderByTimestampAsc(sessionId);
        StringBuilder historicoChat = new StringBuilder("```chat\n");
        ConversationSession conversationSession = null;
        for (Message message : messages) {
            historicoChat.append(message.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(" [").append(message.getRole()).append("]: ").append(message.getContent()).append("\n\n");
            conversationSession = message.getConversationSession();
        }
        historicoChat.append("```");
        if (conversationSession == null) {
            throw new IllegalStateException("Mensagem sem sessao associada para sessaoId=" + sessionId);
        }
        conversationSession.setStatus(SpecificationDocumentStatus.APPROVED);
        conversationRepository.save(conversationSession);

        String prompt = promptRepository.findByKey("CREATE_USER_STORY")
                .orElseThrow(() -> new IllegalArgumentException("Prompt CREATE_USER_STORY nao encontrado"))
                .getContent();
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalStateException("Prompt CREATE_USER_STORY sem conteudo");
        }
        Project project = conversationSession.getProject();
        prompt = prompt.replace("{{HISTORICO_CHAT}}", historicoChat.toString()).replace("{{CONSTITUTION}}", projectService.getConstitution(project));

        log.info("[CHAT] prompt CREATE_USER_STORY [file:{}]: {} ", LogUtils.saveLog(prompt, "prompt-criar-historia-usuario", "md"), prompt.length());

        //String response = callWithAdvisors(prompt.toString(), sessionId);
        String response = codeGeneratorOpenApiAgent.executar(prompt.toString());

        if (response == null || response.isBlank()) {
            response = "Nao foi possivel gerar a User Story.";
        }

        log.info("[CHAT] conteudo da User Story: {} ", response);

        return userStoryRepository.save(UserStory.builder()
                .conversationSession(conversationSession)
                .content(response)
                .status(SpecificationDocumentStatus.APPROVED)
                .generatedAt(LocalDateTime.now())
                .build());
    }

    private SessionMemory resolveSessionMemory(ConversationSession session, Long requestProjectId) {
        log.debug("[RESOLVE_MEM] Carregando memoria da sessao={}", session.getId());
        return sessionMemoryById.computeIfAbsent(session.getId(), id -> {
            Long projectId = session.getProject() != null ? session.getProject().getId() : requestProjectId;
            String instructions = buildSessionInstructions(projectId);
            log.info("[RESOLVE_MEM] Memoria criada para sessao={} projectId={}", id, projectId);
            return new SessionMemory(instructions);
        });
    }

    private String buildSessionInstructions(Long projectId) {
        log.debug("[BUILD_INSTR] Montando instrucoes fixas do chat projectId={}", projectId);
        if (projectId == null) {
            return FIXED_REQUIREMENTS_INSTRUCTIONS;
        }

        try {
            String projectContext = projectTool.getSystemPrompt(projectId);
            return FIXED_REQUIREMENTS_INSTRUCTIONS + "\n\nContexto do projeto:\nID dp Projeto (project_id): "+projectId+"\n" + projectContext;
        } catch (Exception e) {
            log.error("[BUILD_INSTR] Falha ao buscar contexto via project_tool, seguindo com instrucoes fixas", e);
            return FIXED_REQUIREMENTS_INSTRUCTIONS;
        }
    }

    private String callResponsesApi(SessionMemory memory, String userInput, Long sessionId) {
        log.debug("[CALL_RESP_API] Chamando Responses API sessao={} temPrevId={}", sessionId, memory.getPreviousResponseId() != null);

        try {
            OpenAIClient client = getOpenAIClient();
            String deploymentName = System.getenv().getOrDefault("AZURE_OPENAI_DEPLOYMENT", "gpt-5.3-codex").trim();

            // Primeira chamada
            ResponseCreateParams.Builder paramsBuilder = ResponseCreateParams.builder()
                    .model(deploymentName)
                    .instructions(memory.getInstructions())
                    .toolChoice(ToolChoiceOptions.AUTO)
                    .input(userInput);

            OpenAIUtils.montarTools(GrepFilesTool.createTool(), ReadFileTool.createTool(), ProjectTool.createTool())
                    .forEach(paramsBuilder::addTool);

            if (memory.getPreviousResponseId() != null && !memory.getPreviousResponseId().isBlank()) {
                paramsBuilder.previousResponseId(memory.getPreviousResponseId());
            }

            Response response = client.responses().create(paramsBuilder.build());
            memory.setPreviousResponseId(response.id());

            // Loop de function calling
            int maxIteracoes = 10;
            int iteracao = 0;
            while (iteracao++ < maxIteracoes && temToolCalls(response)) {
                log.info("[CALL_RESP_API] Iteracao function calling={} de={} sessao={}", iteracao, maxIteracoes, sessionId);

                List<ResponseInputItem> toolOutputs = executarToolCalls(response, sessionId);

                ResponseCreateParams loopParams = ResponseCreateParams.builder()
                        .model(deploymentName)
                        .previousResponseId(response.id())
                        .inputOfResponse(toolOutputs)
                        .build();

                response = client.responses().create(loopParams);
                memory.setPreviousResponseId(response.id());
            }

            if (iteracao > maxIteracoes) {
                log.warn("[CALL_RESP_API] Limite de iteracoes atingido sessao={}", sessionId);
            }

            return OpenAIUtils.extrairResposta(response);
        } catch (Exception e) {
            log.error("[CALL_RESP_API] Erro ao chamar Responses API da sessao=" + sessionId, e);
            return "Nao foi possivel gerar uma resposta no momento.";
        }
    }

    private boolean temToolCalls(Response response) {
        return response.output().stream().anyMatch(item -> item.functionCall().isPresent());
    }

    private List<ResponseInputItem> executarToolCalls(Response response, Long sessionId) {
        log.debug("[EXEC_TOOL_CALLS] Processando tool calls sessao={}", sessionId);
        List<ResponseInputItem> results = new ArrayList<>();

        for (ResponseOutputItem item : response.output()) {
            if (item.functionCall().isEmpty()) {
                continue;
            }

            ResponseFunctionToolCall toolCall = item.functionCall().get();
            String toolName = toolCall.name();
            String arguments = toolCall.arguments();
            String callId = toolCall.callId();

            log.info("[EXEC_TOOL_CALLS] Executando tool={} callId={} sessao={}", toolName, callId, sessionId);
            log.debug("[EXEC_TOOL_CALLS] Argumentos tool={}: {}", toolName, arguments);

            String resultado;
            try {
                Map<String, String> params = parsearArgumentos(arguments);
                resultado = rotearTool(toolName, params);
                log.debug("[EXEC_TOOL_CALLS] Resultado tool={} tamanho={}", toolName, resultado.length());
            } catch (Exception e) {
                log.error("[EXEC_TOOL_CALLS] Erro ao executar tool={} callId={}", toolName, callId, e);
                resultado = "Erro ao executar a ferramenta " + toolName + ": " + e.getMessage();
            }

            results.add(ResponseInputItem.ofFunctionCallOutput(
                    ResponseInputItem.FunctionCallOutput.builder()
                            .callId(callId)
                            .output(resultado)
                            .build()
            ));
        }

        log.debug("[EXEC_TOOL_CALLS] Total de resultados={} sessao={}", results.size(), sessionId);
        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parsearArgumentos(String arguments) throws Exception {
        log.trace("[PARSEAR_ARGS] Parseando argumentos: {}", arguments);
        Map<String, Object> raw = objectMapper.readValue(arguments, Map.class);
        Map<String, String> params = new HashMap<>();
        raw.forEach((k, v) -> params.put(k, v != null ? v.toString() : null));
        return params;
    }

    private String rotearTool(String toolName, Map<String, String> params) throws Exception {
        log.debug("[ROTEAR_TOOL] Roteando para tool={}", toolName);
        return switch (toolName) {
            case "grep_files" -> grepFilesTool.execute(params);
            case "read_file" -> readFileTool.execute(params);
            case "project_tool" -> projectTool.execute(params);
            default -> throw new IllegalArgumentException("Tool desconhecida: " + toolName);
        };
    }

    private OpenAIClient getOpenAIClient() {
        log.debug("[GET_OPENAI] Criando client OpenAI para Responses API");

        String baseUrl = System.getenv().getOrDefault("SPRING_AI_OPENAI_BASE_URL", "").trim();
        String apiKey = System.getenv().getOrDefault("SPRING_AI_OPENAI_API_KEY", "").trim();

        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("Defina SPRING_AI_OPENAI_BASE_URL");
        }

        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("Defina SPRING_AI_OPENAI_API_KEY");
        }

        return OpenAIOkHttpClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageRequest> listBySession(Long id) {
        return messageRepository.findByConversationSessionId(id).stream().map(m->ChatMessageRequest.builder()
                .message(m.getContent())
                .messageId(m.getId())
                .sessionName(m.getConversationSession().getName())
                .sessionId(m.getConversationSession().getId())
                .timestamp(m.getTimestamp())
                .role(m.getRole())
                .build()).collect(Collectors.toList());
    }

    private static class SessionMemory {

        private final String instructions;
        private String previousResponseId;

        private SessionMemory(String instructions) {
            this.instructions = instructions;
        }

        private String getInstructions() {
            return instructions;
        }

        private String getPreviousResponseId() {
            return previousResponseId;
        }

        private void setPreviousResponseId(String previousResponseId) {
            this.previousResponseId = previousResponseId;
        }
    }
}
