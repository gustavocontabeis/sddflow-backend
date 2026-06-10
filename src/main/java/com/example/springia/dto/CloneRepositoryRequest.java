package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CloneRepositoryRequest {
    private String owner;
    private String repo;
    private String branch;
}
