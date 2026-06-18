package com.example.springia.agent.tool.discovery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryToolTest {

    @Test
    void fallbackCriteriaShouldInferClassAndAttributeFromSimplePortugueseQuestion() {
        DiscoveryTool.SearchCriteria criteria = DiscoveryTool.fallbackCriteria(
                "Do projeto ID 4, Tarefa tem prioridade?",
                ".java"
        );

        assertEquals("Tarefa", criteria.targetClass());
        assertEquals("prioridade", criteria.targetAttribute());
        assertEquals("prioridade", criteria.searchKeyword());
        assertEquals("prioridade", criteria.searchPattern());
        assertEquals(".java", criteria.fileExtensions());
    }

    @Test
    void extractKeywordsShouldIgnoreStopWordsAndNumericTokens() {
        List<String> keywords = DiscoveryTool.extractKeywords(
                "No projeto 4, a classe Tarefa possui prioridade e status?"
        );

        assertEquals(List.of("tarefa", "prioridade", "status"), keywords);
    }

    @Test
    void normalizeExtensionsShouldPrefixDotsRemoveDuplicatesAndKeepOrder() {
        String normalized = DiscoveryTool.normalizeExtensions("java, .xml,java,.SQL", ".java");

        assertEquals(".java,.xml,.sql", normalized);
    }

    @Test
    void extractJsonObjectShouldReturnOnlyJsonPayload() {
        String json = DiscoveryTool.extractJsonObject("texto antes {\"search_keyword\":\"prioridade\"} texto depois");

        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.contains("prioridade"));
    }
}

