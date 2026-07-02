package com.example.springia.agent;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsFunctionToolDefinition;
import com.azure.ai.openai.models.ChatCompletionsFunctionToolDefinitionFunction;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatCompletionsToolSelection;
import com.azure.ai.openai.models.ChatCompletionsToolSelectionPreset;
import com.azure.ai.openai.models.ChatCompletionsFunctionToolCall;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestToolMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CodeGeneratorAzureSdkAgent {

    private static final String SYSTEM_PROMPT_RESOURCE_PATH = "prompts/system-prompt.md";


    public void executar(String userPrompt) {
        String endpoint = System.getenv().getOrDefault("AZURE_AI_PROJECT_ENDPOINT", "");
        String apiKey = System.getenv().getOrDefault("SPRING_AI_OPENAI_API_KEY", "");
        String deploymentName = System.getenv().getOrDefault("AZURE_OPENAI_DEPLOYMENT", "gpt-5.3-codex");
        String systemPrompt = readResourceFile(SYSTEM_PROMPT_RESOURCE_PATH);
        OpenAIClient client = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();

        // O SDK azure-ai-openai 1.0.0-beta.16 não expõe Responses API diretamente.
        // Este exemplo mostra o mesmo fluxo de tool calling usando ChatCompletions,
        // que é o caminho suportado nesta versão do OpenAIClient.
        ChatCompletionsOptions options = new ChatCompletionsOptions(List.of(
                new ChatRequestSystemMessage(systemPrompt),
                new ChatRequestUserMessage(userPrompt)
        ))
                .setModel(deploymentName)
                .setTools(List.of(criarToolHoraUtc()))
                .setToolChoice(new ChatCompletionsToolSelection(ChatCompletionsToolSelectionPreset.AUTO))
                .setParallelToolCalls(false)
                .setTemperature(0.2);

        ChatCompletions primeiraResposta = client.getChatCompletions(deploymentName, options);
        ChatChoice primeiraEscolha = primeiraResposta.getChoices().get(0);
        var primeiraMensagem = primeiraEscolha.getMessage();

        if (primeiraMensagem.getToolCalls() == null || primeiraMensagem.getToolCalls().isEmpty()) {
            System.out.println("Resposta final: " + primeiraMensagem.getContent());
            return;
        }

        ChatCompletionsFunctionToolCall toolCall = (ChatCompletionsFunctionToolCall) primeiraMensagem.getToolCalls().get(0);
        String resultadoTool = executarTool(toolCall);

        List<ChatRequestMessage> mensagensDeSeguimento = new ArrayList<>(options.getMessages());
        ChatRequestAssistantMessage mensagemAssistente = new ChatRequestAssistantMessage(primeiraMensagem.getContent())
                .setToolCalls(primeiraMensagem.getToolCalls());
        mensagensDeSeguimento.add(mensagemAssistente);
        mensagensDeSeguimento.add(new ChatRequestToolMessage(resultadoTool, toolCall.getId()));

        ChatCompletionsOptions segundaRequisicao = new ChatCompletionsOptions(mensagensDeSeguimento)
                .setModel(deploymentName)
                .setTools(List.of(criarToolHoraUtc()))
                .setToolChoice(new ChatCompletionsToolSelection(ChatCompletionsToolSelectionPreset.AUTO))
                .setParallelToolCalls(false)
                .setTemperature(0.2);

        ChatCompletions respostaFinal = client.getChatCompletions(deploymentName, segundaRequisicao);
        System.out.println("Resposta final: " + respostaFinal.getChoices().get(0).getMessage().getContent());
    }

    private static ChatCompletionsFunctionToolDefinition criarToolHoraUtc() {
        ChatCompletionsFunctionToolDefinitionFunction function = new ChatCompletionsFunctionToolDefinitionFunction("obter_hora_utc")
                .setDescription("Retorna a hora atual em UTC no formato ISO-8601")
                .setParameters(BinaryData.fromString("""
                        {
                          "type": "object",
                          "properties": {
                            "timezone": {
                              "type": "string",
                              "description": "Fuso horário IANA; use UTC neste exemplo"
                            }
                          },
                          "required": ["timezone"],
                          "additionalProperties": false
                        }
                        """))
                .setStrict(true);

        return new ChatCompletionsFunctionToolDefinition(function);
    }

    private static String executarTool(ChatCompletionsFunctionToolCall toolCall) {
        if (toolCall == null || toolCall.getFunction() == null) {
            throw new IllegalArgumentException("Tool call inválida");
        }

        if (!"obter_hora_utc".equals(toolCall.getFunction().getName())) {
            throw new IllegalArgumentException("Tool não suportada: " + toolCall.getFunction().getName());
        }

        return OffsetDateTime.now(ZoneOffset.UTC).toString();
    }

    private String readResourceFile(String resourcePath) {
        log.debug("[READ_RES] Lendo arquivo de resources: {}", resourcePath);

        try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Arquivo não encontrado em resources: " + resourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("[READ_RES] Erro ao ler arquivo de resources: {}", resourcePath, e);
            throw new RuntimeException("Falha ao ler arquivo de resources: " + resourcePath, e);
        }
    }

}