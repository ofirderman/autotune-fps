package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    public SmartPresetRecommendation smartRecommendation(Minecraft client) {
        HardwareProfile profile = detectProfile(client);
        PresetTarget currentTarget = client == null ? null : captureCurrentPresetTarget(client);
        return buildSmartRecommendation(profile, currentTarget);
    }

    public boolean hasRestoreSnapshot() {
        return runtimeState.hasRestoreSnapshot();
    }

    public boolean isAutoApplyEnabled() {
        return autoApplyMode() != AutoApplyMode.OFF;
    }

    public AutoApplyMode autoApplyMode() {
        return config.autoApplyMode == null ? AutoApplyMode.OFF : config.autoApplyMode;
    }

    public String toggleAutoApply() {
        return setAutoApplyMode(nextAutoApplyMode());
    }

    public boolean isSavedPresetCurrentlyApplied(Minecraft client) {
        if (client == null || !config.selectedPreset.isEnabled()) {
            return false;
        }
        PresetTarget target = resolveAdaptiveTarget(detectProfile(client), config.selectedPreset);
        return captureCurrentPresetTarget(client).equals(target);
    }

    public String currentModeLabel(Minecraft client) {
        if (!config.selectedPreset.isEnabled()) {
            return "Custom settings";
        }
        if (client == null) {
            return "Saved preset selected (settings unavailable)";
        }
        PresetTarget currentTarget = captureCurrentPresetTarget(client);
        PresetTarget lastSmartTarget = runtimeState.lastSmartTarget();
        String smartModeLabel = runtimeState.smartModeLabel();
        if (runtimeState.lastSmartBasePreset == config.selectedPreset
            && lastSmartTarget != null
            && smartModeLabel != null
            && currentTarget.equals(lastSmartTarget)) {
            return smartModeLabel;
        }
        if (isSavedPresetCurrentlyApplied(client)) {
            return config.selectedPreset.displayName() + " preset";
        }
        return "Custom settings";
    }

    public String[] describeStatusLines(Minecraft client) {
        HardwareProfile profile = detectProfile(client);
        SmartPresetRecommendation smartRecommendation = buildSmartRecommendation(
            profile,
            client == null ? null : captureCurrentPresetTarget(client)
        );
        List<String> lines = new ArrayList<>();
        lines.add("AutoTune FPS status");
        lines.add("- Mode: " + currentModeLabel(client));
        if (config.selectedPreset.isEnabled()) {
            lines.add("- Saved preset: " + config.selectedPreset.displayName());
        }
        lines.add("- Recommended preset: " + smartRecommendation.basePreset().displayName());
        lines.add("- Auto-apply: " + autoApplyMode().displayName());
        lines.add("- Restore: " + runtimeState.describeRestoreState());
        lines.add("- Hardware: " + displayTier(profile.tier()));
        String recoveryState = runtimeState.describeRecoveryState();
        if (recoveryState != null) {
            lines.add("- Recovery: " + recoveryState);
        }
        if (isGraphicsModeUnavailable(client)) {
            lines.add("- Graphics: unavailable on this Minecraft version; skipped");
        }
        lines.add("- Renderer: " + displayValue(profile.renderer()));
        return lines.toArray(String[]::new);
    }

    public String[] previewPresetLines(Minecraft client, PresetProfile preset) {
        HardwareProfile profile = detectProfile(client);
        List<String> lines = new ArrayList<>();
        if (!preset.isEnabled()) {
            lines.add("Preview: Restore");
            lines.add("- Restore your original settings from before AutoTune FPS changed them.");
            lines.add("- Restore: " + runtimeState.describeRestoreState());
            lines.add("- Preview only. Nothing changes yet.");
            return lines.toArray(String[]::new);
        }

        PresetTarget target = resolveAdaptiveTarget(profile, preset);
        PresetTarget currentTarget = client == null ? null : captureCurrentPresetTarget(client);
        lines.add("Preview: " + preset.displayName());
        if (currentTarget == null) {
            lines.add("- Current settings unavailable.");
        } else {
            appendChangedSettingLines(lines, currentTarget, target);
            if (countChangedSettings(currentTarget, target) == 0) {
                lines.add("- Already matches this target.");
            }
        }
        lines.add("- Preview only. Nothing changes yet.");
        return lines.toArray(String[]::new);
    }

    public String[] previewRecommendedPresetLines(Minecraft client) {
        return previewSmartRecommendedPresetLines(client);
    }

    public String[] previewSmartRecommendedPresetLines(Minecraft client) {
        SmartPresetRecommendation recommendation = smartRecommendation(client);
        PresetTarget currentTarget = client == null ? null : captureCurrentPresetTarget(client);
        List<String> lines = new ArrayList<>();
        lines.add("Recommendation preview: " + recommendation.basePreset().displayName());
        if (currentTarget == null) {
            lines.add("- Current settings unavailable.");
        } else {
            appendChangedSettingLines(lines, currentTarget, recommendation.target());
            if (recommendation.changeCount() == 0) {
                lines.add("- Already matches this target.");
            }
        }
        lines.add("- Preview only. Nothing changes yet.");
        return lines.toArray(String[]::new);
    }

    public String[] describePresetGuideLines(Minecraft client) {
        HardwareProfile profile = detectProfile(client);
        List<String> lines = new ArrayList<>();
        lines.add("AutoTune presets");
        lines.add("- Hardware tier: " + displayTier(profile.tier()));
        lines.add("- Recommended preset: " + smartRecommendation(client).basePreset().displayName());
        appendPresetGuideLine(lines, PresetProfile.ULTIMATE_PERFORMANCE);
        appendPresetGuideLine(lines, PresetProfile.PERFORMANCE);
        appendPresetGuideLine(lines, PresetProfile.BALANCED);
        appendPresetGuideLine(lines, PresetProfile.QUALITY);
        lines.add("- restore: " + PresetProfile.OFF.summary());
        lines.add("- restore: " + (runtimeState.hasRestoreSnapshot() ? "Available" : "Unavailable"));
        lines.add("- Tip: use /optimizer preview recommended or /optimizer preview <preset> before applying.");
        return lines.toArray(String[]::new);
    }

    public String applyConfiguredPresetOnJoin(Minecraft client) {
        showRecoveryNoticeIfNeeded(client);
        AutoApplyMode mode = autoApplyMode();
        if (mode == AutoApplyMode.OFF) {
            return null;
        }

        if (mode == AutoApplyMode.SMART_RECOMMENDATION) {
            return applySmartRecommended(client, "autoapply_smart");
        }
        if (!config.selectedPreset.isEnabled()) {
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
        return applySmartRecommendedAndRemember(client);
    }

    public String applySmartRecommendedAndRemember(Minecraft client) {
        return applySmartRecommended(client, "recommend");
    }

    private String applySmartRecommended(Minecraft client, String source) {
        ensureRestoreSnapshot(client);
        SmartPresetRecommendation recommendation = smartRecommendation(client);
        runtimeState.markPendingApply("smart_" + recommendation.basePreset().commandName(), source);
        HardwareProfile profile = detectProfile(client);
        ApplyRuntimeNotes notes = applyPresetTarget(client, recommendation.target(), profile.sodiumLoaded());
        client.options.save();
        config.selectedPreset = recommendation.basePreset();
        config.save();
        runtimeState.rememberSmartPreset(recommendation);
        runtimeState.clearPendingApply();
        boolean clearedRecoveryMode = runtimeState.clearRecoveryMode();
        return describeAppliedPreset(
            "Applied recommendation: " + recommendation.basePreset().displayName() + ".",
            notes,
            clearedRecoveryMode
        );
    }

    public String setAutoApply(boolean enabled) {
        return setAutoApplyMode(enabled ? AutoApplyMode.SELECTED_PRESET : AutoApplyMode.OFF);
    }

    public String setAutoApplyMode(AutoApplyMode mode) {
        config.autoApplyMode = mode == null ? AutoApplyMode.OFF : mode;
        config.applyPresetOnWorldJoin = config.autoApplyMode != AutoApplyMode.OFF;
        config.save();
        if (config.autoApplyMode != AutoApplyMode.OFF && runtimeState.clearRecoveryMode()) {
            return "Auto-apply: " + config.autoApplyMode.displayName() + ". Recovery mode was cleared.";
        }
        return "Auto-apply: " + config.autoApplyMode.displayName() + ".";
    }

    private AutoApplyMode nextAutoApplyMode() {
        return switch (autoApplyMode()) {
            case OFF -> AutoApplyMode.SELECTED_PRESET;
            case SELECTED_PRESET -> AutoApplyMode.SMART_RECOMMENDATION;
            case SMART_RECOMMENDATION -> AutoApplyMode.OFF;
        };
    }

    public void ensureRestoreSnapshotSaved(Minecraft client) {
        ensureRestoreSnapshot(client);
    }

    private PresetTarget captureCurrentPresetTarget(Minecraft client) {
        Options options = client.options;
        Integer renderDistance = options.renderDistance().get();
        Integer simulationDistance = options.simulationDistance().get();
        Double entityDistanceScaling = options.entityDistanceScaling().get();
        Integer biomeBlendRadius = options.biomeBlendRadius().get();
        Boolean entityShadows = options.entityShadows().get();

        return new PresetTarget(
            currentEnumNameOrDefault(getGraphicsModeOption(options), "FANCY"),
            currentEnumNameOrDefault(options.cloudStatus(), "FAST"),
            currentEnumNameOrDefault(options.particles(), "DECREASED"),
            renderDistance == null ? 8 : renderDistance,
            simulationDistance == null ? 5 : simulationDistance,
            entityDistanceScaling == null ? 1.0D : entityDistanceScaling,
            biomeBlendRadius == null ? 0 : biomeBlendRadius,
            entityShadows == null || entityShadows,
            currentEnumNameOrDefault(options.prioritizeChunkUpdates(), "PLAYER_AFFECTED")
        );
    }

    private String applyPreset(Minecraft client, PresetProfile preset, String source) {
        ensureRestoreSnapshot(client);
        runtimeState.markPendingApply(preset.commandName(), source);
        HardwareProfile profile = detectProfile(client);
        PresetTarget target = resolveAdaptiveTarget(profile, preset);
        ApplyRuntimeNotes notes = applyPresetTarget(client, target, profile.sodiumLoaded());
        client.options.save();
        runtimeState.clearPendingApply();
        runtimeState.clearSmartPreset();
        boolean clearedRecoveryMode = runtimeState.clearRecoveryMode();
        return describeAppliedPreset(
            "Applied " + preset.displayName() + " preset.",
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
        runtimeState.clearSmartPreset();
        if (restoreSnapshot == null) {
            return appendRecoveryClearedSuffix(
                "No saved settings to restore. AutoTune is now off.",
                clearedRecoveryMode
            );
        }

        ApplyRuntimeNotes notes = restoreSavedSettings(client, restoreSnapshot, detectProfile(client).sodiumLoaded());
        client.options.save();
        return describeRestoredSettings(notes, clearedRecoveryMode);
    }

    private void saveRestoreSnapshot(Minecraft client) {
        runtimeState.saveRestoreSnapshot(captureRestoreSnapshot(client.options));
    }

    private void ensureRestoreSnapshot(Minecraft client) {
        if (!runtimeState.hasRestoreSnapshot()) {
            saveRestoreSnapshot(client);
        }
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
            graphicsModeOutcome == GraphicsModeOutcome.SKIPPED_UNAVAILABLE,
            usedSafeSimulationDistanceFallback
        );
    }

    private static PresetTarget resolveAdaptiveTarget(HardwareProfile profile, PresetProfile preset) {
        return switch (profile.presetBand()) {
            case ENTRY -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 6, 5, 0.5D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 7, 5, 0.5D, 0, false, "NEARBY");
                case BALANCED -> new PresetTarget("FAST", "FAST", "DECREASED", 8, 5, 0.5D, 1, false, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget("FANCY", "FAST", "DECREASED", 10, 6, 0.75D, 2, false, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
            case LOW_MID -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 7, 5, 0.5D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 8, 5, 0.5D, 0, false, "NEARBY");
                case BALANCED -> new PresetTarget("FAST", "FAST", "DECREASED", 10, 6, 0.75D, 2, false, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget("FANCY", "FAST", "DECREASED", 12, 7, 1.0D, 3, true, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
            case MID -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 7, 5, 0.75D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 10, 6, 0.75D, 2, false, "NEARBY");
                case BALANCED -> new PresetTarget("FANCY", "FAST", "DECREASED", 12, 8, 1.0D, 3, true, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget("FANCY", "FANCY", "ALL", 16, 10, 1.25D, 5, true, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
            case UPPER_MID -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 8, 5, 0.75D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "DECREASED", 11, 7, 1.0D, 3, true, "NEARBY");
                case BALANCED -> new PresetTarget("FANCY", "FANCY", "ALL", 16, 9, 1.25D, 4, true, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget("FANCY", "FANCY", "ALL", 20, 11, 1.5D, 6, true, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
            case HIGH_END -> switch (preset) {
                case ULTIMATE_PERFORMANCE -> new PresetTarget("FAST", "OFF", "MINIMAL", 8, 6, 0.75D, 0, false, "NEARBY");
                case PERFORMANCE -> new PresetTarget("FAST", "OFF", "DECREASED", 12, 8, 1.0D, 3, true, "NEARBY");
                case BALANCED -> new PresetTarget("FANCY", "FANCY", "ALL", 18, 10, 1.25D, 5, true, "PLAYER_AFFECTED");
                case QUALITY -> new PresetTarget("FANCY", "FANCY", "ALL", 24, 12, 1.5D, 7, true, "PLAYER_AFFECTED");
                case OFF -> throw new IllegalStateException("OFF should not resolve to a preset target");
            };
        };
    }

    private SmartPresetRecommendation buildSmartRecommendation(HardwareProfile profile, PresetTarget currentTarget) {
        PresetProfile basePreset = chooseSmartBasePreset(profile);
        PresetTarget target = resolveAdaptiveTarget(profile, basePreset);
        int changeCount = currentTarget == null ? 0 : countChangedSettings(currentTarget, target);
        return new SmartPresetRecommendation(basePreset, target, changeCount);
    }

    private static PresetProfile chooseSmartBasePreset(HardwareProfile profile) {
        return switch (profile.presetBand()) {
            case ENTRY, LOW_MID -> PresetProfile.PERFORMANCE;
            case MID, UPPER_MID -> PresetProfile.BALANCED;
            case HIGH_END -> profile.shadersActive() || profile.hasRiskFlags()
                ? PresetProfile.BALANCED
                : PresetProfile.QUALITY;
        };
    }

    private static int countChangedSettings(PresetTarget currentTarget, PresetTarget target) {
        int count = 0;
        count += sameValue(currentTarget.graphicsMode(), target.graphicsMode()) ? 0 : 1;
        count += sameValue(currentTarget.clouds(), target.clouds()) ? 0 : 1;
        count += sameValue(currentTarget.particles(), target.particles()) ? 0 : 1;
        count += currentTarget.renderDistance() == target.renderDistance() ? 0 : 1;
        count += currentTarget.simulationDistance() == target.simulationDistance() ? 0 : 1;
        count += Double.compare(currentTarget.entityDistanceScaling(), target.entityDistanceScaling()) == 0 ? 0 : 1;
        count += currentTarget.biomeBlend() == target.biomeBlend() ? 0 : 1;
        count += currentTarget.entityShadows() == target.entityShadows() ? 0 : 1;
        count += sameValue(currentTarget.chunkUpdatePriority(), target.chunkUpdatePriority()) ? 0 : 1;
        return count;
    }

    private static void appendChangedSettingLines(List<String> lines, PresetTarget currentTarget, PresetTarget target) {
        appendChangedLine(lines, "Graphics", displayEnum(currentTarget.graphicsMode()), displayEnum(target.graphicsMode()));
        appendChangedLine(lines, "Clouds", displayEnum(currentTarget.clouds()), displayEnum(target.clouds()));
        appendChangedLine(lines, "Particles", displayEnum(currentTarget.particles()), displayEnum(target.particles()));
        appendChangedLine(lines, "Render Distance", Integer.toString(currentTarget.renderDistance()), Integer.toString(target.renderDistance()));
        appendChangedLine(lines, "Simulation Distance", Integer.toString(currentTarget.simulationDistance()), Integer.toString(target.simulationDistance()));
        appendChangedLine(lines, "Entity Distance", formatDouble(currentTarget.entityDistanceScaling()), formatDouble(target.entityDistanceScaling()));
        appendChangedLine(lines, "Biome Blend", Integer.toString(currentTarget.biomeBlend()), Integer.toString(target.biomeBlend()));
        appendChangedLine(lines, "Entity Shadows", formatBoolean(currentTarget.entityShadows()), formatBoolean(target.entityShadows()));
        appendChangedLine(lines, "Chunk Updates", displayEnum(currentTarget.chunkUpdatePriority()), displayEnum(target.chunkUpdatePriority()));
    }

    private static void appendChangedLine(List<String> lines, String label, String currentValue, String targetValue) {
        if (!sameValue(currentValue, targetValue)) {
            lines.add("- " + label + ": " + currentValue + " -> " + targetValue);
        }
    }

    private static boolean sameValue(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private static String displayEnum(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        String[] words = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private static String formatBoolean(boolean value) {
        return value ? "On" : "Off";
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0$", "").replaceAll("\\.$", ".0");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
            graphicsModeOutcome == GraphicsModeOutcome.SKIPPED_UNAVAILABLE,
            usedSafeSimulationDistanceFallback
        );
    }

    private static GraphicsModeOutcome setGraphicsModeOption(Minecraft client, OptionInstance<?> option, String enumConstantName, boolean sodiumLoaded) {
        if (option == null) {
            AutoTuneFps.LOGGER.warn("Graphics mode option is unavailable on this Minecraft version; skipped requested graphics mode {}", enumConstantName);
            return GraphicsModeOutcome.SKIPPED_UNAVAILABLE;
        }
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
        if (option == null) {
            return;
        }
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
            "graphicsPreset",
            "getGraphicsMode",
            "getPreset",
            "method_42534",
            "method_75329");
        if (option != null) {
            return option;
        }

        AutoTuneFps.LOGGER.warn("AutoTune could not find the graphics mode option on this Minecraft version.");
        return null;
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
        if (option == null) {
            return false;
        }
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
        if (option == null) {
            return null;
        }
        Object current = option.get();
        if (current instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return null;
    }

    private static String currentEnumNameOrDefault(OptionInstance<?> option, String fallback) {
        String currentValue = currentEnumName(option);
        if (currentValue == null || currentValue.isBlank()) {
            return fallback;
        }
        return currentValue;
    }

    private static String effectiveGraphicsModeForTarget(Minecraft client, String requestedGraphicsMode, boolean sodiumLoaded) {
        String currentGraphicsMode = currentEnumNameOrDefault(getGraphicsModeOption(client.options), "FANCY");
        if (client.level != null && "FABULOUS".equals(currentGraphicsMode) && !"FABULOUS".equals(requestedGraphicsMode)) {
            return currentGraphicsMode;
        }
        if ("FABULOUS".equals(requestedGraphicsMode) && sodiumLoaded) {
            return "FANCY";
        }
        if ("FABULOUS".equals(requestedGraphicsMode) && client.level != null && !"FABULOUS".equals(currentGraphicsMode)) {
            return "FANCY";
        }
        return requestedGraphicsMode;
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
        StringBuilder builder = new StringBuilder("Restored your original settings from before AutoTune FPS changed them and turned AutoTune off.");
        appendRuntimeNotes(builder, notes);
        if (clearedRecoveryMode) {
            builder.append(" Recovery mode was cleared, but auto-apply still stays off until you turn it back on.");
        }
        return builder.toString();
    }

    private static void appendRuntimeNotes(StringBuilder builder, ApplyRuntimeNotes notes) {
        if (notes.keptCurrentGraphicsModeInWorld()) {
            builder.append(" Graphics mode stayed unchanged in-world to avoid a disruptive video reload.");
        } else if (notes.usedFancyForSodiumCompatibility()) {
            builder.append(" Used Fancy instead of Fabulous for Sodium compatibility.");
        } else if (notes.usedFancyInsteadOfFabulousInWorld()) {
            builder.append(" Used Fancy instead of Fabulous in-world for stability.");
        } else if (notes.skippedUnavailableGraphicsMode()) {
            builder.append(" Graphics mode was unavailable on this Minecraft version, so that setting was skipped.");
        }
        if (notes.usedSafeSimulationDistanceFallback()) {
            builder.append(" Used a simulation distance fallback.");
        }
    }

    private static String appendRecoveryClearedSuffix(String message, boolean clearedRecoveryMode) {
        if (!clearedRecoveryMode) {
            return message;
        }
        return message + " Recovery mode was cleared, but auto-apply still stays off until you turn it back on.";
    }

    private void appendPresetGuideLine(List<String> lines, PresetProfile preset) {
        StringBuilder label = new StringBuilder("- ");
        label.append(preset.commandName()).append(": ").append(preset.summary());
        if (config.selectedPreset == preset) {
            label.append(" Selected.");
        }
        lines.add(label.toString());
    }

    private static String displayTier(HardwareTier tier) {
        return switch (tier) {
            case LOW -> "Low";
            case MID -> "Middle";
            case HIGH -> "High";
        };
    }

    private static String displayValue(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }

    private enum GraphicsModeOutcome {
        APPLIED_REQUESTED,
        USED_FANCY_FOR_SODIUM,
        USED_FANCY_IN_WORLD,
        KEPT_CURRENT_IN_WORLD,
        SKIPPED_UNAVAILABLE
    }

    private record ApplyRuntimeNotes(
        boolean usedFancyForSodiumCompatibility,
        boolean keptCurrentGraphicsModeInWorld,
        boolean usedFancyInsteadOfFabulousInWorld,
        boolean skippedUnavailableGraphicsMode,
        boolean usedSafeSimulationDistanceFallback
    ) {
    }

    private static boolean isGraphicsModeUnavailable(Minecraft client) {
        return client != null && getGraphicsModeOption(client.options) == null;
    }
}
