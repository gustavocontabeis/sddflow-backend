package com.example.springia.model;

import com.example.springia.model.enums.SpecificationDocumentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
@Entity
public class UserStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime generatedAt;

    private SpecificationDocumentStatus status;

    @Lob
    private String content;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH, orphanRemoval = false)
    @JoinColumn(name = "id_conversation_session")
    private ConversationSession conversationSession;

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "userStory", cascade = CascadeType.ALL, orphanRemoval = true)
    private SpecSdd spec;

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "userStory", cascade = CascadeType.ALL, orphanRemoval = true)
    private PlanSdd plan;

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "userStory", cascade = CascadeType.ALL, orphanRemoval = true)
    private TaskSdd task;

    @OneToOne(fetch = FetchType.EAGER, mappedBy = "userStory", cascade = CascadeType.ALL, orphanRemoval = true)
    private ImplSdd impl;



}

