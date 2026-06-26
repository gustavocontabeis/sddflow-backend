package com.example.springia.agent.model;

public record ProcessResult(
        int exitCode,
        boolean timedOut,
        String stdout,
        String stderr,
        String command,
        String workingDirectory
) {
    public boolean success() {
        return exitCode == 0 && !timedOut;
    }
}

