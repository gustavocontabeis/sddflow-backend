package com.example.springia.repository;

import com.example.springia.entity.CompilationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompilationLogRepository extends JpaRepository<CompilationLog, Long> {

    List<CompilationLog> findAllByNuTentativaIdOrderByNuIdAsc(Long nuTentativaId);
}

