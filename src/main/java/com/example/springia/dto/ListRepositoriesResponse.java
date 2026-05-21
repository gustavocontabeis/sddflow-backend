package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListRepositoriesResponse {
    private List<RepositoryInfoResponse> repositories;
    private int total;
}
