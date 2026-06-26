package com.example.springia.repository;

import com.example.springia.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    List<Execution> findAllByOrderByNuIdDesc();
}

