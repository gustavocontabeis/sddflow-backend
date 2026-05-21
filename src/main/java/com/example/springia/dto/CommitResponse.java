package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitResponse {
    private String sha;
    private String message;
    private String author;
    private String url;
}
