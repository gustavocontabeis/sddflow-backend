package com.example.springia.agent.tool.files;

import com.example.springia.agent.tool.Tool;
import com.example.springia.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return "Atualiza arquivo existente substituindo trecho específico (old_text -> new_text), com tolerância a diferenças de espaços/indentação";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("file_path", "Caminho do arquivo a alterar (caminho absoluto) OBRIGATÒRIO");
        params.put("old_text", "Trecho atual a ser substituído (copie do read_file; deve existir no arquivo) OBRIGATÒRIO");
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
        String updated;
        int replaced;
        int occurrences = countOccurrences(original, oldText);
        if (occurrences > 0) {
            if (replaceAll) {
                updated = original.replace(oldText, newText);
                replaced = occurrences;
            } else {
                int idx = original.indexOf(oldText);
                updated = original.substring(0, idx) + newText + original.substring(idx + oldText.length());
                replaced = 1;
            }
        } else {
            // Fallback: permite pequenas diferenças de whitespace entre old_text e arquivo.
            FlexibleMatchResult flexible = findFlexibleMatches(original, oldText);
            if (flexible.matches() == 0) {
                throw new IllegalArgumentException(buildOldTextNotFoundMessage(path, original));
            }

            if (replaceAll) {
                updated = flexible.pattern().matcher(original).replaceAll(Matcher.quoteReplacement(newText));
                replaced = flexible.matches();
            } else {
                int start = flexible.firstStart();
                int end = flexible.firstEnd();
                updated = original.substring(0, start) + newText + original.substring(end);
                replaced = 1;
            }

            log.info("[TOOL] update_file aplicou fallback flexível de whitespace para {}", path);
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

    private FlexibleMatchResult findFlexibleMatches(String content, String snippet) {
        String trimmed = snippet.trim();
        if (trimmed.isEmpty()) {
            return new FlexibleMatchResult(0, -1, -1, Pattern.compile("$^"));
        }

        String[] tokens = trimmed.split("\\s+");
        List<String> escapedTokens = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            if (!token.isEmpty()) {
                escapedTokens.add(Pattern.quote(token));
            }
        }

        if (escapedTokens.isEmpty()) {
            return new FlexibleMatchResult(0, -1, -1, Pattern.compile("$^"));
        }

        String regex = String.join("\\s+", escapedTokens);
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        int matches = 0;
        int firstStart = -1;
        int firstEnd = -1;
        while (matcher.find()) {
            if (firstStart < 0) {
                firstStart = matcher.start();
                firstEnd = matcher.end();
            }
            matches++;
        }

        return new FlexibleMatchResult(matches, firstStart, firstEnd, pattern);
    }

    private String buildOldTextNotFoundMessage(java.nio.file.Path path, String original) {
        String preview = original.length() <= 600 ? original : original.substring(0, 600) + "\n...";
        return "old_text não encontrado no arquivo: " + path
                + ". Use read_file para ler o arquivo e copie um trecho literal para old_text."
                + " Prévia do início do arquivo:\n" + preview;
    }

    private record FlexibleMatchResult(int matches, int firstStart, int firstEnd, Pattern pattern) {}
}

