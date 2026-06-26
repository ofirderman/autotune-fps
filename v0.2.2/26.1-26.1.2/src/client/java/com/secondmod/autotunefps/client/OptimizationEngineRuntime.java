package com.secondmod.autotunefps.client;

public final class OptimizationEngineRuntime {
    private static volatile OptimizationEngine engine;
    private static volatile boolean particleHookAvailable;

    private OptimizationEngineRuntime() {
    }

    public static void install(OptimizationEngine optimizationEngine) {
        engine = optimizationEngine;
    }

    public static boolean allowParticle() {
        OptimizationEngine activeEngine = engine;
        return activeEngine == null || activeEngine.allowParticle();
    }

    public static void markParticleHookAvailable() {
        particleHookAvailable = true;
    }

    public static boolean isParticleHookAvailable() {
        return particleHookAvailable;
    }
}
