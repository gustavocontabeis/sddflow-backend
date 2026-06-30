package com.example.springia.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class CodeGeneratorAgent {

    private final RestClient restClient;
    private final String apiKey;
    private final String baseUrl;

    public CodeGeneratorAgent(
            RestClient.Builder restClientBuilder,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.restClient = restClientBuilder
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String generateJavaCode(String specification) {
        log.info("[CODE_GEN] Iniciando geração com Responses API");

        String requestBody = buildResponsesApiRequest(specification);

        try {
            String response = restClient.post()
                    .uri(baseUrl + "/responses")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info("[CODE_GEN] Resposta recebida com sucesso");
            return extractCodeFromResponse(response);

        } catch (Exception e) {
            log.error("[CODE_GEN] Erro ao chamar Responses API", e);
            throw new RuntimeException("Falha ao gerar código", e);
        }
    }

    private String buildResponsesApiRequest(String specification) {
        // Formato específico para Responses API do Azure OpenAI
        return """
        {
            "model": "gpt-5.3-codex",
            "messages": [
                {
                    "role": "user",
                    "content": "Gere código Java seguindo: %s"
                }
            ],
            "temperature": 0.7,
            "max_tokens": 4000
        }
        """.formatted(specification);
    }

    private String extractCodeFromResponse(String response) {
        // Parse JSON response e extrai o conteúdo
        // Implementação depende da estrutura real da Responses API
        return response;
    }
}