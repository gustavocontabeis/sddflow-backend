package com.example.springia.dto;

import com.example.springia.model.enums.SpecificationDocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStoryResponse {
    private Long id;
    private Long conversationSessionId;
    private Long specId;
    private Long planId;
    private Long taskId;
    private Long implId;
    private SpecificationDocumentStatus status;
    private SpecificationDocumentStatus specStatus;
    private SpecificationDocumentStatus planStatus;
    private SpecificationDocumentStatus taskStatus;
    private SpecificationDocumentStatus implStatus;
    private String content;
    private LocalDateTime generatedAt;
}

