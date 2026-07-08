package com.example.springia.utils;

import com.example.springia.agent.responseapi.request.RequestToolDefinition;
import com.example.springia.agent.responseapi.request.RequestToolProperty;
import com.openai.core.JsonValue;
import com.openai.models.responses.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class OpenAIUtils {

    public static List<FunctionTool> montarTools(RequestToolDefinition...toolsDefinitions) {
        List<FunctionTool> tools = new ArrayList<>();
        for (RequestToolDefinition definition : toolsDefinitions) {
            tools.add(toFunctionTool(definition));
        }
        return tools;

    }

    private static FunctionTool toFunctionTool(RequestToolDefinition definition) {
        log.debug("[TO_FUNCTION] Convertendo definição da tool: {}", definition.getName());

        if (definition.getParameters() == null || definition.getParameters().getType() == null) {
            throw new IllegalArgumentException("Definição de parâmetros inválida para tool: " + definition.getName());
        }

        Map<String, Object> properties = new HashMap<>();
        if (definition.getParameters() != null && definition.getParameters().getProperties() != null) {
            for (Map.Entry<String, RequestToolProperty> entry : definition.getParameters().getProperties().entrySet()) {
                RequestToolProperty value = entry.getValue();
                properties.put(entry.getKey(), Map.of(
                        "type", value.getType(),
                        "description", value.getDescription()
                ));
            }
        }

        FunctionTool.Parameters parameters = FunctionTool.Parameters.builder()
                .putAdditionalProperty("type", JsonValue.from(definition.getParameters().getType()))
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .putAdditionalProperty("required", JsonValue.from(definition.getParameters().getRequired()))
                .putAdditionalProperty("additionalProperties", JsonValue.from(definition.getParameters().getAdditionalProperties()))
                .build();

        return FunctionTool.builder()
                .name(definition.getName())
                .description(definition.getDescription())
                .parameters(parameters)
                .strict(Boolean.TRUE.equals(definition.getStrict()))
                .build();
    }

    public static String extrairResposta(Response response) {
        log.debug("[EXTRACT_TEXT] Extraindo texto da resposta id={}", response != null ? response.id() : null);

        if (response == null || response.output().isEmpty()) {
            return "";
        }

        StringBuilder outputText = new StringBuilder();
        for (ResponseOutputItem outputItem : response.output()) {
            Optional<ResponseOutputMessage> outputMessage = outputItem.message();
            if (outputMessage.isEmpty()) {
                continue;
            }

            for (ResponseOutputMessage.Content content : outputMessage.get().content()) {
                Optional<ResponseOutputText> textOptional = content.outputText();
                if (textOptional.isPresent()) {
                    if (!outputText.isEmpty()) {
                        outputText.append("\n");
                    }
                    outputText.append(textOptional.get().text());
                }
            }
        }

        return outputText.toString();
    }

}
