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
public class ResponseUsage {

    @JsonProperty("input_tokens")
    private Integer inputTokens;

    @JsonProperty("input_tokens_details")
    private ResponseTokenDetails inputTokensDetails;

    @JsonProperty("output_tokens")
    private Integer outputTokens;

    @JsonProperty("output_tokens_details")
    private ResponseTokenDetails outputTokensDetails;

    @JsonProperty("total_tokens")
    private Integer totalTokens;
}

