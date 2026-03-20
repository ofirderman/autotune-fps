package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

public final class PresetApplier {
    private final AutoTuneConfig config;
    private final HardwareProfileDetector hardwareProfileDetector;

    public PresetApplier(AutoTuneConfig config) {
        this.config = config;
        this.hardwareProfileDetector = new HardwareProfileDetector();
    }

    public AutoTuneConfig config() {
        return config;
    }

    public HardwareProfile detectProfile(Minecraft client) {
        return hardwareProfileDetector.detect(client);
    }

    public String[] describeStatusLines(Minecraft client) {
        HardwareProfile profile = detectProfile(client);
        return new String[] {
            "AutoTune FPS Status",
            "- Active preset: " + formatPresetWithTier(config.selectedPreset, profile.tier()),
            "- Recommended preset: " + formatPresetWithTier(profile.recommendedPreset(), profile.tier()),
            "- Hardware tier: " + profile.tier() + " (" + describeTier(profile.tier()) + ")",
            "- Graphics safety: " + describeGraphicsSafety(profile),
            "- Auto-apply: " + (config.applyPresetOnWorldJoin ? "ON" : "OFF"),
            "- Renderer: " + displayValue(profile.renderer()),
            "- Vendor: " + displayValue(profile.vendor())
        };
    }

    public String applyConfiguredPresetOnJoin(Minecraft client) {
        if (!config.applyPresetOnWorldJoin || !config.selectedPreset.isEnabled()) {
            return null;
        }

        return applyPreset(client, config.selectedPreset);
    }

    public String applyPresetAndRemember(Minecraft client, PresetProfile preset) {
        String message = applyPreset(client, preset);
        config.selectedPreset = preset;
        config.save();
        return message;
    }

    public String applyRecommendedAndRemember(Minecraft client) {
        HardwareProfile profile = detectProfile(client);
        PresetProfile recommended = profile.recommendedPreset();
        ApplyRuntimeNotes notes = applyPresetTarget(client, resolveTarget(profile, recommended));
        client.options.save();
        config.selectedPreset = recommended;
        config.save();
        return describeAppliedPreset(
            "Applied recommended preset: " + recommended.displayName() + " for " + profile.tier() + " tier.",
            notes
        );
    }

    public String setAutoApply(boolean enabled) {
        config.applyPresetOnWorldJoin = enabled;
        config.save();
        return "Auto-apply on join is now " + (enabled ? "on" : "off") + ".";
    }

    private String applyPreset(Minecraft client, PresetProfile preset) {
        if (!preset.isEnabled()) {
            return "Preset storage updated. AutoTune will not change options until you pick a preset.";
        }

        HardwareProfile profile = detectProfile(client);
        ApplyRuntimeNotes notes = applyPresetTarget(client, resolveTarget(profile, preset));
        client.options.save();
        return describeAppliedPreset(
            "Applied " + preset.displayName() + " preset for " + profile.tier() + " tier.",
            notes
        );
    }

    private static PresetTarget resolveTarget(HardwareProfile profile, PresetProfile preset) {
        return switch (profile.tier()) {
            case LOW -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 6, 5, 0.5D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 8, 5, 0.5D, 0, false, "NEARBY");
                case BALANCED -> new PresetTarget("FAST", "FAST", "DECREASED", 10, 6, 0.75D, 2, false, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget("FANCY", "FAST", "DECREASED", 12, 7, 1.0D, 3, true, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
            case MID -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 6, 5, 0.5D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 10, 6, 0.75D, 2, false, "NEARBY");
                case BALANCED -> new PresetTarget("FANCY", "FAST", "DECREASED", 12, 8, 1.0D, 3, true, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget("FANCY", "FANCY", "ALL", 16, 10, 1.25D, 5, true, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
            case HIGH -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 6, 5, 0.5D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "DECREASED", 12, 8, 1.0D, 3, true, "NEARBY");
                case BALANCED -> new PresetTarget("FANCY", "FANCY", "ALL", 18, 10, 1.25D, 5, true, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget(profile.fabulousSafe() ? "FABULOUS" : "FANCY", "FANCY", "ALL", 24, 12, 1.5D, 7, true, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
        };
    }

    private static ApplyRuntimeNotes applyPresetTarget(Minecraft client, PresetTarget target) {
        Options options = client.options;
        options.renderDistance().set(target.renderDistance());
        boolean usedSafeSimulationDistanceFallback = setSimulationDistanceOption(options.simulationDistance(), target.simulationDistance());
        options.entityDistanceScaling().set(target.entityDistanceScaling());
        options.biomeBlendRadius().set(target.biomeBlend());
        options.entityShadows().set(target.entityShadows());

        GraphicsModeOutcome graphicsModeOutcome = setGraphicsModeOption(client, options.graphicsMode(), target.graphicsMode());
        setEnumOption(options.cloudStatus(), target.clouds());
        setEnumOption(options.particles(), target.particles());
        setEnumOption(options.prioritizeChunkUpdates(), target.chunkUpdatePriority());
        return new ApplyRuntimeNotes(
            graphicsModeOutcome == GraphicsModeOutcome.KEPT_CURRENT_IN_WORLD,
            graphicsModeOutcome == GraphicsModeOutcome.USED_FANCY_IN_WORLD,
            usedSafeSimulationDistanceFallback
        );
    }

    private static GraphicsModeOutcome setGraphicsModeOption(Minecraft client, OptionInstance<?> option, String enumConstantName) {
        if (client.level != null && isCurrentEnumValue(option, "FABULOUS") && !"FABULOUS".equals(enumConstantName)) {
            AutoTuneFps.LOGGER.info("Keeping current graphics mode while in-world because switching away from FABULOUS is unstable");
            return GraphicsModeOutcome.KEPT_CURRENT_IN_WORLD;
        }

        String effectiveMode = enumConstantName;
        GraphicsModeOutcome outcome = GraphicsModeOutcome.APPLIED_REQUESTED;
        if ("FABULOUS".equals(enumConstantName) && client.level != null && !isCurrentEnumValue(option, "FABULOUS")) {
            AutoTuneFps.LOGGER.info("Skipping automatic switch to FABULOUS while in-world; using FANCY for stability");
            effectiveMode = "FANCY";
            outcome = GraphicsModeOutcome.USED_FANCY_IN_WORLD;
        }
        setEnumOption(option, effectiveMode);
        return outcome;
    }

    private static void setEnumOption(OptionInstance<?> option, String enumConstantName) {
        Object current = option.get();
        if (!(current instanceof Enum<?> enumValue)) {
            return;
        }

        try {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Enum<?> replacement = Enum.valueOf((Class) enumValue.getDeclaringClass(), enumConstantName);
            setOptionValue(option, replacement);
        } catch (IllegalArgumentException exception) {
            AutoTuneFps.LOGGER.debug("Skipping missing enum constant {} for {}", enumConstantName, option, exception);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setOptionValue(OptionInstance<?> option, Object value) {
        ((OptionInstance) option).set(value);
    }

    private static boolean isCurrentEnumValue(OptionInstance<?> option, String enumConstantName) {
        Object current = option.get();
        return current instanceof Enum<?> enumValue && enumConstantName.equals(enumValue.name());
    }

    private static boolean setSimulationDistanceOption(OptionInstance<Integer> option, int requestedDistance) {
        Integer currentDistance = option.get();
        int safeRequestedDistance = Math.max(5, requestedDistance);
        int fallbackDistance = currentDistance == null ? 5 : Math.max(5, currentDistance);
        return setIntegerOption(option, safeRequestedDistance, fallbackDistance, "simulation distance");
    }

    private static boolean setIntegerOption(OptionInstance<Integer> option, int requestedValue, int fallbackValue, String optionName) {
        setOptionValue(option, requestedValue);
        Integer appliedValue = option.get();
        if (appliedValue != null && appliedValue == requestedValue) {
            return false;
        }

        AutoTuneFps.LOGGER.warn("Failed to apply {} value {}; using {}", optionName, requestedValue, fallbackValue);
        if (fallbackValue != requestedValue) {
            setOptionValue(option, fallbackValue);
        }
        return true;
    }

    private static String describeAppliedPreset(String baseMessage, ApplyRuntimeNotes notes) {
        StringBuilder builder = new StringBuilder(baseMessage);
        if (notes.keptCurrentGraphicsModeInWorld()) {
            builder.append(" Graphics mode stayed unchanged in-world because switching away from Fabulous is unstable there.");
        } else if (notes.usedFancyInsteadOfFabulousInWorld()) {
            builder.append(" Used Fancy instead of Fabulous in-world for stability.");
        }
        if (notes.usedSafeSimulationDistanceFallback()) {
            builder.append(" Used a safe simulation distance fallback.");
        }
        return builder.toString();
    }

    private static String describeTier(HardwareTier tier) {
        return switch (tier) {
            case LOW -> "weaker hardware, prefers safer/lighter settings";
            case MID -> "balanced hardware, good default quality/performance mix";
            case HIGH -> "stronger hardware, can use higher visual settings safely";
        };
    }

    private static String describeGraphicsSafety(HardwareProfile profile) {
        if (profile.fabulousSafe()) {
            return "Fabulous allowed";
        }
        return "Fancy fallback for stability";
    }

    private static String displayValue(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }

    private static String formatPresetWithTier(PresetProfile preset, HardwareTier tier) {
        return preset.commandName() + " (" + tier + " tier)";
    }

    private enum GraphicsModeOutcome {
        APPLIED_REQUESTED,
        USED_FANCY_IN_WORLD,
        KEPT_CURRENT_IN_WORLD
    }

    private record ApplyRuntimeNotes(
        boolean keptCurrentGraphicsModeInWorld,
        boolean usedFancyInsteadOfFabulousInWorld,
        boolean usedSafeSimulationDistanceFallback
    ) {
    }
}
