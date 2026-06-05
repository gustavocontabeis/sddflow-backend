package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProcessBuilderReturnDTO {
    private int exitCode;
    private String output;
    public boolean isOk(){
        return exitCode == 0;
    }
}
