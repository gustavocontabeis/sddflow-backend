package com.example.springia.agent.tool.diff;

import com.example.springia.agent.model.CodeDiffSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Gera resumo simples de diff entre conteudo anterior e posterior.
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.diff.CodeDiffTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Slf4j
@Component
public class CodeDiffTool {

    @Tool(name = "code_diff", description = "Calcula um resumo simples de diff entre conteudo anterior e posterior")
    public CodeDiffSummary summarize(
            @ToolParam(description = "Caminho do arquivo analisado") String filePath,
            @ToolParam(description = "Conteudo antes da alteracao") String before,
            @ToolParam(description = "Conteudo depois da alteracao") String after
    ) {
        log.info("{[CODE_DIFF]} calculando diff de {}", filePath);
        List<String> beforeLines = splitLines(before);
        List<String> afterLines = splitLines(after);

        int beforeSize = beforeLines.size();
        int afterSize = afterLines.size();
        int added = Math.max(0, afterSize - beforeSize);
        int removed = Math.max(0, beforeSize - afterSize);
        String summary = "linhas antes=" + beforeSize + ", depois=" + afterSize + ", adicionadas=" + added + ", removidas=" + removed;
        return new CodeDiffSummary(filePath, beforeSize, afterSize, added, removed, summary);
    }

    private List<String> splitLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        return content.lines().filter(Objects::nonNull).toList();
    }
}

