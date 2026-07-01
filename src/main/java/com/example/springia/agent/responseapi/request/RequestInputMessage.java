package com.example.springia.agent.responseapi.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestInputMessage {

    private String role;
    private List<RequestInputContent> content;
}

