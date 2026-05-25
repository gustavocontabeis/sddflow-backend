package com.example.springia.agent.tool;

import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * Registro de ferramentas disponíveis para o agente
 */
@Slf4j
public class ToolRegistry {

    private final Map<String, Tool> tools = new HashMap<>();

    public void registerTool(Tool tool) {
        tools.put(tool.getName(), tool);
        log.info("[TOOL REGISTRY] Tool registrada: {}", tool.getName());
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * Retorna uma descrição de todas as ferramentas disponíveis para usar no prompt
     */
    public String getToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ferramentas disponíveis:\n");

        for (Tool tool : tools.values()) {
            sb.append("\n## ").append(tool.getName()).append("\n");
            sb.append("Descrição: ").append(tool.getDescription()).append("\n");

            Map<String, String> params = tool.getParameters();
            if (!params.isEmpty()) {
                sb.append("Parâmetros:\n");
                params.forEach((key, value) ->
                    sb.append("  - ").append(key).append(": ").append(value).append("\n")
                );
            }
        }

        return sb.toString();
    }
}

