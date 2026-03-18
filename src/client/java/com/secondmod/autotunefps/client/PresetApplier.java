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
        config.selectedPreset = preset;
        config.save();
        return applyPreset(client, preset);
    }

    public String applyRecommendedAndRemember(Minecraft client) {
        HardwareProfile profile = detectProfile(client);
        PresetProfile recommended = profile.recommendedPreset();
        config.selectedPreset = recommended;
        config.save();
        applyPresetTarget(client.options, resolveTarget(profile, recommended));
        client.options.save();
        return "Applied recommended preset: " + recommended.displayName() + " for " + profile.tier() + " tier.";
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
        applyPresetTarget(client.options, resolveTarget(profile, preset));
        client.options.save();
        return "Applied " + preset.displayName() + " preset for " + profile.tier() + " tier.";
    }

    private static PresetTarget resolveTarget(HardwareProfile profile, PresetProfile preset) {
        return switch (profile.tier()) {
            case LOW -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 6, 4, 0.5D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 8, 5, 0.5D, 0, false, "NEARBY");
                case BALANCED -> new PresetTarget("FAST", "FAST", "DECREASED", 10, 6, 0.75D, 2, false, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget("FANCY", "FAST", "DECREASED", 12, 7, 1.0D, 3, true, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
            case MID -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 6, 4, 0.5D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 10, 6, 0.75D, 2, false, "NEARBY");
                case BALANCED -> new PresetTarget("FANCY", "FAST", "DECREASED", 12, 8, 1.0D, 3, true, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget("FANCY", "FANCY", "ALL", 16, 10, 1.25D, 5, true, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
            case HIGH -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 6, 4, 0.5D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "DECREASED", 12, 8, 1.0D, 3, true, "NEARBY");
                case BALANCED -> new PresetTarget("FANCY", "FANCY", "ALL", 18, 10, 1.25D, 5, true, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget(profile.fabulousSafe() ? "FABULOUS" : "FANCY", "FANCY", "ALL", 24, 12, 1.5D, 7, true, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
        };
    }

    private static void applyPresetTarget(Options options, PresetTarget target) {
        options.renderDistance().set(target.renderDistance());
        options.simulationDistance().set(target.simulationDistance());
        options.entityDistanceScaling().set(target.entityDistanceScaling());
        options.biomeBlendRadius().set(target.biomeBlend());
        options.entityShadows().set(target.entityShadows());

        setEnumOption(options.graphicsMode(), target.graphicsMode());
        setEnumOption(options.cloudStatus(), target.clouds());
        setEnumOption(options.particles(), target.particles());
        setEnumOption(options.prioritizeChunkUpdates(), target.chunkUpdatePriority());
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
}
