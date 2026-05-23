package com.example.springia.repository;

import com.example.springia.model.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptRepository extends JpaRepository<Prompt, Long> {
    Optional<Prompt> findByKey(String key);

    void deleteByKey(String key);
}

