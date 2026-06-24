package com.example.springia.model;

import com.example.springia.model.converter.CodeRepoTypeConverter;
import com.example.springia.model.enums.CodeRepoType;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "lictb003_repositorio_codigo")
public class CodeRepo {

    @Id
    @SequenceGenerator(name = "licsq003_repositorio_codigo", sequenceName = "licsq003_repositorio_codigo", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "licsq003_repositorio_codigo")
    @Column(name = "nu_repositorio_codigo", nullable = false)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "no_repositorio", nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 500)
    @Column(name = "de_caminho_repositorio", nullable = false, length = 500)
    private String path;

    @Size(max = 500)
    @Column(name = "de_url_repositorio", nullable = true, length = 500)
    private String url;

    @NotBlank
    @Size(max = 100)
    @Column(name = "no_branch", nullable = false, length = 100)
    private String branch;

    @Column(name = "de_constituicao", nullable = false, columnDefinition = "TEXT")
    private String constitution;

    @NotBlank
    @Column(name = "de_estrutura", nullable = false, columnDefinition = "TEXT")
    private String structure;

    @NotNull
    @Convert(converter = CodeRepoTypeConverter.class)
    @Column(name = "ic_tipo_repositorio", nullable = false, length = 1)
    private CodeRepoType type;

    @NotBlank
    @Size(max = 100)
    @Column(name = "de_extensoes_arquivos_fonte", nullable = false, length = 100)
    private String extensoesDeArquivosFonte;

    @Size(max = 200)
    @Column(name = "co_comando_compilacao", nullable = false, length = 200)
    private String comandoCompilacao;

    @ToString.Exclude
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinColumn(name = "nu_projeto", nullable = false)
    private Project project;

}
