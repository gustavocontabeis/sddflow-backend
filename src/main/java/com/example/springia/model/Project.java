package com.example.springia.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "lictb002_projeto")
public class Project {

    @Id
    @SequenceGenerator(name = "licsq002_projeto", sequenceName = "licsq002_projeto", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq002_projeto")
    @Column(name = "nu_projeto", nullable = false)
    private Long id;

    @NotBlank
    @Size(max = 10)
    @Column(name = "sg_projeto", nullable = false, length = 10)
    private String sigla;

    @NotBlank
    @Size(max = 100)
    @Column(name = "no_projeto", nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 4000)
    @Column(name = "de_constituicao", nullable = false, length = 4000, columnDefinition = "TEXT")
    private String constitution;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<CodeRepo> repos;

}
