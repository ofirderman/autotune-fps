package com.secondmod.autotunefps.client;

public enum PresetProfile {
    OFF("restore", "Off"),
    ULTIMATE_PERFORMANCE("ultimate_performance", "Ultimate Performance"),
    PERFORMANCE("performance", "Performance"),
    BALANCED("balanced", "Balanced"),
    QUALITY("quality", "Quality");

    private final String commandName;
    private final String displayName;

    PresetProfile(String commandName, String displayName) {
        this.commandName = commandName;
        this.displayName = displayName;
    }

    public String commandName() {
        return commandName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return this != OFF;
    }

    public String summary() {
        return switch (this) {
            case OFF -> "Restores the settings AutoTune first saved for you.";
            case ULTIMATE_PERFORMANCE -> "Absolute lowest practical visual settings for maximum FPS.";
            case PERFORMANCE -> "FPS-focused, but less extreme than Ultimate Performance.";
            case BALANCED -> "Recommended middle point for this hardware tier.";
            case QUALITY -> "Highest safe visual quality for this hardware tier.";
        };
    }
}
