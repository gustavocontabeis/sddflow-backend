package com.example.springia.service;

import com.example.springia.model.TaskSdd;
import com.example.springia.model.UserStory;
import com.example.springia.model.enums.SpecificationDocumentStatus;
import com.example.springia.repository.TaskSddRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSddService {

    private final TaskSddRepository taskSddRepository;

    public List<TaskSdd> findAll() {
        log.info("[SERVICE] findAll TaskSdd");
        List<TaskSdd> result = taskSddRepository.findAll();
        log.info("[SERVICE] findAll TaskSdd total={}", result.size());
        return result;
    }

    public Optional<TaskSdd> findById(Long id) {
        log.info("[SERVICE] findById TaskSdd id={}", id);
        Optional<TaskSdd> result = taskSddRepository.findById(id);
        log.info("[SERVICE] findById TaskSdd id={} found={}", id, result.isPresent());
        return result;
    }

    public TaskSdd save(TaskSdd taskSdd) {
        log.info("[SERVICE] save TaskSdd id={} userStoryId={} contentLength={}",
                taskSdd.getId(),
                taskSdd.getUserStory() != null ? taskSdd.getUserStory().getId() : null,
                taskSdd.getContent() != null ? taskSdd.getContent().length() : 0);
        TaskSdd saved = taskSddRepository.save(taskSdd);
        log.info("[SERVICE] save TaskSdd persistedId={}", saved.getId());
        return saved;
    }

    public TaskSdd saveTask(UserStory userStory, String content) {
        log.info("[SERVICE] saveTask userStoryId={} contentLength={}",
                userStory != null ? userStory.getId() : null,
                content != null ? content.length() : 0);

        TaskSdd existing = taskSddRepository.findByUserStory_Id(userStory.getId()).orElse(null);
        if (existing != null) {
            log.info("[SERVICE] saveTask updating existing TaskSdd id={}", existing.getId());
            existing.setContent(content);
            existing.setStatus(SpecificationDocumentStatus.IN_PROGRESS);
            return taskSddRepository.save(existing);
        }

        TaskSdd taskSdd = TaskSdd.builder()
                .userStory(userStory)
                .content(content)
                .status(SpecificationDocumentStatus.IN_PROGRESS)
                .build();
        return save(taskSdd);
    }

    public Optional<TaskSdd> approve(Long id) {
        log.info("[SERVICE] approve TaskSdd id={}", id);
        return taskSddRepository.findById(id).map(taskSdd -> {
            taskSdd.setStatus(SpecificationDocumentStatus.APPROVED);
            TaskSdd saved = taskSddRepository.save(taskSdd);
            log.info("[SERVICE] approve TaskSdd id={} status={}", saved.getId(), saved.getStatus());
            return saved;
        });
    }

    @Transactional
    public void delete(Long id) {
        log.info("[SERVICE] delete TaskSdd id={}", id);
        taskSddRepository.findById(id).ifPresent(taskSdd -> {
            // Break bidirectional relationship before deleting
            if (taskSdd.getUserStory() != null) {
                taskSdd.getUserStory().setTask(null);
                taskSdd.setUserStory(null);
            }
            taskSddRepository.deleteById(id);
        });
        log.info("[SERVICE] delete TaskSdd id={} completed", id);
    }
}

