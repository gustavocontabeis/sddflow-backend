package com.example.springia.repository;

import com.example.springia.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationSessionIdOrderByTimestampAsc(Long sessionId);

    Optional<Message> findTopByConversationSessionIdOrderByTimestampDesc(Long sessionId);

    List<Message> findByConversationSessionId(Long sessionId);

    @Query("""
            select m from lictb005_mensagem m
            where m.id = (
                select max(m2.id) from lictb005_mensagem m2
                where m2.conversationSession.id = :sessionId
                  and m2.timestamp = (
                      select max(m3.timestamp) from lictb005_mensagem m3 where m3.conversationSession.id = :sessionId
                  )
            )
            """)
    Optional<Message> findByLastMessage(@Param("sessionId") Long sessionId);
}