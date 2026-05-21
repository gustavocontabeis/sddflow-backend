package com.example.springia.repository;

import com.example.springia.model.PlanSdd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanSddRepository extends JpaRepository<PlanSdd, Long> {
    Optional<PlanSdd> findByUserStoryId(Long userStoryId);
}

