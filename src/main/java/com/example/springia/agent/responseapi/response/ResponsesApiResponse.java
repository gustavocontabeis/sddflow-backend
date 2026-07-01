package com.example.springia.agent.responseapi.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponsesApiResponse {

    private String id;
    private String object;

    @JsonProperty("created_at")
    private Long createdAt;

    private String status;
    private Boolean background;

    @JsonProperty("completed_at")
    private Long completedAt;

    @JsonProperty("content_filters")
    private List<ResponseContentFilter> contentFilters;

    private Object error;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("incomplete_details")
    private Object incompleteDetails;

    private Object instructions;

    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

    @JsonProperty("max_tool_calls")
    private Integer maxToolCalls;

    private String model;
    private Object moderation;
    private List<ResponseOutputItem> output;

    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("previous_response_id")
    private String previousResponseId;

    @JsonProperty("prompt_cache_key")
    private String promptCacheKey;

    @JsonProperty("prompt_cache_retention")
    private String promptCacheRetention;

    private ResponseReasoning reasoning;

    @JsonProperty("safety_identifier")
    private String safetyIdentifier;

    @JsonProperty("service_tier")
    private String serviceTier;

    private Boolean store;
    private Double temperature;
    private ResponseText text;

    @JsonProperty("tool_choice")
    private String toolChoice;

    private List<ResponseToolDefinition> tools;

    @JsonProperty("top_logprobs")
    private Integer topLogprobs;

    @JsonProperty("top_p")
    private Double topP;

    private String truncation;
    private ResponseUsage usage;
    private String user;
    private Map<String, Object> metadata;
}

