package com.example.springia.repository;

import com.example.springia.model.CodeRepo;
import com.example.springia.model.enums.CodeRepoType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeRepoRepository extends JpaRepository<CodeRepo, Long> {
    List<CodeRepo> findByProjectId(Long projectId);
    List<CodeRepo> findByProjectIdAndType(Long projectId, CodeRepoType type);
    Optional<CodeRepo> findByProjectIdAndName(Long projectId, String name);
}

