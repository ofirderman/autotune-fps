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
    public HardwarePresetBand presetBand() {
        int totalScore = Math.max(0, Math.min(6, cpuScore + ramScore + gpuScore));
        HardwarePresetBand scoreBand;
        if (totalScore <= 1) {
            scoreBand = HardwarePresetBand.ENTRY;
        } else if (totalScore == 2) {
            scoreBand = HardwarePresetBand.LOW_MID;
        } else if (totalScore <= 4) {
            scoreBand = HardwarePresetBand.MID;
        } else if (totalScore == 5) {
            scoreBand = HardwarePresetBand.UPPER_MID;
        } else {
            scoreBand = HardwarePresetBand.HIGH_END;
        }

        if (scoreBand == HardwarePresetBand.HIGH_END && (shadersActive || hasRiskFlags)) {
            return HardwarePresetBand.UPPER_MID;
        }
        return scoreBand;
    }

    public PresetProfile baselinePreset() {
        return switch (presetBand()) {
            case ENTRY, LOW_MID -> PresetProfile.PERFORMANCE;
            case MID, UPPER_MID -> PresetProfile.BALANCED;
            case HIGH_END -> PresetProfile.QUALITY;
        };
    }

    public String compactSummary() {
        return switch (tier) {
            case LOW -> "Low tier";
            case MID -> "Middle tier";
            case HIGH -> "High tier";
        };
    }
}
