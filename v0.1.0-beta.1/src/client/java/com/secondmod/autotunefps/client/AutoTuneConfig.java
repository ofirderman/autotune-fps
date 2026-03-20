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

public final class AutoTuneConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve(AutoTuneFps.MOD_ID + ".json");

    public PresetProfile selectedPreset = PresetProfile.BALANCED;
    public boolean applyPresetOnWorldJoin = false;

    public static AutoTuneConfig load() {
        if (Files.notExists(CONFIG_PATH)) {
            AutoTuneConfig config = new AutoTuneConfig();
            config.save();
            return config;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            AutoTuneConfig config = GSON.fromJson(reader, AutoTuneConfig.class);
            if (config == null) {
                throw new JsonParseException("Config file was empty");
            }

            config.sanitize();
            return config;
        } catch (IOException | JsonParseException exception) {
            AutoTuneFps.LOGGER.warn("Failed to load config, falling back to defaults", exception);
            AutoTuneConfig config = new AutoTuneConfig();
            config.save();
            return config;
        }
    }

    public void save() {
        sanitize();

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            AutoTuneFps.LOGGER.warn("Failed to save config", exception);
        }
    }

    private void sanitize() {
        if (selectedPreset == null) {
            selectedPreset = PresetProfile.BALANCED;
        }
    }
}
