package com.example.springia.repository;

import com.example.springia.model.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecificationDocumentRepository extends JpaRepository<UserStory, Long> {

    Optional<UserStory> findTopByConversationSessionIdOrderByGeneratedAtDesc(Long sessionId);

    List<UserStory> findByConversationSessionId(Long sessionId);
}

