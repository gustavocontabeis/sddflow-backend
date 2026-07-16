package com.example.springia.model;

import com.example.springia.model.converter.MessageRoleConverter;
import com.example.springia.model.enums.MessageRole;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "lictb005_mensagem")
public class Message {

    @Id
    @SequenceGenerator(name = "licsq005_mensagem", sequenceName = "licsq005_mensagem", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq005_mensagem")
    @Column(name = "nu_mensagem", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nu_sessao_conversa", nullable = false)
    private ConversationSession conversationSession;

    @NotNull
    @Convert(converter = MessageRoleConverter.class)
    @Column(name = "ic_papel_mensagem", nullable = false, length = 1)
    private MessageRole role;

    @NotBlank
    @Size(max = 7000)
    @Column(name = "de_conteudo", nullable = false, length = 7000, columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Column(name = "dh_registro", nullable = false)
    private LocalDateTime timestamp;
}