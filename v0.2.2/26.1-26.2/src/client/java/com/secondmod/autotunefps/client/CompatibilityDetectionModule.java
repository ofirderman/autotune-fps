package com.secondmod.autotunefps.client;

import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

public final class CompatibilityDetectionModule {
    private static final String[][] COMMON_MODS = {
        {"sodium", "Sodium"},
        {"iris", "Iris"},
        {"entityculling", "Entity Culling"},
        {"immediatelyfast", "ImmediatelyFast"},
        {"lithium", "Lithium"}
    };

    private final List<String> detectedMods = new ArrayList<>();

    public void detect() {
        detectedMods.clear();
        FabricLoader loader = FabricLoader.getInstance();
        for (String[] mod : COMMON_MODS) {
            if (loader.isModLoaded(mod[0])) {
                detectedMods.add(mod[1]);
            }
        }
    }

    public String summary() {
        return detectedMods.isEmpty() ? "None detected" : String.join(", ", detectedMods);
    }

    public boolean hasDetectedMods() {
        return !detectedMods.isEmpty();
    }
}
