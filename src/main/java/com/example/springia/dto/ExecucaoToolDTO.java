package com.example.springia.dto;

import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExecucaoToolDTO {
    private String functionName;
    private ResponseFunctionToolCall functionCall;
    private ResponseInputItem functionCallRespose;
}
