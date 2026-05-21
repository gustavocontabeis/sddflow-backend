package com.example.springia.dto;

import com.example.springia.model.enums.CodeRepoType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;
    private String sigla;
    private String name;
    private String constitution;
    private List<ProjectRepoResponse> repos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectRepoResponse {
        private Long id;
        private String path;
        private CodeRepoType type;
    }
}

