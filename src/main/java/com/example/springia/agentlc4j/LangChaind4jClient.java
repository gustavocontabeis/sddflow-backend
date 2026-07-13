package com.example.springia.agentlc4j;

import dev.langchain4j.model.openai.OpenAiResponsesChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LangChaind4jClient {

    public static final String USER_PROMPT = "Olá. Como está a copa do mundo de 2026?";
    public static final String AZURE_AI_OPENAI_API_KEY = "SPRING_AI_OPENAI_API_KEY";
    public static final String AZURE_AI_PROJECT_ENDPOINT = "AZURE_AI_PROJECT_ENDPOINT";
    public static final String SPRING_AI_OPENAI_BASE_URL = "SPRING_AI_OPENAI_BASE_URL";
    public static final String SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL = "SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL";

    public void dev(){

        OpenAiResponsesChatModel model = OpenAiResponsesChatModel.builder()
                .baseUrl(System.getenv(SPRING_AI_OPENAI_BASE_URL))
                .apiKey(System.getenv(AZURE_AI_OPENAI_API_KEY))
                .modelName(System.getenv(SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL))
                .temperature(0.2)
                .build();

        log.info("{}", model.chat(USER_PROMPT));

        Assistant assistant = AiServices.create(Assistant.class, model);

        String resposta = assistant.chat("Olá");

        log.info("{}", resposta);
    }
}
