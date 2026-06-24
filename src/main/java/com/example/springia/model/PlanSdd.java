package com.example.springia.model;

import com.example.springia.model.converter.SpecificationDocumentStatusConverter;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "lictb008_plano_sdd")
public class PlanSdd {

    @Id
    @SequenceGenerator(name = "licsq008_plano_sdd", sequenceName = "licsq008_plano_sdd", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq008_plano_sdd")
    @Column(name = "nu_plano_sdd", nullable = false)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "nu_historia_usuario", nullable = false)
    private UserStory userStory;

    @NotBlank
    @Lob
    @Column(name = "de_plano", nullable = false, columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Convert(converter = SpecificationDocumentStatusConverter.class)
    @Column(name = "ic_status_documento", nullable = false, length = 1)
    private SpecificationDocumentStatus status;

}

