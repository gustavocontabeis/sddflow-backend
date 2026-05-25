package com.example.springia.agent.tool;

import java.util.Map;

/**
 * Interface para ferramentas que podem ser executadas pelo Agent.
 */
public interface Tool {

    /**
     * Retorna o nome da ferramenta (usado pelo LLM para selecioná-la)
     */
    String getName();

    /**
     * Retorna a descrição da ferramenta
     */
    String getDescription();

    /**
     * Retorna os parâmetros esperados (nome -> tipo/descrição)
     */
    Map<String, String> getParameters();

    /**
     * Executa a ferramenta com os parâmetros fornecidos
     */
    String execute(Map<String, String> params) throws Exception;
}

