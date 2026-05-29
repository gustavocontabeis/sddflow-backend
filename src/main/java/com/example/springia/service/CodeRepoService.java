package com.example.springia.service;

import com.example.springia.model.CodeRepo;
import com.example.springia.repository.CodeRepoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeRepoService {

    private final CodeRepoRepository codeRepoRepository;
    private final DiscoveryService discoveryService;

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
    public CodeRepo updateConstitution(Long id) {
        Optional<CodeRepo> byId = findById(id);
        if(byId.isPresent()){
            CodeRepo codeRepo = byId.get();
            String dicovery = discoveryService.dicovery(Path.of(codeRepo.getPath()));
            codeRepo.setConstitution(dicovery);
            save(codeRepo);
            return codeRepo;
        }
        return null;
    }
}

