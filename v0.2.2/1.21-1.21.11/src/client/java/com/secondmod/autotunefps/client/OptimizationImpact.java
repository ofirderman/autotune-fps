package com.secondmod.autotunefps.client;

public enum OptimizationImpact {
    NONE("None"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    VERY_HIGH("Very High");

    private final String displayName;

    OptimizationImpact(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
