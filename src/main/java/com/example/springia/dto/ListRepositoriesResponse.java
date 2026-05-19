package com.example.springia.dto;
import java.util.List;
public record ListRepositoriesResponse(
List<RepositoryInfoResponse> repositories,
int total
) {
}
