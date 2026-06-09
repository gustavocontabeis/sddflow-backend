package com.example.springia.model;

import com.example.springia.model.converter.MessageRoleConverter;
import com.example.springia.model.enums.MessageRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Message {

    @Id
    @GeneratedValue
    @Column(nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conversation_session")
    private ConversationSession conversationSession;

    @NotNull
    @Convert(converter = MessageRoleConverter.class)
    @Column(length = 1, nullable = false)
    private MessageRole role;

    @NotNull
    @Lob
    @Column(nullable = false)
    private String content;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime timestamp;
}