package com.example.springia.agent.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedChangeSet(
        String summary,
        String notes,
        List<CompileBy> compilationTargets,
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @JsonAlias("files")
        List<FileChangeCommand> changes
) {

    public GeneratedChangeSet {
        compilationTargets = compilationTargets == null ? List.of() : compilationTargets;
        changes = changes == null ? List.of() : changes;
    }
}

