package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class OptimizationEngine {
    private final AutoTuneConfig config;
    private final CompatibilityDetectionModule compatibility = new CompatibilityDetectionModule();
    private final ParticleReductionModule particleReduction;
    private final List<OptimizationModule> modules;

    public OptimizationEngine(AutoTuneConfig config) {
        this.config = config;
        this.particleReduction = new ParticleReductionModule(config);
        this.modules = List.of(particleReduction);
    }

    public void initialize() {
        compatibility.detect();
        for (OptimizationModule module : modules) {
            module.initialize(compatibility);
        }
        OptimizationEngineRuntime.install(this);
        AutoTuneFps.LOGGER.info(
            "[AUTOTUNE_ENGINE] initialized; mode={}; modules={}; detectedMods={}",
            mode().displayName(),
            modules.size(),
            compatibility.summary()
        );
        if (compatibility.hasDetectedMods()) {
            AutoTuneFps.LOGGER.info(
                "[AUTOTUNE_ENGINE] compatibility mode active; Particle Reduction has no ownership overlap with detected mods"
            );
        }
    }

    public OptimizationEngineMode mode() {
        return config.optimizationEngineMode == null ? OptimizationEngineMode.OFF : config.optimizationEngineMode;
    }

    public ParticleReductionModule particleReduction() {
        return particleReduction;
    }

    public String compatibilitySummary() {
        return compatibility.summary();
    }

    public String cycleMode() {
        return setMode(mode().next());
    }

    public String setMode(OptimizationEngineMode mode) {
        config.optimizationEngineMode = mode == null ? OptimizationEngineMode.OFF : mode;
        config.save();
        AutoTuneFps.LOGGER.info("[AUTOTUNE_ENGINE] mode changed to {}", config.optimizationEngineMode.displayName());
        return "Optimization Engine: " + config.optimizationEngineMode.displayName() + ".";
    }

    public String setParticleReductionEnabled(boolean enabled) {
        particleReduction.setEnabled(enabled);
        config.save();
        AutoTuneFps.LOGGER.info("[AUTOTUNE_ENGINE] module={} enabled={}", particleReduction.id(), enabled);
        return "Particle Reduction: " + (enabled ? "On" : "Off") + ".";
    }

    public String toggleParticleReduction() {
        return setParticleReductionEnabled(!particleReduction.isEnabled());
    }

    public void onClientTick(Minecraft client) {
        for (OptimizationModule module : modules) {
            module.onClientTick(client, mode());
        }
    }

    public boolean allowParticle() {
        return particleReduction.allowParticle(mode());
    }

    public String[] describeStatusLines() {
        return new String[] {
            "- Optimization Engine: " + mode().displayName(),
            "- Particle Reduction: " + (particleReduction.isEnabled() ? "On" : "Off")
                + " (" + particleReduction.supportStatus() + ")",
            "- Compatibility mods: " + compatibility.summary()
        };
    }
}
