package com.example.springia.repository;

import com.example.springia.model.TaskSdd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskSddRepository extends JpaRepository<TaskSdd, Long> {
    Optional<TaskSdd> findByUserStory_Id(Long userStoryId);
}

