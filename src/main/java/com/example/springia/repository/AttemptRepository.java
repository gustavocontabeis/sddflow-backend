package com.example.springia.repository;

import com.example.springia.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findAllByNuExecucaoIdOrderByNuNumeroAsc(Long nuExecucaoId);
}

