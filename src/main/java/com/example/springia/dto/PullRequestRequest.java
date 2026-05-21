package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PullRequestRequest {
    private String owner;
    private String repo;
    private String title;
    private String description;
    private String headBranch;
    private String baseBranch;
}
