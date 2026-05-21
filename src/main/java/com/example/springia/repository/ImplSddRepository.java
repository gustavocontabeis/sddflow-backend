package com.example.springia.repository;

import com.example.springia.model.ImplSdd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImplSddRepository extends JpaRepository<ImplSdd, Long> {
    Optional<ImplSdd> findByUserStory_Id(Long userStoryId);
}

