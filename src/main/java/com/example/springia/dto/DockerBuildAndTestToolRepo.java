package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DockerBuildAndTestToolRepo {
    private boolean success;
    private String logError;
    private String log;
    private String repoName;
}
