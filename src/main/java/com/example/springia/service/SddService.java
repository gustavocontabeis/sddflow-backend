package com.example.springia.service;

import com.example.springia.agent.client.CodeGeneratorOpenAiAgent;
import com.example.springia.dto.ImplSddValidationDto;
import com.example.springia.dto.PromptAuditResponse;
import com.example.springia.model.*;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import com.example.springia.repository.UserStoryRepository;
import com.example.springia.utils.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Serviço para gerenciar SDD (Spec Driven Development).
 * curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.service.SddService" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'
 */
@Slf4j
@Service
public class SddService {

    @Autowired
    private ChatService chatService;

    @Autowired
    private CodeGeneratorOpenAiAgent codeGeneratorOpenAiAgent;

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private SpecSddService specSddService;

    @Autowired
    private PlanSddService planSddService;

    @Autowired
    private TaskSddService taskSddService;

    @Autowired
    private ImplSddService implSddService;

    @Autowired
    private PromptService promptService;

    @Autowired
    private ProjectService projectService;

    public PromptAuditResponse analyzePrompt(Long userStoryId, String question) {
        log.info("[ANALYZE_PROMPT] iniciando para userStoryId={} questionLength={}",
                userStoryId,
                question != null ? question.length() : 0);

        try {
            UserStory userStory = userStoryRepository.findById(userStoryId)
                    .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

            String auditPrompt = buildPromptAuditPrompt(userStory, question);
            log.debug("[ANALYZE_PROMPT] auditPrompt gerado, length={}", auditPrompt.length());

            String rawResponse = chatService.chat(auditPrompt);
            log.debug("[ANALYZE_PROMPT] resposta recebida, length={}", rawResponse != null ? rawResponse.length() : 0);

            return parsePromptAuditResponse(userStoryId, question, rawResponse);
        } catch (Exception ex) {
            log.error("[ANALYZE_PROMPT] erro ao analisar prompt para userStoryId={}", userStoryId, ex);
            throw ex;
        }
    }

    public String createSpec(Long userStoryId) {
        log.info("[CREATE_SPEC] iniciando para userStoryId={}", userStoryId);

        try {
            UserStory userStory = userStoryRepository.findById(userStoryId)
                    .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

            String prompt = loadPromptContent("CREATE_SSD_SPEC");

            String projectConstitution = projectService.getConstitution(userStory.getConversationSession().getProject());

            prompt = prompt
                    .replace("{{CONSTITUTION}}", projectConstitution)
                    .replace("{{USER_STORY}}", userStory.getContent());

            log.debug("[CREATE_SPEC] prompt preparado, [file:{}]", LogUtils.saveLog(prompt, "prompt-criar-sdd-spec", "md"));

            String spec = codeGeneratorOpenAiAgent.executar(prompt);

            specSddService.saveSpec(userStory, spec);

            log.info("[CREATE_SPEC] concluida com sucesso para userStoryId={}, specLength={}", userStoryId, spec.length());

            return spec;
        } catch (Exception ex) {
            log.error("[CREATE_SPEC] erro ao gerar spec para userStoryId={}", userStoryId, ex);
            throw ex;
        }
    }

    public String createPlan(Long userStoryId) {
        log.info("[CREATE_PLAN] iniciando para userStoryId={}", userStoryId);

        try {
            UserStory userStory = userStoryRepository.findById(userStoryId)
                    .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

            String prompt = loadPromptContent("CREATE_SSD_PLAN");

            ConversationSession conversationSession = userStory.getConversationSession();
            Project project = conversationSession.getProject();
            String projectConstitution = projectService.getConstitution(project);

            prompt = prompt
                    .replace("{{CONSTITUTION}}", projectConstitution)
                    .replace("{{USER_STORY}}", userStory.getContent())
                    .replace("{{SDD_SPEC}}", userStory.getSpec().getContent());

            log.debug("[CREATE_PLAN] prompt preparado, [file:{}]", LogUtils.saveLog(prompt, "prompt-criar-sdd-plan", "md"));

            String plan = codeGeneratorOpenAiAgent.executar(prompt);

            planSddService.savePlan(userStory, plan);

            log.info("[CREATE_PLAN] concluida com sucesso para userStoryId={}, planLength={}", userStoryId, plan.length());

            return plan;
        } catch (Exception ex) {
            log.error("[CREATE_PLAN] erro ao gerar plan para userStoryId={}", userStoryId, ex);
            throw ex;
        }
    }

    public String createTask(Long userStoryId) {
        log.info("[CREATE_TASK] iniciando para userStoryId={}", userStoryId);

        try {
            UserStory userStory = userStoryRepository.findById(userStoryId)
                    .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

            String prompt = loadPromptContent("CREATE_SSD_TASK");

            ConversationSession conversationSession = userStory.getConversationSession();
            Project project = conversationSession.getProject();
            String projectConstitution = projectService.getConstitution(project);

            prompt = prompt
                    .replace("{{CONSTITUTION}}", projectConstitution)
                    .replace("{{SDD_SPEC}}", userStory.getSpec().getContent())
                    .replace("{{SDD_PLAN}}", userStory.getPlan().getContent());

            log.debug("[CREATE_TASK] prompt preparado, [file:{}]", LogUtils.saveLog(prompt, "prompt-criar-sdd-task", "md"));

            String content = codeGeneratorOpenAiAgent.executar(prompt);

            taskSddService.saveTask(userStory, content);

            log.info("[CREATE_TASK] concluida com sucesso para userStoryId={}, contentLength={}", userStoryId, content.split(" ").length);

            return content;
        } catch (Exception ex) {
            log.error("[CREATE_TASK] erro ao gerar task para userStoryId={}", userStoryId, ex);
            throw ex;
        }
    }

    public String createImpl(Long userStoryId) {
        log.info("[CREATE_IMPL] iniciando para userStoryId={}", userStoryId);

        try {
            UserStory userStory = userStoryRepository.findById(userStoryId)
                    .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

            String prompt = loadPromptContent("CREATE_SSD_IMPL");

            ConversationSession conversationSession = userStory.getConversationSession();

            prompt = prompt
                    .replace("{{SDD_SPEC}}", userStory.getSpec().getContent())
                    .replace("{{SDD_PLAN}}", userStory.getPlan().getContent())
                    .replace("{{SDD_TASK}}", userStory.getTask().getContent())
            ;

            log.debug("[CREATE_IMPL] prompt preparado, [file:{}]", LogUtils.saveLog(prompt, "prompt-criar-sdd-impl", "md"));

            Project project = conversationSession.getProject();
            String content = codeGeneratorOpenAiAgent.executar(project, prompt);

            log.debug("[CREATE_IMPL] log de execucao do agente, [file:{}]", LogUtils.saveLog(content, "execucao-do-agente", "md"));

            implSddService.save(ImplSdd.builder().id(null).status(SpecificationDocumentStatus.IN_PROGRESS).userStory(userStory).content(content).build());

            log.info("[CREATE_IMPL] concluida com sucesso para userStoryId={}, contentLength={}", userStoryId, content.split(" ").length);

            return content;
        } catch (Exception ex) {
            log.error("[CREATE_IMPL] erro ao gerar impl para userStoryId={}", userStoryId, ex);
            throw ex;
        }
    }

    private ImplSddValidationDto validarCodigoGerado(String content) {
        log.debug("[VALIDAR_CODIGO] iniciando validacao");

        try {
            String validationPrompt = """
                        Você é um engenheiro de software sênior.
                        Este é um checklist do código gerado. Analise o códido e valide se está conforme o checklist e retorne conforme o JSON especificado.
                        IMPORTANTE: Se o código não estiver válido corrija.
                        # BACKEND
                        - Valide se o código gerado não tem classes do pacote javax. precisa ser jakarta:
                        - Classes de domínio precisam ter comentários da proposta em todas as colunas em javadoc:
                        - Código deverá ser gerado dentro do diretório definido na "Estrutura de Diretórios" da contitution.
                        # FRONTEND
                        - Todo componente gerado deve ter o arquivo .html e o .ts
                        - Se um novo componente foi criado ele deverá ter uma rota de acesso
                        - Código deverá ser gerado dentro do diretório definido na "Estrutura de Diretórios" da contitution.
                        # RETORNO:
                        - Responda SOMENTE em JSON neste formato:
                        \\{
                          "content": string, //CODIGO GERADO original ou CODIGO GERADO corrigido. Se houveram erros de validação, o conteúdo aqui já deve estar corrigido.
                          "problems": string, /Liste os erros de validação encontrados.
                          "valid": boolean //true se não houveram erro de valiação.
                        \\}
                        =========== CODIGO GERADO ===========
                        {{PROMPT}}
                        """.replace("{{PROMPT}}", content);

            ImplSddValidationDto implSddValidationDto = chatService.getChatClient().prompt()
                    .user(validationPrompt)
                    .call()
                    .entity(ImplSddValidationDto.class);

            if (!implSddValidationDto.isValid()) {
                log.debug("[VALIDAR_CODIGO] erros encontrados: {}", implSddValidationDto.getProblems());
            } else {
                log.debug("[VALIDAR_CODIGO] validacao OK");
            }

            return implSddValidationDto;
        } catch (Exception ex) {
            log.error("[VALIDAR_CODIGO] erro ao validar codigo", ex);
            throw ex;
        }
    }

    private String buildPromptAuditPrompt(UserStory userStory, String question) {
        log.debug("[BUILD_PROMPT_AUDIT] iniciando construcao");

        try {
            String promptUserStory = loadPromptContent("CREATE_USER_STORY");
            String promptSpec = loadPromptContent("CREATE_SSD_SPEC");
            String promptPlan = loadPromptContent("CREATE_SSD_PLAN");
            String promptTask = loadPromptContent("CREATE_SSD_TASK");
            String promptImpl = loadPromptContent("CREATE_SSD_IMPL");

            String constitution = safeText(userStory.getConversationSession() != null
                    && userStory.getConversationSession().getProject() != null
                    ? userStory.getConversationSession().getProject().getConstitution()
                    : null);

            String userStoryContent = safeText(userStory.getContent());
            String specContent = safeText(userStory.getSpec() != null ? userStory.getSpec().getContent() : null);
            String planContent = safeText(userStory.getPlan() != null ? userStory.getPlan().getContent() : null);
            String taskContent = safeText(userStory.getTask() != null ? userStory.getTask().getContent() : null);
             String implContent = safeText(userStory.getImpl() != null ? userStory.getImpl().getContent() : null);
             log.debug("[BUILD_PROMPT_AUDIT] conteudos carregados");

            return """
                    Você é um analista sênior de prompts e rastreamento de erros.

                    Objetivo: ler a pergunta e vasculhar quais prompts geraram aquele resultado.
                    Se o prompt não gerou este resultado diga "Não há mensões no prompt para gerar este resultado."
                    Compare a pergunta com todas as fontes abaixo e diga exatamente em quais fontes estão as instruções mais prováveis.

                    Regras de resposta:
                    - Responda obrigatoriamente com as chaves abaixo, uma por linha.
                    - Para cada ponto encontrado diga fonte entre: CONSTITUTION, USER_STORY, SPEC, PLAN, TASK, IMPL, DESCONHECIDA com prioridade nesta mesma ordem.
                    - Se conseguir identificar o prompt template relacionado, preencha CHAVE_PROMPT com o nome correto.
                    - Se não houver confiança suficiente, use DESCONHECIDA pois entendo que foi alucunação.
                    - Seja conciso e cite o trecho exato que motivou a decisão.

                    Formato obrigatório:
                    FONTE: <CONSTITUTION|USER_STORY|SPEC|PLAN|TASK|IMPL|DESCONHECIDA>
                    CHAVE_PROMPT: <CREATE_USER_STORY|CREATE_SSD_SPEC|CREATE_SSD_PLAN|CREATE_SSD_TASK|CREATE_SSD_IMPL|N/A>
                    CONFIANCA: <0-100>
                    JUSTIFICATIVA: <texto curto>
                    TRECHO: <trecho curto da fonte>

                    PERGUNTA:
                    %s

                    FONTES:

                    [CONSTITUTION]
                    Prompt template key: N/A
                    Conteudo:
                    %s

                    [USER_STORY]
                    Prompt template key: CREATE_USER_STORY
                    Prompt template:
                    %s
                    Conteudo:
                    %s

                    [SPEC]
                    Prompt template key: CREATE_SSD_SPEC
                    Prompt template:
                    %s
                    Conteudo:
                    %s

                    [PLAN]
                    Prompt template key: CREATE_SSD_PLAN
                    Prompt template:
                    %s
                    Conteudo:
                    %s

                    [TASK]
                    Prompt template key: CREATE_SSD_TASK
                    Prompt template:
                    %s
                    Conteudo:
                    %s

                    [IMPL]
                    Prompt template key: CREATE_SSD_IMPL
                    Prompt template:
                    %s
                    Conteudo:
                    %s
                    """.formatted(
                    safeText(question),
                    constitution,
                    promptUserStory,
                    userStoryContent,
                    promptSpec,
                    specContent,
                    promptPlan,
                    planContent,
                    promptTask,
                    taskContent,
                    promptImpl,
                    implContent
            );
        } catch (Exception ex) {
            log.error("[BUILD_PROMPT_AUDIT] erro ao construir prompt", ex);
            throw ex;
        }
     }

    private PromptAuditResponse parsePromptAuditResponse(Long userStoryId, String question, String rawResponse) {
        log.debug("[PARSE_PROMPT_AUDIT] iniciando parse para userStoryId={}", userStoryId);

        try {
            if (rawResponse == null || rawResponse.isBlank()) {
                log.debug("[PARSE_PROMPT_AUDIT] resposta vazia");
                return new PromptAuditResponse(userStoryId, question, null, null, null, null, null, rawResponse);
            }

            String source = null;
            String promptKey = null;
            Integer confidence = null;
            String justification = null;
            String excerpt = null;

            for (String line : rawResponse.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("FONTE:")) {
                    source = trimmed.substring("FONTE:".length()).trim();
                } else if (trimmed.startsWith("CHAVE_PROMPT:")) {
                    promptKey = trimmed.substring("CHAVE_PROMPT:".length()).trim();
                    if ("N/A".equalsIgnoreCase(promptKey)) {
                        promptKey = null;
                    }
                } else if (trimmed.startsWith("CONFIANCA:")) {
                    confidence = parseInteger(trimmed.substring("CONFIANCA:".length()).trim());
                } else if (trimmed.startsWith("JUSTIFICATIVA:")) {
                    justification = trimmed.substring("JUSTIFICATIVA:".length()).trim();
                } else if (trimmed.startsWith("TRECHO:")) {
                    excerpt = trimmed.substring("TRECHO:".length()).trim();
                }
            }

            if (promptKey == null) {
                promptKey = mapPromptKey(source);
            }

            if (justification == null || justification.isBlank()) {
                justification = rawResponse;
            }

            log.debug("[PARSE_PROMPT_AUDIT] parse concluido");
            return new PromptAuditResponse(userStoryId, question, source, promptKey, confidence, justification, excerpt, rawResponse);
        } catch (Exception ex) {
            log.error("[PARSE_PROMPT_AUDIT] erro ao fazer parse para userStoryId={}", userStoryId, ex);
            throw ex;
        }
    }

    private String loadPromptContent(String key) {
        log.debug("[LOAD_PROMPT_CONTENT] carregando prompt chave={}", key);
        try {
            return promptService.findByKey(key)
                    .map(Prompt::getContent)
                    .orElse("[PROMPT NAO ENCONTRADO: " + key + "]");
        } catch (Exception ex) {
            log.error("[LOAD_PROMPT_CONTENT] erro ao carregar prompt chave={}", key, ex);
            throw ex;
        }
    }

    private String safeText(String text) {
        return text != null && !text.isBlank() ? text : "[CONTEUDO NAO INFORMADO]";
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            log.debug("[PARSE_INTEGER] erro ao fazer parse de value={}", value);
            return null;
        }
    }

    private String mapPromptKey(String source) {
        if (source == null) {
            return null;
        }

        return switch (source.trim().toUpperCase()) {
            case "USER_STORY" -> "CREATE_USER_STORY";
            case "SPEC" -> "CREATE_SSD_SPEC";
            case "PLAN" -> "CREATE_SSD_PLAN";
            case "TASK" -> "CREATE_SSD_TASK";
            case "IMPL" -> "CREATE_SSD_IMPL";
            default -> null;
        };
    }

}

