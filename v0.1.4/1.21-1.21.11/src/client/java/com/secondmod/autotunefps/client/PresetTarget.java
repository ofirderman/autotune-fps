package com.secondmod.autotunefps.client;

public record PresetTarget(
    String graphicsMode,
    String clouds,
    String particles,
    int renderDistance,
    int simulationDistance,
    double entityDistanceScaling,
    int biomeBlend,
    boolean entityShadows,
    String chunkUpdatePriority
) {
}

