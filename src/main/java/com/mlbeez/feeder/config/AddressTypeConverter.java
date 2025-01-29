package com.mlbeez.feeder.config;

import com.mlbeez.feeder.model.AddressType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AddressTypeConverter implements AttributeConverter<AddressType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AddressType addressType) {
        return (addressType != null) ? addressType.getValue() : null;
    }

    @Override
    public AddressType convertToEntityAttribute(Integer dbData) {
        try {
            return (dbData != null) ? AddressType.fromValue(dbData) : null;
        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException("Invalid value for AddressType: " + dbData, e);
        }
    }
}

