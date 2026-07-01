package com.example.springia.agent.responseapi.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateFileToolParams {

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("content")
    private String content;

}

