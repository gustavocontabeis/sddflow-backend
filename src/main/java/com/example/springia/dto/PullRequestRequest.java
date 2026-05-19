package com.example.springia.dto;
public record PullRequestRequest(
String owner,
String repo,
String title,
String description,
String headBranch,
String baseBranch
) {
}
