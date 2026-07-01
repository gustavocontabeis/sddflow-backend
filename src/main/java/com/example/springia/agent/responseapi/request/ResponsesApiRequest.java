package com.example.springia.agent.responseapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponsesApiRequest {

    private String model;
    private List<RequestInputMessage> input;
    private List<RequestToolDefinition> tools;

    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

    @JsonProperty("tool_choice")
    private String toolChoice;

    private RequestTextConfig text;
}

