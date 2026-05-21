package com.example.springia.model;

import com.example.springia.model.enums.ConcersationStage;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
@Entity
public class ConversationSession {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private SpecificationDocumentStatus status;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_project", nullable = false)
    private Project project;

    @OneToOne(fetch = FetchType.EAGER, optional = true, mappedBy = "conversationSession")
    private UserStory userStory;

    @OneToMany(mappedBy = "conversationSession", cascade = CascadeType.PERSIST, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Message> messages;
}