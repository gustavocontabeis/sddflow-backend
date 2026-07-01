package com.example.springia.agent.responseapi.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseContentFilter {

    private Boolean blocked;

    @JsonProperty("source_type")
    private String sourceType;

    @JsonProperty("content_filter_raw")
    private List<Object> contentFilterRaw;

    @JsonProperty("content_filter_results")
    private ResponseContentFilterResults contentFilterResults;

    @JsonProperty("content_filter_offsets")
    private ResponseContentFilterOffsets contentFilterOffsets;
}

