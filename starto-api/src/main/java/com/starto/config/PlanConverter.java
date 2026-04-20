package com.starto.config;

import com.starto.enums.Plan;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PlanConverter implements AttributeConverter<Plan, String> {

    @Override
    public String convertToDatabaseColumn(Plan plan) {
        if (plan == null) {
            return null;
        }
        return plan.name();
    }

    @Override
    public Plan convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Plan.EXPLORER;
        }
        try {
            return Plan.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Plan.EXPLORER;
        }
    }
}
