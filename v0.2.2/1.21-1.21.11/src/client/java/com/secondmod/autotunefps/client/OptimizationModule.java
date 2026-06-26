package com.secondmod.autotunefps.client;

import net.minecraft.client.Minecraft;

public interface OptimizationModule {
    String id();

    String displayName();

    OptimizationImpact fpsImpact();

    OptimizationImpact visualImpact();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isSupported();

    String supportStatus();

    String tooltip();

    void initialize(CompatibilityDetectionModule compatibility);

    void onClientTick(Minecraft client, OptimizationEngineMode mode);
}
