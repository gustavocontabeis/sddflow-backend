package com.example.springia.scanner.service;

import com.example.springia.scanner.model.CodeFile;
import com.example.springia.scanner.model.CodeType;
import com.example.springia.scanner.model.Language;
import com.example.springia.scanner.model.StackDiscoveryReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Descobre stack tecnologica do projeto com base no output do scanner.
 */
@Service
@RequiredArgsConstructor
public class StackDiscoveryService {

    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("<java.version>([^<]+)</java.version>");
    private static final Pattern SPRING_BOOT_PARENT_VERSION_PATTERN = Pattern.compile(
        "<parent>[\\s\\S]*?<artifactId>spring-boot-starter-parent</artifactId>[\\s\\S]*?<version>([^<]+)</version>[\\s\\S]*?</parent>");
    private static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile("<artifactId>([^<]+)</artifactId>");

    private final RepositoryScannerService scannerService;

    public StackDiscoveryReport discover(String rootPath) {
        List<CodeFile> chunks = scannerService.scan(rootPath);
        Map<String, AssembledFile> files = assembleFiles(chunks);

        Map<String, Long> languageFileCount = files.values().stream()
            .collect(Collectors.groupingBy(
                file -> file.language().getValue(),
                LinkedHashMap::new,
                Collectors.counting()
            ));

        Map<String, Long> codeTypeFileCount = files.values().stream()
            .collect(Collectors.groupingBy(
                file -> file.type().getValue(),
                LinkedHashMap::new,
                Collectors.counting()
            ));

        String primaryLanguage = languageFileCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("unknown");

        LinkedHashSet<String> manifestsFound = new LinkedHashSet<>();
        LinkedHashSet<String> buildTools = new LinkedHashSet<>();
        LinkedHashSet<String> packageManagers = new LinkedHashSet<>();
        LinkedHashSet<String> frameworks = new LinkedHashSet<>();
        LinkedHashSet<String> libraries = new LinkedHashSet<>();
        LinkedHashSet<String> testLibraries = new LinkedHashSet<>();
        LinkedHashSet<String> databases = new LinkedHashSet<>();
        LinkedHashSet<String> cloudServices = new LinkedHashSet<>();

        String javaVersion = null;
        String springBootVersion = null;

        for (Map.Entry<String, AssembledFile> entry : files.entrySet()) {
            String path = entry.getKey();
            String filename = fileName(path);
            String content = entry.getValue().content();

            if ("pom.xml".equalsIgnoreCase(filename)) {
                manifestsFound.add("pom.xml");
                buildTools.add("Maven");
                packageManagers.add("Maven Central");

                javaVersion = firstMatch(JAVA_VERSION_PATTERN, content, javaVersion);
                springBootVersion = firstMatch(SPRING_BOOT_PARENT_VERSION_PATTERN, content, springBootVersion);

                analyzePomDependencies(content, frameworks, libraries, testLibraries, databases, cloudServices);
            }

            if ("package.json".equalsIgnoreCase(filename)) {
                manifestsFound.add("package.json");
                packageManagers.add("npm");
            }

            if ("composer.json".equalsIgnoreCase(filename)) {
                manifestsFound.add("composer.json");
                packageManagers.add("Composer");
            }

            if ("go.mod".equalsIgnoreCase(filename)) {
                manifestsFound.add("go.mod");
                packageManagers.add("Go Modules");
            }

            if ("pyproject.toml".equalsIgnoreCase(filename) || "requirements.txt".equalsIgnoreCase(filename)) {
                manifestsFound.add(filename);
                packageManagers.add("pip");
            }
        }

        // Sinais via codigo, independente de manifesto
        for (AssembledFile file : files.values()) {
            String content = file.content().toLowerCase();
            if (content.contains("@restcontroller") || content.contains("@controller")) {
                frameworks.add("Spring MVC");
            }
            if (content.contains("@entity")) {
                libraries.add("JPA/Hibernate");
            }
            if (content.contains("org.springframework.ai") || content.contains("spring.ai")) {
                frameworks.add("Spring AI");
            }
        }

        return StackDiscoveryReport.builder()
            .rootPath(rootPath)
            .totalFiles(files.size())
            .totalChunks(chunks.size())
            .primaryLanguage(primaryLanguage)
            .languageFileCount(sortByValueDesc(languageFileCount))
            .codeTypeFileCount(sortByValueDesc(codeTypeFileCount))
            .javaVersion(javaVersion)
            .springBootVersion(springBootVersion)
            .manifestsFound(new ArrayList<>(manifestsFound))
            .buildTools(new ArrayList<>(buildTools))
            .packageManagers(new ArrayList<>(packageManagers))
            .frameworks(new ArrayList<>(frameworks))
            .libraries(new ArrayList<>(libraries))
            .testLibraries(new ArrayList<>(testLibraries))
            .databases(new ArrayList<>(databases))
            .cloudServices(new ArrayList<>(cloudServices))
            .build();
    }

    private Map<String, AssembledFile> assembleFiles(List<CodeFile> chunks) {
        return chunks.stream()
            .collect(Collectors.groupingBy(CodeFile::getPath))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<CodeFile> ordered = entry.getValue().stream()
                        .sorted(Comparator.comparingInt(c -> c.getChunkNumber() == null ? Integer.MAX_VALUE : c.getChunkNumber()))
                        .toList();

                    String content = ordered.stream()
                        .map(CodeFile::getContent)
                        .collect(Collectors.joining("\n"));

                    Language language = ordered.isEmpty() ? Language.UNKNOWN : ordered.getFirst().getLanguage();
                    CodeType type = ordered.isEmpty() ? CodeType.UNKNOWN : ordered.getFirst().getType();

                    return new AssembledFile(language, type, content);
                },
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private void analyzePomDependencies(
        String pomContent,
        Set<String> frameworks,
        Set<String> libraries,
        Set<String> testLibraries,
        Set<String> databases,
        Set<String> cloudServices
    ) {
        Matcher matcher = ARTIFACT_ID_PATTERN.matcher(pomContent);
        while (matcher.find()) {
            String artifactId = matcher.group(1).trim();
            String normalized = artifactId.toLowerCase();

            if (normalized.startsWith("spring-boot-starter")) {
                frameworks.add("Spring Boot");
            }
            if (normalized.contains("spring-ai")) {
                frameworks.add("Spring AI");
            }
            if (normalized.contains("spring-cloud-azure")) {
                cloudServices.add("Azure Spring Cloud");
            }
            if (normalized.contains("starter-data-jpa")) {
                libraries.add("Spring Data JPA");
            }
            if (normalized.equals("h2")) {
                databases.add("H2 Database");
            }
            if (normalized.equals("lombok")) {
                libraries.add("Lombok");
            }
            if (normalized.contains("actuator")) {
                libraries.add("Spring Boot Actuator");
            }
            if (normalized.endsWith("-test") || normalized.contains("junit") || normalized.contains("mockito")) {
                testLibraries.add(artifactId);
            }
        }
    }

    private String firstMatch(Pattern pattern, String content, String currentValue) {
        if (currentValue != null) {
            return currentValue;
        }
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private Map<String, Long> sortByValueDesc(Map<String, Long> source) {
        return source.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private String fileName(String path) {
        int idx = path.lastIndexOf('/');
        if (idx < 0) {
            idx = path.lastIndexOf('\\');
        }
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private record AssembledFile(Language language, CodeType type, String content) {
    }
}

