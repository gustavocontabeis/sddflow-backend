package com.example.springia.repository;

import com.example.springia.model.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationRepository extends JpaRepository<ConversationSession, Long> {
    List<ConversationSession> findAllByOrderByCreatedAtDesc();
}
