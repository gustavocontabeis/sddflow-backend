package com.example.springia.service;

import com.example.springia.model.Prompt;
import com.example.springia.repository.PromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptRepository promptRepository;

    public List<Prompt> findAll() {
        log.info("[SERVICE] findAll Prompt");
        List<Prompt> result = promptRepository.findAll();
        log.info("[SERVICE] findAll Prompt total={}", result.size());
        return result;
    }

    public Optional<Prompt> findById(Long id) {
        log.info("[SERVICE] findById Prompt id={}", id);
        Optional<Prompt> result = promptRepository.findById(id);
        log.info("[SERVICE] findById Prompt id={} found={}", id, result.isPresent());
        return result;
    }

    public Optional<Prompt> findByKey(String key) {
        log.info("[SERVICE] findByKey Prompt key={}", key);
        Optional<Prompt> result = promptRepository.findByKey(key);
        log.info("[SERVICE] findByKey Prompt key={} found={}", key, result.isPresent());
        return result;
    }

    public Prompt save(Prompt prompt) {
        log.info("[SERVICE] save Prompt id={} key={} contentLength={}",
                prompt.getId(),
                prompt.getKey(),
                prompt.getContent() != null ? prompt.getContent().length() : 0);
        Prompt saved = promptRepository.save(prompt);
        log.info("[SERVICE] save Prompt persistedId={}", saved.getId());
        return saved;
    }

    public void delete(Long id) {
        log.info("[SERVICE] delete Prompt id={}", id);
        promptRepository.deleteById(id);
        log.info("[SERVICE] delete Prompt id={} completed", id);
    }

    public void deleteByKey(String key) {
        log.info("[SERVICE] deleteByKey Prompt key={}", key);
        promptRepository.deleteByKey(key);
        log.info("[SERVICE] deleteByKey Prompt key={} completed", key);
    }
}

