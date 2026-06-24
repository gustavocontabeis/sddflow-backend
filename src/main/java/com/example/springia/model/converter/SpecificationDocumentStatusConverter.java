package com.example.springia.model.converter;

import com.example.springia.model.enums.SpecificationDocumentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SpecificationDocumentStatusConverter implements AttributeConverter<SpecificationDocumentStatus, String> {

    @Override
    public String convertToDatabaseColumn(SpecificationDocumentStatus attribute) {
        return attribute != null ? attribute.getSigla() : null;
    }

    @Override
    public SpecificationDocumentStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? SpecificationDocumentStatus.fromSigla(dbData) : null;
    }
}

