package com.example.springia.model;

import com.example.springia.model.enums.Stage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
@Entity
public class ConversationSession {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private Stage stage;

    @Lob
    private String contextJson;

    private LocalDateTime createdAt;
}