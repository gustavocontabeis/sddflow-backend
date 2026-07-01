package com.example.springia.agent.responseapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestToolParameters {

    private String type;
    private Map<String, RequestToolProperty> properties;
    private List<String> required;

    @JsonProperty("additionalProperties")
    private Boolean additionalProperties;
}

