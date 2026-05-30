package com.example.springia.dto;

import com.example.springia.model.enums.SpecificationDocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryResponse {
    private Long id;
    private Long userStoryId;
    private Long specId;
    private Long planId;
    private Long taskId;
    private Long implId;
    private Long projectId;
    private String projectSigla;
    private String name;
    private SpecificationDocumentStatus status;
    private SpecificationDocumentStatus userStoryStatus;
    private SpecificationDocumentStatus specStatus;
    private SpecificationDocumentStatus planStatus;
    private SpecificationDocumentStatus taskStatus;
    private SpecificationDocumentStatus implStatus;
    private LocalDateTime createdAt;
}
