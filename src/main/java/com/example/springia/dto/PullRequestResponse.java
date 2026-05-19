package com.example.springia.dto;
public record PullRequestResponse(
int number,
String title,
String state,
String htmlUrl
) {
}
