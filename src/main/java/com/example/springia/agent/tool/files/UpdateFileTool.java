package com.example.springia.agent.tool.files;

import com.example.springia.agent.tool.Tool;
import com.example.springia.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Ferramenta para editar arquivos existentes sem sobrescrever todo o conteúdo.
 */
@Slf4j
public class UpdateFileTool implements Tool {

    private final String basePath;

    public UpdateFileTool(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public String getName() {
        return "update_file";
    }

    @Override
    public String getDescription() {
        return "Atualiza arquivo existente substituindo trecho específico (old_text -> new_text)";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("file_path", "Caminho do arquivo a alterar (caminho absoluto) OBRIGATÒRIO");
        params.put("old_text", "Trecho exato atual a ser substituído OBRIGATÒRIO");
        params.put("new_text", "Novo trecho que substituirá old_text");
        params.put("replace_all", "Opcional: true para substituir todas ocorrências; padrão false");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String filePath = params.get("file_path");
        String oldText = params.get("old_text");
        String newText = params.get("new_text");
        boolean replaceAll = Boolean.parseBoolean(params.getOrDefault("replace_all", "false"));

        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("file_path é obrigatório");
        }
        if (oldText == null || oldText.isEmpty()) {
            throw new IllegalArgumentException("old_text é obrigatório e não pode ser vazio");
        }
        if (newText == null) {
            newText = "";
        }

        String fullPath = basePath + "/" + filePath;
        var path = Paths.get(FileUtils.fixPath(fullPath));

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Arquivo não encontrado: " + path);
        }

        String original = Files.readString(path);
        int occurrences = countOccurrences(original, oldText);
        if (occurrences == 0) {
            throw new IllegalArgumentException("old_text não encontrado no arquivo: " + path);
        }

        String updated;
        int replaced;
        if (replaceAll) {
            updated = original.replace(oldText, newText);
            replaced = occurrences;
        } else {
            int idx = original.indexOf(oldText);
            updated = original.substring(0, idx) + newText + original.substring(idx + oldText.length());
            replaced = 1;
        }

        Files.writeString(path, updated);
        log.info("[TOOL] Arquivo atualizado: {} ({} ocorrência(s) substituída(s))", path, replaced);
        return "Arquivo atualizado com sucesso: " + path + " (" + replaced + " ocorrência(s))";
    }

    private int countOccurrences(String content, String snippet) {
        int count = 0;
        int idx = 0;
        while (true) {
            idx = content.indexOf(snippet, idx);
            if (idx < 0) {
                return count;
            }
            count++;
            idx += snippet.length();
        }
    }
}

