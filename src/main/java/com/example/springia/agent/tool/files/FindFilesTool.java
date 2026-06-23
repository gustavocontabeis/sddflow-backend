package com.example.springia.agent.tool.files;

import com.example.springia.agent.tool.Tool;
import com.example.springia.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ferramenta para listar arquivos e diretórios
 */
@Slf4j
public class FindFilesTool implements Tool {

    private final String basePath;

    public FindFilesTool(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public String getName() {
        return "find_files";
    }

    @Override
    public String getDescription() {
        return "Busca arquivos por nome, recursivamente, a partir de um diretório específico";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("directory_path", "Path do diretório raiz da busca - OBRIGATORIO");
        params.put("file_name", "Nome do arquivo para busca exata (ignora maiúsculas/minúsculas) - OBRIGATORIO");
        return params;
    }

    @Override
    public String execute(Map<String, String> params) throws Exception {
        String dirPath = params.getOrDefault("directory_path", "").trim();
        String fileName = params.getOrDefault("file_name", "").trim();

        if (dirPath.isBlank()) {
            throw new IllegalArgumentException("directory_path é obrigatório");
        }

        if (fileName.isBlank()) {
            throw new IllegalArgumentException("file_name é obrigatório");
        }

        String fullPath = basePath + "/" + dirPath;

        var path = Paths.get(FileUtils.fixPath(fullPath));

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Diretório não encontrado: " + dirPath);
        }

        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Caminho não é um diretório: " + dirPath);
        }

        List<String> itens;
        try (var stream = Files.walk(path)) {
            var normalizedFileName = fileName.toLowerCase();

            itens = stream
                    .filter(Files::isRegularFile)
                    .filter(current -> current.getFileName().toString().toLowerCase().equals(normalizedFileName))
                    .sorted()
                    .map(current -> "[FILE] " + path.relativize(current).toString().replace("\\", "/"))
                    .collect(Collectors.toList());
        }

        String result = String.join("\n", itens);
        log.info("[TOOL] Busca por arquivo '{}' em {} retornou {} itens", fileName, path, itens.size());
        return result;
    }
}
