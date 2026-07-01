package com.example.springia.agent.responseapi.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestToolDefinition {

    private String type;
    private String name;
    private String description;
    private RequestToolParameters parameters;
    private Boolean strict;
}

