package com.example.springia.service;

import com.example.springia.dto.ProjectCreateRequest;
import com.example.springia.dto.ProjectResponse;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request) {
        return saveOrUpdate(request);
    }

    @Transactional
    public ProjectResponse updade(ProjectCreateRequest request) {
        return saveOrUpdate(request);
    }

    private ProjectResponse saveOrUpdate(ProjectCreateRequest request) {
        Project project = request.getId() != null
                ? projectRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Project nao encontrado: " + request.getId()))
                : new Project();

        project.setSigla(request.getSigla());
        project.setName(request.getName());
        project.setConstitution(request.getConstitution());

        List<CodeRepo> repos = new ArrayList<>();
        if (request.getRepos() != null && !request.getRepos().isEmpty()) {
            repos = request.getRepos().stream()
                    .map(repoRequest -> {
                        CodeRepo repo = new CodeRepo();
                        repo.setId(repoRequest.getId());
                        repo.setPath(repoRequest.getPath());
                        repo.setType(repoRequest.getType());
                        repo.setBranch(repoRequest.getBranch());
                        repo.setName(repoRequest.getName());
                        repo.setProject(project);
                        return repo;
                    })
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        project.setRepos(repos);

        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    private ProjectResponse toResponse(Project project) {
        List<ProjectResponse.ProjectRepoResponse> repos = project.getRepos() == null ? List.of() : project.getRepos().stream()
                .map(repo -> new ProjectResponse.ProjectRepoResponse(repo.getId(), repo.getPath(), repo.getType()))
                .toList();

        return new ProjectResponse(
                project.getId(),
                project.getSigla(),
                project.getName(),
                project.getConstitution(),
                repos
        );
    }

    public List<Project>findAll(){
        return projectRepository.findAll();
    }

    public Project findById(long id) {
        return projectRepository.findById(id).orElse(null);
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.deleteById(id);
    }
}

