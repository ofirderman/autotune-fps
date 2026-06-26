package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import net.minecraft.client.Minecraft;

public final class ParticleReductionModule implements OptimizationModule {
    private static final int OPTIMIZED_PARTICLES_PER_TICK = 140;
    private static final int AGGRESSIVE_PARTICLES_PER_TICK = 55;

    private final AutoTuneConfig config;
    private int remainingBudget = Integer.MAX_VALUE;
    private boolean hookObserved;
    private boolean availableLogged;
    private boolean unavailableLogged;
    private int ticksWaitingForHook;

    public ParticleReductionModule(AutoTuneConfig config) {
        this.config = config;
    }

    @Override
    public String id() {
        return "particle_reduction";
    }

    @Override
    public String displayName() {
        return "Particle Reduction";
    }

    @Override
    public OptimizationImpact fpsImpact() {
        return OptimizationImpact.MEDIUM;
    }

    @Override
    public OptimizationImpact visualImpact() {
        return OptimizationImpact.MEDIUM;
    }

    @Override
    public boolean isEnabled() {
        return config.particleReductionEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.particleReductionEnabled = enabled;
    }

    @Override
    public boolean isSupported() {
        return OptimizationEngineRuntime.isParticleHookAvailable();
    }

    @Override
    public String supportStatus() {
        if (!isSupported()) {
            return "Unsupported on this runtime; disabled safely";
        }
        return hookObserved ? "Active hook verified" : "Runtime hook initialized";
    }

    @Override
    public String tooltip() {
        return """
            Particle Reduction
            Limits excessive client-side particle spawns when a scene becomes crowded.
            Helps during explosions, mob farms, combat, rain, and busy servers.
            Some particles may appear less often when the budget is exceeded.

            FPS Impact: Medium
            Visual Impact: Medium""";
    }

    @Override
    public void initialize(CompatibilityDetectionModule compatibility) {
        AutoTuneFps.LOGGER.info(
            "[AUTOTUNE_ENGINE] module={} initialized; enabled={}; optimizedBudget={}; aggressiveBudget={}; compatibilityMods={}",
            id(),
            isEnabled(),
            OPTIMIZED_PARTICLES_PER_TICK,
            AGGRESSIVE_PARTICLES_PER_TICK,
            compatibility.summary()
        );
    }

    @Override
    public void onClientTick(Minecraft client, OptimizationEngineMode mode) {
        if (!isSupported()) {
            remainingBudget = Integer.MAX_VALUE;
            ticksWaitingForHook++;
            if (!unavailableLogged && ticksWaitingForHook >= 1200) {
                unavailableLogged = true;
                AutoTuneFps.LOGGER.warn(
                    "[AUTOTUNE_ENGINE] module={} runtime hook unavailable; module disabled safely",
                    id()
                );
            }
            return;
        }
        if (!availableLogged) {
            availableLogged = true;
            AutoTuneFps.LOGGER.info(
                "[AUTOTUNE_ENGINE] module={} runtime hook available; enabled={}",
                id(),
                isEnabled()
            );
        }
        remainingBudget = switch (mode) {
            case OFF -> Integer.MAX_VALUE;
            case OPTIMIZED -> OPTIMIZED_PARTICLES_PER_TICK;
            case AGGRESSIVE -> AGGRESSIVE_PARTICLES_PER_TICK;
        };
    }

    public boolean allowParticle(OptimizationEngineMode mode) {
        hookObserved = true;
        if (!isEnabled() || mode == OptimizationEngineMode.OFF) {
            return true;
        }
        if (remainingBudget > 0) {
            remainingBudget--;
            return true;
        }
        return false;
    }
}
