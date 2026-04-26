package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class PresetApplier {
    private final AutoTuneConfig config;
    private final AutoTuneRuntimeState runtimeState;
    private final HardwareProfileDetector hardwareProfileDetector;

    public PresetApplier(AutoTuneConfig config, AutoTuneRuntimeState runtimeState) {
        this.config = config;
        this.runtimeState = runtimeState;
        this.hardwareProfileDetector = new HardwareProfileDetector();
    }

    public AutoTuneConfig config() {
        return config;
    }

    public HardwareProfile detectProfile(Minecraft client) {
        return hardwareProfileDetector.detect(client);
    }

    public boolean hasRestoreSnapshot() {
        return runtimeState.hasRestoreSnapshot();
    }

    public boolean isAutoApplyEnabled() {
        return config.applyPresetOnWorldJoin;
    }

    public String toggleAutoApply() {
        return setAutoApply(!config.applyPresetOnWorldJoin);
    }

    public boolean isAggressiveModeEnabled() {
        return config.aggressiveMode;
    }

    public String toggleAggressiveMode() {
        return setAggressiveMode(!config.aggressiveMode);
    }

    public String[] describeStatusLines(Minecraft client) {
        HardwareProfile profile = detectProfile(client);
        List<String> lines = new ArrayList<>();
        lines.add("AutoTune FPS");
        lines.add("- Selected preset: " + formatPresetWithTier(config.selectedPreset, profile.tier()));
        lines.add("- Recommended preset: " + formatPresetWithTier(profile.recommendedPreset(), profile.tier()));
        lines.add("- Tier: " + profile.tier() + " (" + describeTier(profile.tier()) + ")");
        lines.add("- Auto-apply: " + (config.applyPresetOnWorldJoin ? "ON" : "OFF"));
        lines.add("- Aggressive mode: " + (config.aggressiveMode ? "ON" : "OFF"));
        lines.add("- Restore snapshot: " + runtimeState.describeRestoreState());
        String recoveryState = runtimeState.describeRecoveryState();
        if (recoveryState != null) {
            lines.add("- Recovery: " + recoveryState);
        }
        lines.add("- GPU: " + displayValue(profile.renderer()));
        return lines.toArray(String[]::new);
    }

    public String[] previewPresetLines(Minecraft client, PresetProfile preset) {
        HardwareProfile profile = detectProfile(client);
        List<String> lines = new ArrayList<>();
        if (!preset.isEnabled()) {
            lines.add("AutoTune preset preview: Restore");
            lines.add("- Would restore the settings AutoTune saved before the first preset apply.");
            lines.add("- Restore snapshot: " + runtimeState.describeRestoreState());
            lines.add("- This preview does not change any settings.");
            return lines.toArray(String[]::new);
        }

        PresetTarget target = resolveTarget(profile, preset);
        if (config.aggressiveMode) {
            target = applyAggressiveMode(target);
        }
        lines.add("AutoTune preset preview: " + preset.displayName() + " for " + profile.tier() + " tier");
        lines.add("- Graphics: " + previewGraphicsMode(client, profile, target.graphicsMode()) + ", clouds: " + target.clouds() + ", particles: " + target.particles());
        lines.add("- Render distance: " + target.renderDistance() + ", simulation distance: " + target.simulationDistance());
        lines.add("- Entity distance: " + target.entityDistanceScaling() + ", biome blend: " + target.biomeBlend());
        lines.add("- Entity shadows: " + (target.entityShadows() ? "on" : "off") + ", chunk updates: " + target.chunkUpdatePriority());
        lines.add("- Aggressive mode: " + (config.aggressiveMode ? "included" : "off"));
        lines.add("- This preview does not change any settings.");
        return lines.toArray(String[]::new);
    }

    public String[] previewRecommendedPresetLines(Minecraft client) {
        HardwareProfile profile = detectProfile(client);
        return previewPresetLines(client, profile.recommendedPreset());
    }

    public String[] describePresetGuideLines(Minecraft client) {
        HardwareProfile profile = detectProfile(client);
        List<String> lines = new ArrayList<>();
        lines.add("AutoTune preset guide");
        lines.add("- Tier: " + profile.tier() + " (" + describeTier(profile.tier()) + ")");
        lines.add("- Recommended: " + profile.recommendedPreset().displayName());
        appendPresetGuideLine(lines, profile, PresetProfile.ULTIMATE_PERFORMANCE);
        appendPresetGuideLine(lines, profile, PresetProfile.PERFORMANCE);
        appendPresetGuideLine(lines, profile, PresetProfile.BALANCED);
        appendPresetGuideLine(lines, profile, PresetProfile.QUALITY);
        lines.add("- Restore: " + PresetProfile.OFF.summary() + " Snapshot: " + runtimeState.describeRestoreState());
        lines.add("- Tip: use /optimizer preview recommended or /optimizer preview <preset> before applying.");
        return lines.toArray(String[]::new);
    }

    public String applyConfiguredPresetOnJoin(Minecraft client) {
        showRecoveryNoticeIfNeeded(client);
        if (!config.applyPresetOnWorldJoin || !config.selectedPreset.isEnabled()) {
            return null;
        }

        return applyPreset(client, config.selectedPreset, "autoapply");
    }

    public String applyPresetAndRemember(Minecraft client, PresetProfile preset) {
        if (!preset.isEnabled()) {
            return restoreSavedSettingsAndRemember(client);
        }

        String message = applyPreset(client, preset, "manual");
        config.selectedPreset = preset;
        config.save();
        return message;
    }

    public String applyRecommendedAndRemember(Minecraft client) {
        saveRestoreSnapshot(client);
        runtimeState.markPendingApply("recommended", "recommend");
        HardwareProfile profile = detectProfile(client);
        PresetProfile recommended = profile.recommendedPreset();
        PresetTarget target = resolveTarget(profile, recommended);
        if (config.aggressiveMode) {
            target = applyAggressiveMode(target);
        }
        ApplyRuntimeNotes notes = applyPresetTarget(client, target, profile.sodiumLoaded());
        client.options.save();
        config.selectedPreset = recommended;
        config.save();
        runtimeState.clearPendingApply();
        boolean clearedRecoveryMode = runtimeState.clearRecoveryMode();
        return describeAppliedPreset(
            "Applied recommended preset: " + recommended.displayName() + " for " + profile.tier() + " tier.",
            notes,
            clearedRecoveryMode
        );
    }

    public String setAutoApply(boolean enabled) {
        config.applyPresetOnWorldJoin = enabled;
        config.save();
        if (enabled && runtimeState.clearRecoveryMode()) {
            return "Auto-apply on join is now on. Recovery mode was cleared.";
        }
        return "Auto-apply on join is now " + (enabled ? "on" : "off") + ".";
    }

    public String setAggressiveMode(boolean enabled) {
        config.aggressiveMode = enabled;
        config.save();
        return "Aggressive mode is now " + (enabled ? "on" : "off") + ".";
    }

    private String applyPreset(Minecraft client, PresetProfile preset, String source) {
        saveRestoreSnapshot(client);
        runtimeState.markPendingApply(preset.commandName(), source);
        HardwareProfile profile = detectProfile(client);
        PresetTarget target = resolveTarget(profile, preset);
        if (config.aggressiveMode) {
            target = applyAggressiveMode(target);
        }
        ApplyRuntimeNotes notes = applyPresetTarget(client, target, profile.sodiumLoaded());
        client.options.save();
        runtimeState.clearPendingApply();
        boolean clearedRecoveryMode = runtimeState.clearRecoveryMode();
        return describeAppliedPreset(
            "Applied " + preset.displayName() + " preset for " + profile.tier() + " tier.",
            notes,
            clearedRecoveryMode
        );
    }

    private void showRecoveryNoticeIfNeeded(Minecraft client) {
        if (client.player == null) {
            return;
        }

        String recoveryNotice = runtimeState.consumeRecoveryNotice();
        if (recoveryNotice == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(recoveryNotice));
    }

    private String restoreSavedSettingsAndRemember(Minecraft client) {
        AutoTuneRuntimeState.RestoreSnapshot restoreSnapshot = runtimeState.restoreSnapshot();
        boolean clearedRecoveryMode = runtimeState.clearRecoveryMode();
        config.selectedPreset = PresetProfile.OFF;
        config.save();
        if (restoreSnapshot == null) {
            return appendRecoveryClearedSuffix(
                "No saved settings to restore. AutoTune is now off.",
                clearedRecoveryMode
            );
        }

        ApplyRuntimeNotes notes = restoreSavedSettings(client, restoreSnapshot, detectProfile(client).sodiumLoaded());
        client.options.save();
        runtimeState.clearRestoreSnapshot();
        return describeRestoredSettings(notes, clearedRecoveryMode);
    }

    private void saveRestoreSnapshot(Minecraft client) {
        runtimeState.saveRestoreSnapshot(captureRestoreSnapshot(client.options));
    }

    private static AutoTuneRuntimeState.RestoreSnapshot captureRestoreSnapshot(Options options) {
        AutoTuneRuntimeState.RestoreSnapshot snapshot = new AutoTuneRuntimeState.RestoreSnapshot();
        snapshot.graphicsMode = currentEnumName(getGraphicsModeOption(options));
        snapshot.clouds = currentEnumName(options.cloudStatus());
        snapshot.particles = currentEnumName(options.particles());
        snapshot.chunkUpdatePriority = currentEnumName(options.prioritizeChunkUpdates());
        snapshot.renderDistance = options.renderDistance().get();
        snapshot.simulationDistance = options.simulationDistance().get();
        snapshot.entityDistanceScaling = options.entityDistanceScaling().get();
        snapshot.biomeBlend = options.biomeBlendRadius().get();
        snapshot.entityShadows = options.entityShadows().get();
        return snapshot;
    }

    private static ApplyRuntimeNotes restoreSavedSettings(Minecraft client, AutoTuneRuntimeState.RestoreSnapshot snapshot, boolean sodiumLoaded) {
        Options options = client.options;
        boolean usedSafeSimulationDistanceFallback = false;

        if (snapshot.renderDistance != null) {
            options.renderDistance().set(snapshot.renderDistance);
        }
        if (snapshot.simulationDistance != null) {
            usedSafeSimulationDistanceFallback = setSimulationDistanceOption(options.simulationDistance(), snapshot.simulationDistance);
        }
        if (snapshot.entityDistanceScaling != null) {
            options.entityDistanceScaling().set(snapshot.entityDistanceScaling);
        }
        if (snapshot.biomeBlend != null) {
            options.biomeBlendRadius().set(snapshot.biomeBlend);
        }
        if (snapshot.entityShadows != null) {
            options.entityShadows().set(snapshot.entityShadows);
        }

        GraphicsModeOutcome graphicsModeOutcome = GraphicsModeOutcome.APPLIED_REQUESTED;
        if (snapshot.graphicsMode != null) {
            graphicsModeOutcome = setGraphicsModeOption(client, getGraphicsModeOption(options), snapshot.graphicsMode, sodiumLoaded);
        }
        if (snapshot.clouds != null) {
            setEnumOption(options.cloudStatus(), snapshot.clouds);
        }
        if (snapshot.particles != null) {
            setEnumOption(options.particles(), snapshot.particles);
        }
        if (snapshot.chunkUpdatePriority != null) {
            setEnumOption(options.prioritizeChunkUpdates(), snapshot.chunkUpdatePriority);
        }

        return new ApplyRuntimeNotes(
            graphicsModeOutcome == GraphicsModeOutcome.USED_FANCY_FOR_SODIUM,
            graphicsModeOutcome == GraphicsModeOutcome.KEPT_CURRENT_IN_WORLD,
            graphicsModeOutcome == GraphicsModeOutcome.USED_FANCY_IN_WORLD,
            usedSafeSimulationDistanceFallback
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

    private static PresetTarget applyAggressiveMode(PresetTarget target) {
        String graphicsMode = "FABULOUS".equals(target.graphicsMode()) ? "FANCY" : target.graphicsMode();
        String clouds = switch (target.clouds()) {
            case "FANCY" -> "FAST";
            default -> target.clouds();
        };
        String particles = switch (target.particles()) {
            case "ALL" -> "DECREASED";
            case "DECREASED" -> "MINIMAL";
            default -> target.particles();
        };
        int renderDistance = Math.max(6, target.renderDistance() - 2);
        int simulationDistance = Math.max(5, target.simulationDistance() - 2);
        double entityDistanceScaling = Math.max(0.5D, target.entityDistanceScaling() - 0.25D);
        int biomeBlend = Math.max(0, target.biomeBlend() - 2);
        return new PresetTarget(
            graphicsMode,
            clouds,
            particles,
            renderDistance,
            simulationDistance,
            entityDistanceScaling,
            biomeBlend,
            false,
            "NEARBY"
        );
    }

    private static String previewGraphicsMode(Minecraft client, HardwareProfile profile, String requestedGraphicsMode) {
        if ("FABULOUS".equals(requestedGraphicsMode) && profile.sodiumLoaded()) {
            return "FANCY (Sodium compatibility)";
        }
        String currentGraphicsMode = currentEnumName(getGraphicsModeOption(client.options));
        if (client.level != null && "FABULOUS".equals(currentGraphicsMode) && !"FABULOUS".equals(requestedGraphicsMode)) {
            return requestedGraphicsMode + " (may stay current while in-world)";
        }
        return requestedGraphicsMode;
    }

    private static ApplyRuntimeNotes applyPresetTarget(Minecraft client, PresetTarget target, boolean sodiumLoaded) {
        Options options = client.options;
        options.renderDistance().set(target.renderDistance());
        boolean usedSafeSimulationDistanceFallback = setSimulationDistanceOption(options.simulationDistance(), target.simulationDistance());
        options.entityDistanceScaling().set(target.entityDistanceScaling());
        options.biomeBlendRadius().set(target.biomeBlend());
        options.entityShadows().set(target.entityShadows());

        GraphicsModeOutcome graphicsModeOutcome = setGraphicsModeOption(client, getGraphicsModeOption(options), target.graphicsMode(), sodiumLoaded);
        setEnumOption(options.cloudStatus(), target.clouds());
        setEnumOption(options.particles(), target.particles());
        setEnumOption(options.prioritizeChunkUpdates(), target.chunkUpdatePriority());
        return new ApplyRuntimeNotes(
            graphicsModeOutcome == GraphicsModeOutcome.USED_FANCY_FOR_SODIUM,
            graphicsModeOutcome == GraphicsModeOutcome.KEPT_CURRENT_IN_WORLD,
            graphicsModeOutcome == GraphicsModeOutcome.USED_FANCY_IN_WORLD,
            usedSafeSimulationDistanceFallback
        );
    }

    private static GraphicsModeOutcome setGraphicsModeOption(Minecraft client, OptionInstance<?> option, String enumConstantName, boolean sodiumLoaded) {
        if (client.level != null && isCurrentEnumValue(option, "FABULOUS") && !"FABULOUS".equals(enumConstantName)) {
            AutoTuneFps.LOGGER.info("Keeping current graphics mode while in-world because switching away from FABULOUS is unstable");
            return GraphicsModeOutcome.KEPT_CURRENT_IN_WORLD;
        }

        String effectiveMode = enumConstantName;
        GraphicsModeOutcome outcome = GraphicsModeOutcome.APPLIED_REQUESTED;
        if ("FABULOUS".equals(enumConstantName) && sodiumLoaded) {
            AutoTuneFps.LOGGER.info("Skipping automatic switch to FABULOUS while Sodium is detected; using FANCY for compatibility");
            effectiveMode = "FANCY";
            outcome = GraphicsModeOutcome.USED_FANCY_FOR_SODIUM;
        }
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

    private static OptionInstance<?> getGraphicsModeOption(Options options) {
        OptionInstance<?> option = tryGetOption(options,
            "graphicsMode",
            "getGraphicsMode",
            "getPreset",
            "graphicsPreset",
            "method_42534",
            "method_75329");
        if (option != null) {
            return option;
        }

        throw new IllegalStateException("AutoTune could not find the graphics mode option on this Minecraft version.");
    }

    private static OptionInstance<?> tryGetOption(Options options, String... methodNames) {
        Class<?> optionsClass = options.getClass();
        for (String methodName : methodNames) {
            try {
                Method method = optionsClass.getMethod(methodName);
                Object value = method.invoke(options);
                if (value instanceof OptionInstance<?> option) {
                    return option;
                }
            } catch (NoSuchMethodException ignored) {
                // Try the next known accessor name for this Minecraft patch line.
            } catch (ReflectiveOperationException exception) {
                AutoTuneFps.LOGGER.debug("Failed to read option {} from {}", methodName, optionsClass.getName(), exception);
            }
        }
        return null;
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

    private static String currentEnumName(OptionInstance<?> option) {
        Object current = option.get();
        if (current instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return null;
    }

    private static String describeAppliedPreset(String baseMessage, ApplyRuntimeNotes notes, boolean clearedRecoveryMode) {
        StringBuilder builder = new StringBuilder(baseMessage);
        appendRuntimeNotes(builder, notes);
        if (clearedRecoveryMode) {
            builder.append(" Recovery mode was cleared, but auto-apply still stays off until you turn it back on.");
        }
        return builder.toString();
    }

    private static String describeRestoredSettings(ApplyRuntimeNotes notes, boolean clearedRecoveryMode) {
        StringBuilder builder = new StringBuilder("Restored your saved settings and turned AutoTune off.");
        appendRuntimeNotes(builder, notes);
        if (clearedRecoveryMode) {
            builder.append(" Recovery mode was cleared, but auto-apply still stays off until you turn it back on.");
        }
        return builder.toString();
    }

    private static void appendRuntimeNotes(StringBuilder builder, ApplyRuntimeNotes notes) {
        if (notes.keptCurrentGraphicsModeInWorld()) {
            builder.append(" Graphics mode stayed unchanged in-world because switching away from Fabulous is unstable there.");
        } else if (notes.usedFancyForSodiumCompatibility()) {
            builder.append(" Used Fancy instead of Fabulous for Sodium compatibility.");
        } else if (notes.usedFancyInsteadOfFabulousInWorld()) {
            builder.append(" Used Fancy instead of Fabulous in-world for stability.");
        }
        if (notes.usedSafeSimulationDistanceFallback()) {
            builder.append(" Used a safe simulation distance fallback.");
        }
    }

    private static String appendRecoveryClearedSuffix(String message, boolean clearedRecoveryMode) {
        if (!clearedRecoveryMode) {
            return message;
        }
        return message + " Recovery mode was cleared, but auto-apply still stays off until you turn it back on.";
    }

    private void appendPresetGuideLine(List<String> lines, HardwareProfile profile, PresetProfile preset) {
        StringBuilder label = new StringBuilder("- ");
        label.append(preset.commandName()).append(": ").append(preset.summary());
        if (profile.recommendedPreset() == preset) {
            label.append(" Recommended.");
        }
        if (config.selectedPreset == preset) {
            label.append(" Selected.");
        }
        lines.add(label.toString());
    }

    private static String describeTier(HardwareTier tier) {
        return switch (tier) {
            case LOW -> "safer/lighter defaults";
            case MID -> "balanced defaults";
            case HIGH -> "higher visual headroom";
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
        USED_FANCY_FOR_SODIUM,
        USED_FANCY_IN_WORLD,
        KEPT_CURRENT_IN_WORLD
    }

    private record ApplyRuntimeNotes(
        boolean usedFancyForSodiumCompatibility,
        boolean keptCurrentGraphicsModeInWorld,
        boolean usedFancyInsteadOfFabulousInWorld,
        boolean usedSafeSimulationDistanceFallback
    ) {
    }
}
