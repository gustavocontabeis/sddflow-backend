package com.example.springia.repository;

import com.example.springia.entity.ArtifactChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtifactChangeRepository extends JpaRepository<ArtifactChange, Long> {

    List<ArtifactChange> findAllByNuTentativaIdOrderByNuIdAsc(Long nuTentativaId);
}

