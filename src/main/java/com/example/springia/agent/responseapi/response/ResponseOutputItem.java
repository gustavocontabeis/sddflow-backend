package com.example.springia.agent.responseapi.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseOutputItem {

    private String id;
    private String type;
    private String status;
    private String arguments;
    private Object content;

    @JsonProperty("call_id")
    private String callId;

    private String name;
}

