package com.example.springia.service;

import com.example.springia.model.ImplSdd;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import com.example.springia.repository.ImplSddRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImplSddService {

    private final ImplSddRepository implSddRepository;

    public List<ImplSdd> findAll() {
        log.info("[SERVICE] findAll ImplSdd");
        List<ImplSdd> result = implSddRepository.findAll();
        log.info("[SERVICE] findAll ImplSdd total={}", result.size());
        return result;
    }

    public Optional<ImplSdd> findById(Long id) {
        log.info("[SERVICE] findById ImplSdd id={}", id);
        Optional<ImplSdd> result = implSddRepository.findById(id);
        log.info("[SERVICE] findById ImplSdd id={} found={}", id, result.isPresent());
        return result;
    }

    public ImplSdd save(ImplSdd implSdd) {
        log.info("[SERVICE] save ImplSdd id={} userStoryId={} contentLength={}",
                implSdd.getId(),
                implSdd.getUserStory() != null ? implSdd.getUserStory().getId() : null,
                implSdd.getContent() != null ? implSdd.getContent().length() : 0);
        ImplSdd saved = implSddRepository.save(implSdd);
        log.info("[SERVICE] save ImplSdd persistedId={}", saved.getId());
        return saved;
    }

    public Optional<ImplSdd> approve(Long id) {
        log.info("[SERVICE] approve ImplSdd id={}", id);
        return implSddRepository.findById(id).map(implSdd -> {
            implSdd.setStatus(SpecificationDocumentStatus.APPROVED);
            ImplSdd saved = implSddRepository.save(implSdd);
            log.info("[SERVICE] approve ImplSdd id={} status={}", saved.getId(), saved.getStatus());
            return saved;
        });
    }

    @Transactional
    public void delete(Long id) {
        log.info("[SERVICE] delete ImplSdd id={}", id);
        implSddRepository.findById(id).ifPresent(implSdd -> {
            if (implSdd.getUserStory() != null) {
                // Break bidirectional association before removing child.
                implSdd.getUserStory().setImpl(null);
                implSdd.setUserStory(null);
            }
            implSddRepository.deleteById(id);
        });
        log.info("[SERVICE] delete ImplSdd id={} completed", id);
    }

}

