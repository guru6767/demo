package com.starto.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Plan {
    EXPLORER, // Free
    TRIAL,    // ₹29 / 7 days
    SPRINT,   // ₹59 / 7 days
    BOOST,    // ₹99 / 15 days
    PRO,      // ₹149 / 1 month
    PRO_PLUS, // ₹349 / 3 months
    GROWTH,   // ₹579 / 6 months
    ANNUAL,   // ₹999 / 12 months
    CAPTAIN,  // ₹99 / 1 month
    CAPTAIN_PRO; // ₹799 / 12 months

    @JsonCreator
    public static Plan fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return EXPLORER;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.equals("FREE")) {
            return EXPLORER;
        }
        try {
            return Plan.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return EXPLORER;
        }
    }
}