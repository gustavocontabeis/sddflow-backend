package com.example.springia.repository;

import com.example.springia.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySessionIdOrderByTimestampAsc(String sessionId);

    List<Message> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}