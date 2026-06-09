package com.example.springia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImplSddValidationDto {
    private String content;
    private String problems;
    private boolean valid;
}
