package com.example.springia.service;

import com.example.springia.model.SpecSdd;
import com.example.springia.model.UserStory;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import com.example.springia.repository.SpecSddRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Slf4j
@Service
@RequiredArgsConstructor
public class SpecSddService {

    private final SpecSddRepository specSddRepository;

    public List<SpecSdd> findAll() {
        log.info("[SERVICE] findAll SpecSdd");
        List<SpecSdd> result = specSddRepository.findAll();
        log.info("[SERVICE] findAll SpecSdd total={}", result.size());
        return result;
    }

    public Optional<SpecSdd> findById(Long id) {
        log.info("[SERVICE] findById SpecSdd id={}", id);
        Optional<SpecSdd> result = specSddRepository.findById(id);
        log.info("[SERVICE] findById SpecSdd id={} found={}", id, result.isPresent());
        return result;
    }

    public SpecSdd save(SpecSdd specSdd) {
        log.info("[SERVICE] save SpecSdd id={} userStoryId={} contentLength={}",
                specSdd.getId(),
                specSdd.getUserStory() != null ? specSdd.getUserStory().getId() : null,
                specSdd.getContent() != null ? specSdd.getContent().length() : 0);
        SpecSdd saved = specSddRepository.save(specSdd);
        log.info("[SERVICE] save SpecSdd persistedId={}", saved.getId());
        return saved;
    }

    public SpecSdd saveSpec(UserStory userStory, String content) {
        log.info("[SERVICE] saveSpec userStoryId={} contentLength={}",
                userStory != null ? userStory.getId() : null,
                content != null ? content.length() : 0);

        SpecSdd existing = specSddRepository.findByUserStory_Id(userStory.getId()).orElse(null);
        if (existing != null) {
            log.info("[SERVICE] saveSpec updating existing SpecSdd id={}", existing.getId());
            existing.setContent(content);
            existing.setStatus(SpecificationDocumentStatus.IN_PROGRESS);
            return specSddRepository.save(existing);
        }

        SpecSdd specSdd = SpecSdd.builder()
                .userStory(userStory)
                .content(content)
                .status(SpecificationDocumentStatus.IN_PROGRESS)
                .build();

        return save(specSdd);
    }

    public Optional<SpecSdd> approve(Long id) {
        log.info("[SERVICE] approve SpecSdd id={}", id);
        return specSddRepository.findById(id).map(specSdd -> {
            specSdd.setStatus(SpecificationDocumentStatus.APPROVED);
            SpecSdd saved = specSddRepository.save(specSdd);
            log.info("[SERVICE] approve SpecSdd id={} status={}", saved.getId(), saved.getStatus());
            return saved;
        });
    }

    @Transactional
    public void delete(Long id) {
        log.info("[SERVICE] delete SpecSdd id={}", id);
        specSddRepository.findById(id).ifPresent(specSdd -> {
            if (specSdd.getUserStory() != null) {
                // Break bidirectional association before removing child.
                specSdd.getUserStory().setSpec(null);
                specSdd.setUserStory(null);
            }
            specSddRepository.delete(specSdd);
        });
        log.info("[SERVICE] delete SpecSdd id={} completed", id);
    }
}

