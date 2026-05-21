package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryInfoResponse {
    private String name;
    private String url;
    private String description;
    private int stars;
    private int forks;
}
