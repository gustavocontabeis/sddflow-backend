package com.example.springia.service;

import com.example.springia.dto.ImplSddValidationDto;
import com.example.springia.dto.PromptAuditResponse;
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
    private ImplSddService implSddService;

    @Autowired
    private PromptService promptService;

    public PromptAuditResponse analyzePrompt(Long userStoryId, String question) {
        log.info("Iniciando analise de prompt para userStoryId={} questionLength={}",
                userStoryId,
                question != null ? question.length() : 0);

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

        String auditPrompt = buildPromptAuditPrompt(userStory, question);
        log.info("PROMPT_AUDIT userStoryId={} promptLength={}, prompt={}", userStoryId, auditPrompt.length(), auditPrompt);

        String rawResponse = chatService.chat(auditPrompt);
        log.info("PROMPT_AUDIT resposta recebida userStoryId={} responseLength={}",
                userStoryId,
                rawResponse != null ? rawResponse.length() : 0);

        return parsePromptAuditResponse(userStoryId, question, rawResponse);
    }

    public String createSpec(Long userStoryId) {
        log.info("Iniciando geracao de SPEC para userStoryId={}", userStoryId);

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

        String prompt = loadPromptContent("CREATE_SSD_SPEC");

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

        String prompt = loadPromptContent("CREATE_SSD_PLAN");

        ConversationSession conversationSession = userStory.getConversationSession();
        String projectConstitution = conversationSession.getProject().getConstitution();

        prompt = prompt
                .replace("{{CONSTITUTION}}", projectConstitution)
                .replace("{{USER_STORY}}", userStory.getContent())
                .replace("{{SDD_SPEC}}", userStory.getSpec().getContent());

        log.info("PLAN promptLength={}, prompt:={}", prompt.length(), prompt);

        String plan = chatService.chat(prompt);

        planSddService.savePlan(userStory, plan);

        log.info("PLAN gerada e salva com sucesso para userStoryId={}, specLength={}", userStoryId, plan.length());

        return plan;
    }

    public String createTask(Long userStoryId) {
        log.info("Iniciando geracao de TASK para userStoryId={}", userStoryId);

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

        String prompt = loadPromptContent("CREATE_SSD_TASK");

        ConversationSession conversationSession = userStory.getConversationSession();
        String projectConstitution = conversationSession.getProject().getConstitution();

        prompt = prompt
                .replace("{{CONSTITUTION}}", projectConstitution)
                .replace("{{SDD_SPEC}}", userStory.getSpec().getContent())
                .replace("{{SDD_PLAN}}", userStory.getPlan().getContent());

        log.info("TASK promptLength={}, promptLenght:={}", prompt.split(" ").length, prompt);

        String content = chatService.chat(prompt);

        taskSddService.saveTask(userStory, content);

        log.info("TASK gerada e salva com sucesso para userStoryId={}, tokenLength={}", userStoryId, content.split(" ").length);

        return content;
    }

    public String createImpl(Long userStoryId) {
        log.info("Iniciando geracao de IMPL para userStoryId={}", userStoryId);

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new IllegalArgumentException("UserStory nao encontrada: " + userStoryId));

        String prompt = loadPromptContent("CREATE_SSD_IMPL");

        ConversationSession conversationSession = userStory.getConversationSession();
        String projectConstitution = conversationSession.getProject().getConstitution();

        prompt = prompt
                .replace("{{CONSTITUTION}}", projectConstitution)
                .replace("{{SDD_SPEC}}", userStory.getSpec().getContent())
                .replace("{{SDD_PLAN}}", userStory.getPlan().getContent())
                .replace("{{SDD_TASK}}", userStory.getTask().getContent());

        log.info("[CREATE IMPL] promptLength={}", prompt.split(" ").length);
        log.debug("[CREATE IMPL] {}", prompt);

        String content = chatService.chat(prompt);

        int i = 0;
        ImplSddValidationDto implSddValidationDto = ImplSddValidationDto.builder().build();
        do{
            implSddValidationDto = validarCodigoGerado(content);
        } while (implSddValidationDto.isValid() && i++ <=3);

        implSddService.save(ImplSdd.builder().id(null).status(SpecificationDocumentStatus.IN_PROGRESS).userStory(userStory).content(content).build());

        log.info("[CREATE IMPL] gerada e salva com sucesso para userStoryId={}, tokenLength={}", userStoryId, content.split(" ").length);

        return content;
    }

    private ImplSddValidationDto validarCodigoGerado(String content) {
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
                    """//.replace("{{CONSTITUTION}}", projectConstitution)
                .replace("{{PROMPT}}", content);

        ImplSddValidationDto implSddValidationDto = chatService.getChatClient().prompt()
                .user(validationPrompt)
                .call()
                .entity(ImplSddValidationDto.class);

        log.info("[CREATE IMPL] erros de validação: {} ", implSddValidationDto.getProblems());

        if(!implSddValidationDto.isValid()){
            log.warn("[CREATE IMPL] Erro de validação. Tente novamente.");
            log.warn("{}", implSddValidationDto.getProblems());
            log.warn("[CREATE IMPL] conteúdo original \n{}", content);
            content = implSddValidationDto.getContent();
            log.warn("[CREATE IMPL] conteúdo corrigido \n{}", content);
        }
        return implSddValidationDto;
    }

    private String buildPromptAuditPrompt(UserStory userStory, String question) {
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

        return """
                Você é um analista sênior de prompts e rastreamento de erros.

                Objetivo: ler a pergunta e vasculhar quais fontes/prompts provavelmente gerou o resultado incorreto.
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
    }

    private PromptAuditResponse parsePromptAuditResponse(Long userStoryId, String question, String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
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

        return new PromptAuditResponse(userStoryId, question, source, promptKey, confidence, justification, excerpt, rawResponse);
    }

    private String loadPromptContent(String key) {
        return promptService.findByKey(key)
                .map(Prompt::getContent)
                .orElse("[PROMPT NAO ENCONTRADO: " + key + "]");
    }

    private String safeText(String text) {
        return text != null && !text.isBlank() ? text : "[CONTEUDO NAO INFORMADO]";
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
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

