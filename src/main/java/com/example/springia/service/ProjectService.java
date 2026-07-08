package com.example.springia.service;

import com.example.springia.dto.ProjectCreateRequest;
import com.example.springia.dto.ProjectResponse;
import com.example.springia.model.CodeRepo;
import com.example.springia.model.Project;
import com.example.springia.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ProjectService(
            ProjectRepository projectRepository,
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.projectRepository = projectRepository;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

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
                        CodeRepo codeRepo = project.getRepos().stream().filter(s -> s.getId().equals(repoRequest.getId())).findFirst().orElse(null);
                        repo.setId(repoRequest.getId());
                        repo.setPath(repoRequest.getPath());
                        repo.setUrl(repoRequest.getUrl());
                        repo.setType(repoRequest.getType());
                        repo.setBranch(repoRequest.getBranch());
                        repo.setName(repoRequest.getName());
                        repo.setConstitution(codeRepo != null ? codeRepo.getConstitution() : null);
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

    @Transactional
    public Project updateStructureInConstitution(Long id) {

        Project project = findById(id);
        List<CodeRepo> repos = project.getRepos();
        StringBuilder constitutionsRepositorios = new StringBuilder();
        for (CodeRepo repo : repos) {
            constitutionsRepositorios.append("=============================\n");
            constitutionsRepositorios.append("TIPO DE REPOSITÓRIO: ");
            constitutionsRepositorios.append(repo.getType());
            constitutionsRepositorios.append("\n");
            constitutionsRepositorios.append("NOME DE REPOSITÓRIO: ");
            constitutionsRepositorios.append(repo.getName());
            constitutionsRepositorios.append("\n");
            constitutionsRepositorios.append("URL DE REPOSITÓRIO: ");
            constitutionsRepositorios.append(repo.getUrl());
            constitutionsRepositorios.append("\n");
            constitutionsRepositorios.append("PATH DE REPOSITÓRIO: ");
            constitutionsRepositorios.append(repo.getPath());
            constitutionsRepositorios.append("\n");
            constitutionsRepositorios.append("CONTITUTION DO REPOSITÓRIO: ");
            constitutionsRepositorios.append("\n");
            constitutionsRepositorios.append(repo.getConstitution());
            constitutionsRepositorios.append("\n");
        }

        String prompt = String.format("""
                Voce é um arquiteto de software sênior especializado em SDD Spec Driven Developement.
                Esta é a CONTITUTION DO PROJETO:
                [%s]
                
                Esta são as contitutions de CADA REPOSITÓRIO DESTE PROJETO:
                [%s]
                
                Crie uma CONTITUTION DO PROJETO completa e organizada. Mantenha a informação completa das contitutions de CADA REPOSITÓRIO DESTE PROJETO.
                Não utilize número nos capítulos, utilize títulos e subtítulos.
                
                """, project.getConstitution(), constitutionsRepositorios.toString());

        log.info("");
        log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        log.info("PROMPT CONSTITUTION [{}]", prompt.split(" ").length);
        log.info("{}", prompt);

        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        log.info("NOVA CONSTITUTION [{}]", content.split(" ").length);
        log.info("{}", content);

        project.setConstitution(content);

        return project;
    }

    public String getConstitution(Project project) {
        StringBuilder sb = new StringBuilder();
        sb.append("# CONTITUTION ").append("\n\n");
        sb.append("- id (project_id): ").append(project.getId()).append("\n");
        sb.append("- sigla: ").append(safe(project.getSigla())).append("\n");
        sb.append("- nome: ").append(safe(project.getName())).append("\n");
        sb.append("\n\n");
        sb.append(safeBlock(project.getConstitution())).append("\n");

        List<CodeRepo> repos = project.getRepos();
        if (repos == null || repos.isEmpty()) {
            sb.append("\n## Repositórios\n");
            sb.append("[nenhum repositório configurado]\n");
            return sb.toString().trim();
        }

        sb.append("\n## Repositórios\n");

        for (CodeRepo repo : repos) {
            log.trace("[BUILD_SYS_PROMPT] Processando repositório {}", repo.getName());

            sb.append("\n### Repositório\n");
            sb.append("- id: ").append(repo.getId()).append("\n");
            sb.append("- nome: ").append(safe(repo.getName())).append("\n");
            sb.append("- path: ").append(safe(repo.getPath())).append("\n");
            sb.append("- url: ").append(safe(repo.getUrl())).append("\n");
            sb.append("- branch: ").append(safe(repo.getBranch())).append("\n");
            sb.append("- tipo: ").append(repo.getType() != null ? repo.getType().name() : "[vazio]").append("\n");
            sb.append("- extensões de Arquivos Fonte: ").append(safe(repo.getExtensoesDeArquivosFonte())).append("\n");
            sb.append("- comando de compilação: ").append(safe(repo.getComandoCompilacao())).append("\n");
            sb.append("#### CONSTITUTION DO REPOSITÓRIO:\n");
            sb.append(safeBlock(repo.getConstitution())).append("\n");
            sb.append("#### ESTRUTURA DO REPOSITÓRIO:\n");
            sb.append(safeBlock(repo.getStructure())).append("\n");
        }
        return sb.toString();
    }
    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "[vazio]";
        }
        return value;
    }

    private String safeBlock(String value) {
        if (value == null || value.isBlank()) {
            return "[vazio]";
        }
        return value.strip();
    }
}

