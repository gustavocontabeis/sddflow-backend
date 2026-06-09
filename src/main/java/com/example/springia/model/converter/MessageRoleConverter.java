package com.example.springia.model.converter;

import com.example.springia.model.enums.MessageRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class MessageRoleConverter implements AttributeConverter<MessageRole, String> {

    @Override
    public String convertToDatabaseColumn(MessageRole attribute) {
        return attribute != null ? attribute.getSigla() : null;
    }

    @Override
    public MessageRole convertToEntityAttribute(String dbData) {
        return dbData != null ? MessageRole.fromSigla(dbData) : null;
    }
}

