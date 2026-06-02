package com.example.springia.dto;

import com.example.springia.model.enums.CodeRepoType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreateRequest {

    private Long id;
    private String sigla;
    private String name;
    private String path;
    private String url;
    private String constitution;
    private List<ProjectRepoRequest> repos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectRepoRequest {
        private Long id;
        private String path;
        private String url;
        private String branch;
        private String name;
        private String constitution;
        private CodeRepoType type;
    }
}

