package com.secondmod.autotunefps.client;

public record HardwareProfile(
    HardwareTier tier,
    int cpuScore,
    int ramScore,
    int gpuScore,
    String renderer,
    String vendor,
    boolean shadersActive,
    boolean hasRiskFlags,
    boolean sodiumLoaded,
    boolean fabulousSafe
) {
    public PresetProfile recommendedPreset() {
        return switch (tier) {
            case LOW -> PresetProfile.PERFORMANCE;
            case MID, HIGH -> PresetProfile.BALANCED;
        };
    }

    public String compactSummary() {
        return tier + " tier | Recommended: " + recommendedPreset().displayName();
    }
}
