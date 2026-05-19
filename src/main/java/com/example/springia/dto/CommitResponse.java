package com.example.springia.dto;
public record CommitResponse(
String sha,
String message,
String author,
String url
) {
}
