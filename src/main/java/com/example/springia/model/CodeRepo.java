package com.example.springia.model;

import com.example.springia.model.enums.CodeRepoType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class CodeRepo {

    @Id
    @GeneratedValue
    @Column(name = "id_code_repo")
    private Long id;

    private String name;
    private String path;
    private String url;
    private String branch;

    @Lob
    private String constitution;

    @Lob
    private String structure;

    @Enumerated(EnumType.STRING)
    private CodeRepoType type;

    private String extensoesDeArquivosFonte;

    @ToString.Exclude
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinColumn(name = "id_project")
    private Project project;

}
