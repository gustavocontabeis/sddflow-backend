package com.example.springia.agent.tool.process;

import com.example.springia.agent.model.ProcessResult;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.List;

public interface ProcessExecutor {

    ProcessResult execute(List<String> command, Path workingDirectory, Map<String, String> environment, Duration timeout);
}

