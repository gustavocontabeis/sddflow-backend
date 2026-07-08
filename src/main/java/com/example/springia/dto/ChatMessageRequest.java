package com.example.springia.dto;

import com.example.springia.model.enums.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessageRequest {
    private Long sessionId;
    private Long projectId;
    private Long messageId;
    private String message;
    private String sessionName;
    private MessageRole role;
    private LocalDateTime timestamp;
}
