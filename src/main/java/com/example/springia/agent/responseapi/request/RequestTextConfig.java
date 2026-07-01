package com.example.springia.agent.responseapi.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestTextConfig {

    private RequestTextFormat format;
    private String verbosity;
}

