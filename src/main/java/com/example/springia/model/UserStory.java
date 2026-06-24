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
import jakarta.persistence.OneToOne;
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
@Entity(name = "lictb006_historia_usuario")
public class UserStory {

    @Id
    @SequenceGenerator(name = "licsq006_historia_usuario", sequenceName = "licsq006_historia_usuario", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq006_historia_usuario")
    @Column(name = "nu_historia_usuario", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "dh_geracao", nullable = false)
    private LocalDateTime generatedAt;

    @NotNull
    @Convert(converter = SpecificationDocumentStatusConverter.class)
    @Column(name = "ic_status_documento", nullable = false, length = 1)
    private SpecificationDocumentStatus status;

    @NotBlank
    @Column(name = "de_historia_usuario", nullable = false, columnDefinition = "TEXT")
    private String content;

    @NotNull
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinColumn(name = "nu_sessao_conversa", nullable = false)
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

