package com.example.springia.agent.client;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * **Responsabilidade:** Gera código Java usando modelo gpt-5.3-codex com Responses API usando `com.openai.client.OpenAIClient`
 */
@Slf4j
@Component
public class CodeGeneratorOpenApiAgent {

    private static final String SYSTEM_PROMPT_RESOURCE_PATH = "prompts/system-prompt.md";

    public void executar(String userPrompt) {
    }

}