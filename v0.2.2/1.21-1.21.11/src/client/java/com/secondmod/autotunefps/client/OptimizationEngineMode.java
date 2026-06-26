package com.secondmod.autotunefps.client;

public enum OptimizationEngineMode {
    OFF("Off", OptimizationImpact.NONE, OptimizationImpact.NONE),
    OPTIMIZED("Optimized", OptimizationImpact.MEDIUM, OptimizationImpact.LOW),
    AGGRESSIVE("Aggressive", OptimizationImpact.HIGH, OptimizationImpact.HIGH);

    private final String displayName;
    private final OptimizationImpact fpsImpact;
    private final OptimizationImpact visualImpact;

    OptimizationEngineMode(String displayName, OptimizationImpact fpsImpact, OptimizationImpact visualImpact) {
        this.displayName = displayName;
        this.fpsImpact = fpsImpact;
        this.visualImpact = visualImpact;
    }

    public String displayName() {
        return displayName;
    }

    public OptimizationEngineMode next() {
        return switch (this) {
            case OFF -> OPTIMIZED;
            case OPTIMIZED -> AGGRESSIVE;
            case AGGRESSIVE -> OFF;
        };
    }

    public String tooltip() {
        return switch (this) {
            case OFF -> """
                Off
                Disables extra runtime optimization.
                AutoTune only uses its normal preset and settings behavior.

                FPS Impact: None
                Visual Impact: None""";
            case OPTIMIZED -> """
                Optimized
                Reduces unnecessary client-side visual work while keeping visuals mostly normal.
                Helps most in particle-heavy combat, farms, explosions, and busy servers.
                Some excess effects may appear less often.

                FPS Impact: Medium
                Visual Impact: Low""";
            case AGGRESSIVE -> """
                Aggressive Optimization
                Pushes harder for FPS with stronger, customizable visual tradeoffs.
                Helps most in very busy scenes and on lower-end hardware.
                Enabled modules may make effects look noticeably lighter.

                FPS Impact: High
                Visual Impact: Medium to High""";
        };
    }

    public String impactSummary() {
        return "FPS Impact: " + fpsImpact.displayName() + " | Visual Impact: " + visualImpact.displayName();
    }
}
