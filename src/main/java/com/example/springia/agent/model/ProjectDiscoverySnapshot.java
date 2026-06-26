package com.example.springia.agent.model;

public record ProjectDiscoverySnapshot(
        ProjectDiscoveryReport backend,
        ProjectDiscoveryReport frontend,
        String summary
) {
}

