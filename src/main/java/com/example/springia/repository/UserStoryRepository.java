package com.example.springia.repository;

import com.example.springia.model.UserStory;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    Optional<UserStory> findByConversationSessionId(Long sessionId);

    List<UserStory> findByStatus(SpecificationDocumentStatus status);

    Optional<UserStory> findTopByConversationSessionIdOrderByGeneratedAtDesc(Long sessionId);
}

