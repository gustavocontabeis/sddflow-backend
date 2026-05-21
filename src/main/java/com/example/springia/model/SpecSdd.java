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
public class SpecSdd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_user_story")
    private UserStory userStory;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    private SpecificationDocumentStatus status;

}

