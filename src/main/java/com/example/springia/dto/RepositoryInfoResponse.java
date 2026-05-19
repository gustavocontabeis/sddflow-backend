package com.example.springia.dto;
public record RepositoryInfoResponse(
String name,
String url,
String description,
int stars,
int forks
) {
}
