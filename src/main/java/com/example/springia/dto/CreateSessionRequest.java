package com.example.springia.dto;

public record CreateSessionRequest(Long projectId, Long sesseionId, String name, String message) {
}
