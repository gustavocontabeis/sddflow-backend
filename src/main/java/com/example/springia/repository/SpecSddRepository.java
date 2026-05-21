package com.example.springia.repository;

import com.example.springia.model.SpecSdd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpecSddRepository extends JpaRepository<SpecSdd, Long> {
    Optional<SpecSdd> findByUserStory_Id(Long userStoryId);
}

