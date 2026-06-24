package com.example.springia.model;

import com.example.springia.model.converter.SpecificationDocumentStatusConverter;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "lictb004_sessao_conversa")
public class ConversationSession {

    @Id
    @SequenceGenerator(name = "licsq004_sessao_conversa", sequenceName = "licsq004_sessao_conversa", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq004_sessao_conversa")
    @Column(name = "nu_sessao_conversa", nullable = false)
    private Long id;

    @NotBlank
    @Column(name = "no_sessao_conversa", nullable = false, length = 100)
    private String name;

    @NotNull
    @Convert(converter = SpecificationDocumentStatusConverter.class)
    @Column(name = "ic_status_documento", nullable = false, length = 1)
    private SpecificationDocumentStatus status;

    @NotNull
    @Column(name = "dh_criacao", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nu_projeto", nullable = false)
    private Project project;

    @OneToOne(fetch = FetchType.EAGER, optional = true, mappedBy = "conversationSession", cascade = {CascadeType.REMOVE}, orphanRemoval = true)
    private UserStory userStory;

    @OneToMany(mappedBy = "conversationSession", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Message> messages;
}