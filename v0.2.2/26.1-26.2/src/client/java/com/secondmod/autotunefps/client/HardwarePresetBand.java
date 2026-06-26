package com.secondmod.autotunefps.client;

public enum HardwarePresetBand {
    ENTRY("entry"),
    LOW_MID("low-mid"),
    MID("mid"),
    UPPER_MID("upper-mid"),
    HIGH_END("high-end");

    private final String displayName;

    HardwarePresetBand(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
