package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PullRequestResponse {
    private int number;
    private String title;
    private String state;
    private String htmlUrl;
}
