package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloneRepositoryResponse {
    private String owner;
    private String repo;
    private String branch;
    private String clonedPath;
    private long durationMs;
    private String sufixoNumerico;
}
