package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpecificationResponse {
    private Long id;
    private Long sessionId;
    private String content;
    private LocalDateTime generatedAt;
}
