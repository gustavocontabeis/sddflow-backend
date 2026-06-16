package com.example.springia.serviceagent;

import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SddPlanServiceAgent {

    private final ChatClient chatClient;

    public SddPlanServiceAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String validarRepositorio(Project project, String input){
        StringBuilder reposContext = new StringBuilder();
        if (project.getRepos() != null && !project.getRepos().isEmpty()) {
            for (CodeRepo repo : project.getRepos()) {
                reposContext.append("\n--- REPOSITORIO ---\n")
                        .append("Nome: ").append(repo.getName()).append("\n")
                        .append("Tipo: ").append(repo.getType()).append("\n")
                        .append("Diretório: ").append(repo.getPath().replaceAll("\\d+$", "")).append("\n")
                        .append("Estrutura: ").append(repo.getStructure()).append("\n");
            }
        }

        String prompt = String.format("""
                Garanta que a estrutura dos arquivos está dentro do(s) diretório(s) dos repositórios existentes.
                Retorne apenas o prompt original com as correções se necessário.
                Altere somente o necessário.
                --------------------- DIRETÓRIOS ---------------------
                %s
                --------------------- PROMPT ORIGINAL ---------------------
                %s
                """, reposContext, input);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
