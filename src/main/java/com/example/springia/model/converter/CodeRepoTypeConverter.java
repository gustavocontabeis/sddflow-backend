package com.example.springia.model.converter;

import com.example.springia.model.enums.CodeRepoType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class CodeRepoTypeConverter implements AttributeConverter<CodeRepoType, String> {

    @Override
    public String convertToDatabaseColumn(CodeRepoType attribute) {
        return attribute != null ? attribute.getSigla() : null;
    }

    @Override
    public CodeRepoType convertToEntityAttribute(String dbData) {
        return dbData != null ? CodeRepoType.fromSigla(dbData) : null;
    }
}

