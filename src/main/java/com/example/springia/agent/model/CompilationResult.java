package com.example.springia.agent.model;

import java.time.Duration;

public record CompilationResult(
        CompileBy compileBy,
        String projectName,
        String projectRoot,
        String command,
        boolean success,
        boolean timedOut,
        int exitCode,
        Duration duration,
        String output,
        String errorOutput
) {
}

