package com.example.springia.repository;

import com.example.springia.model.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationSession, String> {}