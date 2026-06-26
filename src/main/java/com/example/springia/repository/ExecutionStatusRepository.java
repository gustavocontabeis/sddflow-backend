package com.example.springia.repository;

import com.example.springia.entity.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExecutionStatusRepository extends JpaRepository<ExecutionStatus, Long> {

    Optional<ExecutionStatus> findByCoCodigo(String coCodigo);
}

