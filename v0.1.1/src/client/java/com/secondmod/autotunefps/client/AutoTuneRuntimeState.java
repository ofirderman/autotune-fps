package com.secondmod.autotunefps.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.secondmod.autotunefps.AutoTuneFps;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AutoTuneRuntimeState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve(AutoTuneFps.MOD_ID + "_state.json");

    public boolean pendingApply = false;
    public String pendingPreset = null;
    public String pendingSource = null;
    public boolean recoveryModeActive = false;
    public boolean recoveryNoticePending = false;
    public String lastInterruptedPreset = null;
    public String lastInterruptedSource = null;

    public static AutoTuneRuntimeState load() {
        if (Files.notExists(STATE_PATH)) {
            return new AutoTuneRuntimeState();
        }

        try (Reader reader = Files.newBufferedReader(STATE_PATH)) {
            AutoTuneRuntimeState state = GSON.fromJson(reader, AutoTuneRuntimeState.class);
            if (state == null) {
                throw new JsonParseException("Runtime state file was empty");
            }

            state.sanitize();
            return state;
        } catch (IOException | JsonParseException exception) {
            AutoTuneFps.LOGGER.warn("Failed to load runtime state, starting clean", exception);
            return new AutoTuneRuntimeState();
        }
    }

    public void save() {
        sanitize();

        try {
            Files.createDirectories(STATE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(STATE_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            AutoTuneFps.LOGGER.warn("Failed to save runtime state", exception);
        }
    }

    public void markPendingApply(String presetName, String source) {
        pendingApply = true;
        pendingPreset = normalizePreset(presetName);
        pendingSource = normalizeSource(source);
        save();
    }

    public void clearPendingApply() {
        if (!pendingApply && pendingPreset == null && pendingSource == null) {
            return;
        }

        pendingApply = false;
        pendingPreset = null;
        pendingSource = null;
        save();
    }

    public String recoverIfInterrupted(AutoTuneConfig config) {
        if (!pendingApply) {
            return null;
        }

        lastInterruptedPreset = pendingPreset;
        lastInterruptedSource = pendingSource;
        recoveryModeActive = true;
        recoveryNoticePending = true;
        pendingApply = false;
        pendingPreset = null;
        pendingSource = null;

        if (config.applyPresetOnWorldJoin) {
            config.applyPresetOnWorldJoin = false;
            config.save();
        }

        save();
        return describeInterruptedApply();
    }

    public String consumeRecoveryNotice() {
        if (!recoveryNoticePending) {
            return null;
        }

        recoveryNoticePending = false;
        save();
        return "AutoTune paused auto-apply after an interrupted preset apply. Check /optimizer status.";
    }

    public String describeRecoveryState() {
        if (!recoveryModeActive) {
            return null;
        }

        StringBuilder builder = new StringBuilder("auto-apply paused after an interrupted apply");
        String interruptedApply = describeInterruptedApply();
        if (interruptedApply != null) {
            builder.append(" (").append(interruptedApply).append(")");
        }
        return builder.toString();
    }

    public boolean clearRecoveryMode() {
        if (!recoveryModeActive && !recoveryNoticePending && lastInterruptedPreset == null && lastInterruptedSource == null) {
            return false;
        }

        recoveryModeActive = false;
        recoveryNoticePending = false;
        lastInterruptedPreset = null;
        lastInterruptedSource = null;
        save();
        return true;
    }

    private String describeInterruptedApply() {
        String source = displayValue(lastInterruptedSource);
        String preset = displayValue(lastInterruptedPreset);
        if (source == null && preset == null) {
            return null;
        }
        if (source == null) {
            return "last preset: " + preset;
        }
        if (preset == null) {
            return "source: " + source;
        }
        return "source: " + source + ", preset: " + preset;
    }

    private static String displayValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String normalizePreset(String presetName) {
        if (presetName == null || presetName.isBlank()) {
            return null;
        }
        return presetName;
    }

    private static String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return source;
    }

    private void sanitize() {
        pendingPreset = normalizePreset(pendingPreset);
        pendingSource = normalizeSource(pendingSource);
        lastInterruptedPreset = normalizePreset(lastInterruptedPreset);
        lastInterruptedSource = normalizeSource(lastInterruptedSource);
        if (!recoveryModeActive && !recoveryNoticePending && lastInterruptedPreset == null && lastInterruptedSource == null) {
            return;
        }
    }
}
