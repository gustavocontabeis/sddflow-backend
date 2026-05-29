package com.example.springia.service;

import com.example.springia.model.CodeRepo;
import com.example.springia.repository.CodeRepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CodeRepoService {
    private final CodeRepoRepository codeRepoRepository;

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
}

