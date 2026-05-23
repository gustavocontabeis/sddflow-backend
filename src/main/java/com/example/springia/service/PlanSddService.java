package com.example.springia.service;

import com.example.springia.model.PlanSdd;
import com.example.springia.model.UserStory;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import com.example.springia.repository.PlanSddRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanSddService {

    private final PlanSddRepository planSddRepository;

    public List<PlanSdd> findAll() {
        log.info("[SERVICE] findAll PlanSdd");
        List<PlanSdd> result = planSddRepository.findAll();
        log.info("[SERVICE] findAll PlanSdd total={}", result.size());
        return result;
    }

    public Optional<PlanSdd> findById(Long id) {
        log.info("[SERVICE] findById PlanSdd id={}", id);
        Optional<PlanSdd> result = planSddRepository.findById(id);
        log.info("[SERVICE] findById PlanSdd id={} found={}", id, result.isPresent());
        return result;
    }

    public PlanSdd save(PlanSdd planSdd) {
        log.info("[SERVICE] save PlanSdd id={} userStoryId={} contentLength={}",
                planSdd.getId(),
                planSdd.getUserStory() != null ? planSdd.getUserStory().getId() : null,
                planSdd.getContent() != null ? planSdd.getContent().length() : 0);
        PlanSdd saved = planSddRepository.save(planSdd);
        log.info("[SERVICE] save PlanSdd persistedId={}", saved.getId());
        return saved;
    }

    public PlanSdd savePlan(UserStory userStory, String content) {
        log.info("[SERVICE] savePlan userStoryId={} contentLength={}",
                userStory != null ? userStory.getId() : null,
                content != null ? content.length() : 0);

        PlanSdd existing = planSddRepository.findByUserStoryId(userStory.getId()).orElse(null);
        if (existing != null) {
            log.info("[SERVICE] savePlan updating existing PlanSdd id={}", existing.getId());
            existing.setContent(content);
            existing.setStatus(SpecificationDocumentStatus.IN_PROGRESS);
            return planSddRepository.save(existing);
        }

        PlanSdd planSdd = PlanSdd.builder()
                .userStory(userStory)
                .content(content)
                .status(SpecificationDocumentStatus.IN_PROGRESS)
                .build();
        return save(planSdd);
    }

    public Optional<PlanSdd> approve(Long id) {
        log.info("[SERVICE] approve PlanSdd id={}", id);
        return planSddRepository.findById(id).map(planSdd -> {
            planSdd.setStatus(SpecificationDocumentStatus.APPROVED);
            PlanSdd saved = planSddRepository.save(planSdd);
            log.info("[SERVICE] approve PlanSdd id={} status={}", saved.getId(), saved.getStatus());
            return saved;
        });
    }

    @Transactional
    public void delete(Long id) {
        log.info("[SERVICE] delete PlanSdd id={}", id);
        planSddRepository.findById(id).ifPresent(planSdd -> {
            if (planSdd.getUserStory() != null) {
                // Break bidirectional association before removing child.
                planSdd.getUserStory().setPlan(null);
                planSdd.setUserStory(null);
            }
            planSddRepository.delete(planSdd);
        });
        log.info("[SERVICE] delete PlanSdd id={} completed", id);
    }
}

