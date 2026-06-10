package com.example.springia.dto;

import com.example.springia.model.CodeRepo;
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
    private String imageName;
    public boolean isOk(){
        return exitCode == 0;
    }
}
