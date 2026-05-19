package com.example.springia.dto;
public record CommitRequest(
String owner,
String repo,
String branch,
String message,
String filePath,
String fileContent
) {
}
