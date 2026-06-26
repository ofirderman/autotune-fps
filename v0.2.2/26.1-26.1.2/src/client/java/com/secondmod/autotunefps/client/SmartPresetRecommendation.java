package com.secondmod.autotunefps.client;

public record SmartPresetRecommendation(
    PresetProfile basePreset,
    PresetTarget target,
    int changeCount
) {
}
