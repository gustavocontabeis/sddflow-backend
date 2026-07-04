package com.example.springia.agent.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.ResponseCreateParams;

import com.azure.identity.AuthenticationUtil;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.openai.credential.BearerTokenCredential;

/**
 * **Responsabilidade:** Gera código Java usando modelo gpt-5.3-codex com Responses API usando `com.openai.client.OpenAIClient`
 */
@Slf4j
@Component
public class CodeGeneratorOpenApiAgent {

    private static final String SYSTEM_PROMPT_RESOURCE_PATH = "prompts/system-prompt.md";

    public void executar(String userPrompt) {

        String endpoint = System.getenv().getOrDefault("AZURE_AI_PROJECT_ENDPOINT", "");
        String apiKey = System.getenv().getOrDefault("SPRING_AI_OPENAI_API_KEY", "");
        String deploymentName = System.getenv().getOrDefault("AZURE_OPENAI_DEPLOYMENT", "gpt-5.3-codex");

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(endpoint)
                // Set the Azure Entra ID
                .credential(BearerTokenCredential.create(AuthenticationUtil.getBearerTokenSupplier(
                        new DefaultAzureCredentialBuilder().build(), "https://ai.azure.com/.default")))
                .build();

        ResponseCreateParams.Builder paramsBuilder = ResponseCreateParams.builder()
                .model(deploymentName)
                .input("What's the capital of France?");

        ResponseCreateParams createParams = paramsBuilder.build();

        client.responses().create(createParams).output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .forEach(outputText -> System.out.println(outputText.text()));
    }

}
