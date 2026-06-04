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
            case OFF -> "Restore your original settings from before AutoTune FPS changed them.";
            case ULTIMATE_PERFORMANCE -> "Maximum FPS focus with limited distance scaling.";
            case PERFORMANCE -> "FPS-focused settings with light adaptive scaling.";
            case BALANCED -> "Middle point between FPS and visual quality.";
            case QUALITY -> "Higher visual quality while keeping performance reasonable.";
        };
    }
}
