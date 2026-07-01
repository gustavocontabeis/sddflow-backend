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
public class ResponseContentFilterOffsets {

    @JsonProperty("start_offset")
    private Integer startOffset;

    @JsonProperty("end_offset")
    private Integer endOffset;

    @JsonProperty("check_offset")
    private Integer checkOffset;
}

