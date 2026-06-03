package com.example.springia.service;

import com.example.springia.model.ConversationSession;
import com.example.springia.repository.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Transactional(readOnly = true)
    public List<ConversationSession> findAllByOrderByCreatedAtDesc() {
        return conversationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<ConversationSession> findById(Long id) {
        return conversationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return conversationRepository.existsById(id);
    }

    @Transactional(readOnly = false)
    public void delete(Long id) {
        conversationRepository.deleteById(id);
    }

}
