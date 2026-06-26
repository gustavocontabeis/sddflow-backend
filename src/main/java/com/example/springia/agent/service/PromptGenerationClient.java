package com.example.springia.agent.service;

import com.example.springia.agent.model.CompilationResult;
import com.example.springia.agent.model.CompileBy;
import com.example.springia.agent.model.FileChangeCommand;
import com.example.springia.agent.model.FileWriteResult;
import com.example.springia.agent.model.GeneratedChangeSet;
import com.example.springia.agent.model.ProjectDiscoverySnapshot;

import java.util.List;

public interface PromptGenerationClient {

    ProjectDiscoverySnapshot discover();

    String buildInitialPrompt(String taskDescription, ProjectDiscoverySnapshot discovery);

    String buildRepairPrompt(String taskDescription, ProjectDiscoverySnapshot discovery, String previousResponse, String feedback);

    String generate(String prompt);

    void validateScope(ProjectDiscoverySnapshot discovery, GeneratedChangeSet changeSet);

    List<FileWriteResult> writeChanges(List<FileChangeCommand> changes);

    List<CompilationResult> compile(CompileBy compileBy);

    String buildFeedback(List<CompilationResult> compilationResults, String previousResponse, String repairHint);

    boolean isSuccessful(List<CompilationResult> compilationResults);
}

