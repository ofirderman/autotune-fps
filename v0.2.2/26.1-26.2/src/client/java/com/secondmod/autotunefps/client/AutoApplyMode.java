package com.secondmod.autotunefps.client;

public enum AutoApplyMode {
    OFF("Off"),
    SELECTED_PRESET("Selected preset"),
    SMART_RECOMMENDATION("Recommended preset");

    private final String displayName;

    AutoApplyMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
