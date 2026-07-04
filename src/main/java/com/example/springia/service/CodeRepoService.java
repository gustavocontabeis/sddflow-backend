package com.example.springia.service;

import com.example.springia.dto.CloneRepositoryRequest;
import com.example.springia.dto.CloneRepositoryResponse;
import com.example.springia.dto.DiscoveryRepoDTO;
import com.example.springia.dto.UpdateCodeRepoConstitutionsRequest;
import com.example.springia.model.CodeRepo;
import com.example.springia.repository.CodeRepoRepository;
import com.example.springia.utils.FileUtils;
import com.example.springia.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeRepoService {

    private final CodeRepoRepository codeRepoRepository;
    private final DiscoveryService discoveryService;
    private final GitHubService gitHubService;

    @Transactional(readOnly = true)
    public List<CodeRepo> findAll() {
        return codeRepoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CodeRepo> findById(Long id) {
        return codeRepoRepository.findById(id);
    }

    @Transactional
    public CodeRepo save(CodeRepo codeRepo) {
        return codeRepoRepository.save(codeRepo);
    }

    @Transactional
    public void delete(Long id) {
        codeRepoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CodeRepo> findByProjectId(Long projectId) {
        return codeRepoRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<CodeRepo> findByProjectIdAndType(Long projectId, com.example.springia.model.enums.CodeRepoType type) {
        return codeRepoRepository.findByProjectIdAndType(projectId, type);
    }

    @Transactional(readOnly = true)
    public Optional<CodeRepo> findByProjectIdAndName(Long projectId, String name) {
        return codeRepoRepository.findByProjectIdAndName(projectId, name);
    }

    @Transactional(readOnly = false)
    public CodeRepo updateStructure(Long id) throws IOException {

        Optional<CodeRepo> byId = findById(id);

        if(byId.isPresent()){

            CodeRepo codeRepo = byId.get();
            String[] split = codeRepo.getUrl().split("/");
            String owner =  split[split.length - 2];
            String repo = split[split.length - 1].replace(".git", "");

            CloneRepositoryResponse cloneRepositoryResponse = gitHubService.cloneRepository(CloneRepositoryRequest.builder().owner(owner).repo(repo).branch(codeRepo.getBranch()).build());

            DiscoveryRepoDTO discovery = discoveryService.dicovery(Path.of(cloneRepositoryResponse.getClonedPath()));

            String strutcture = discovery.getStrutcture();

            do {
                strutcture = strutcture.replace(cloneRepositoryResponse.getSufixoNumerico(), "");
            } while (strutcture.contains(cloneRepositoryResponse.getSufixoNumerico()));

            log.info("strutcture: {}", strutcture);

            codeRepo.setStructure(strutcture);
            codeRepo.setExtensoesDeArquivosFonte(discovery.getExtensoesDeArquivosFonte());

            save(codeRepo);
            FileUtils.removeDir(Paths.get(cloneRepositoryResponse.getClonedPath()));
            return codeRepo;
        }
        return null;
    }

    @Transactional
    public CodeRepo updateConstitutions(Long id, UpdateCodeRepoConstitutionsRequest request) {

        Optional<CodeRepo> byId = findById(id);

        if (byId.isEmpty()) {
            return null;
        }

        CodeRepo codeRepo = byId.get();

        // Atualiza apenas os dois campos permitidos pelo endpoint PATCH.
        if (request != null) {
            if (request.getConstitution() != null) {
                codeRepo.setConstitution(request.getConstitution());
            }
            if (request.getStructure() != null) {
                codeRepo.setStructure(request.getStructure());
            }
        }

        return save(codeRepo);
    }
}

