package com.example.springia.agent.tool.discovery;

import com.example.springia.agent.model.ProjectDiscoveryReport;
import com.example.springia.agent.model.ProjectDiscoverySnapshot;
import com.example.springia.config.AgentProperties;
import com.example.springia.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Faz o discovery dos projetos backend e frontend.
 * {@code curl -X POST "http://localhost:8080/actuator/loggers/com.example.springia.agent.tool.discovery.ProjectDiscoveryTool" -H "Content-Type: application/json" -d '{"configuredLevel":"DEBUG"}'}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectDiscoveryTool {

    private final AgentProperties agentProperties;

    @Tool(name = "project_discovery", description = "Descobre a estrutura e os arquivos relevantes dos projetos backend e frontend")
    public ProjectDiscoverySnapshot discover() {
        log.info("{[DISCOVER]} iniciando discovery dos projetos");
        ProjectDiscoveryReport backend = discover(Path.of(agentProperties.getBackendRoot()), "backend");
        ProjectDiscoveryReport frontend = discover(Path.of(agentProperties.getFrontendRoot()), "frontend");
        String summary = buildSummary(backend, frontend);
        return new ProjectDiscoverySnapshot(backend, frontend, summary);
    }

    public ProjectDiscoveryReport discover(Path rootPath, String projectName) {
        log.info("{[DISCOVER]} analisando {} em {}", projectName, rootPath);
        boolean exists = Files.exists(rootPath);
        boolean directory = exists && Files.isDirectory(rootPath);

        if (!exists || !directory) {
            return new ProjectDiscoveryReport(rootPath, exists, directory, List.of(), List.of(), List.of(), List.of(), Map.of(), projectName + " indisponivel");
        }

        List<String> files = FileUtils.listFilesNames(rootPath);
        List<String> javaFiles = filter(files, ".java");
        List<String> resourceFiles = filter(files, ".properties", ".yml", ".yaml", ".xml", ".html", ".ts", ".scss", ".css", ".json");
        List<String> testFiles = files.stream().filter(file -> file.contains("/src/test/") || file.contains("Test.") || file.contains("IntegrationTest.") || file.contains("AcceptanceTest.")).toList();
        Map<String, Long> extensionCounts = countByExtension(files);
        String summary = buildProjectSummary(projectName, rootPath, files, javaFiles, resourceFiles, testFiles, extensionCounts);
        return new ProjectDiscoveryReport(rootPath, true, true, files, javaFiles, resourceFiles, testFiles, extensionCounts, summary);
    }

    private String buildSummary(ProjectDiscoveryReport backend, ProjectDiscoveryReport frontend) {
        return "BACKEND: " + backend.summary() + '\n' + "FRONTEND: " + frontend.summary();
    }

    private String buildProjectSummary(String projectName, Path rootPath, List<String> files, List<String> javaFiles, List<String> resourceFiles, List<String> testFiles, Map<String, Long> extensionCounts) {
        return projectName + " em " + rootPath + " com " + files.size() + " arquivos, " + javaFiles.size() + " java, " + resourceFiles.size() + " recursos e " + testFiles.size() + " testes" + ". Extensoes: " + extensionCounts;
    }

    private List<String> filter(List<String> files, String... extensions) {
        List<String> result = new ArrayList<>();
        for (String file : files) {
            for (String extension : extensions) {
                if (file.endsWith(extension)) {
                    result.add(file);
                    break;
                }
            }
        }
        return result;
    }

    private Map<String, Long> countByExtension(List<String> files) {
        Map<String, Long> counts = new LinkedHashMap<>();
        files.stream()
                .map(this::extractExtension)
                .sorted(Comparator.naturalOrder())
                .forEach(extension -> counts.merge(extension, 1L, Long::sum));
        return counts;
    }

    private String extractExtension(String file) {
        int index = file.lastIndexOf('.');
        return index < 0 ? "sem_extensao" : file.substring(index + 1).toLowerCase();
    }
}

