package com.example.springia.agent.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ProjectDiscoveryReport(
        Path rootPath,
        boolean exists,
        boolean directory,
        List<String> files,
        List<String> javaFiles,
        List<String> resourceFiles,
        List<String> testFiles,
        Map<String, Long> extensionCounts,
        String summary
) {
}

