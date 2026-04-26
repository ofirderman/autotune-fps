package com.secondmod.autotunefps.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Supplier;

public final class PresetSelectionScreen extends Screen {
    private final Screen lastScreen;
    private final PresetApplier presetApplier;
    private HardwareProfile hardwareProfile;

    public PresetSelectionScreen(Screen lastScreen, PresetApplier presetApplier) {
        super(Component.literal("AutoTune Presets"));
        this.lastScreen = lastScreen;
        this.presetApplier = presetApplier;
    }

    @Override
    protected void init() {
        if (this.minecraft != null) {
            this.hardwareProfile = this.presetApplier.detectProfile(this.minecraft);
        }

        int buttonWidth = 220;
        int buttonHeight = 18;
        int x = (this.width - buttonWidth) / 2;
        int y = 79;
        int buttonStep = 19;

        addPresetButton(PresetProfile.ULTIMATE_PERFORMANCE, x, y, buttonWidth, buttonHeight);
        addPresetButton(PresetProfile.PERFORMANCE, x, y + buttonStep, buttonWidth, buttonHeight);
        addPresetButton(PresetProfile.BALANCED, x, y + buttonStep * 2, buttonWidth, buttonHeight);
        addPresetButton(PresetProfile.QUALITY, x, y + buttonStep * 3, buttonWidth, buttonHeight);
        addPresetButton(PresetProfile.OFF, x, y + buttonStep * 4, buttonWidth, buttonHeight);
        this.addRenderableWidget(Button.builder(Component.literal(formatAutoApplyButtonLabel()), this::onAutoApplyButtonPressed)
            .bounds(x, y + buttonStep * 5, buttonWidth, buttonHeight)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal(formatAggressiveModeButtonLabel()), this::onAggressiveModeButtonPressed)
            .bounds(x, y + buttonStep * 6, buttonWidth, buttonHeight)
            .build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), this::onDoneButtonPressed)
            .bounds(x, y + buttonStep * 7, buttonWidth, buttonHeight)
            .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Avoid the built-in blur background path because newer 1.21-1.21.11 only allows one blur pass per frame.
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, "AutoTune Presets", this.width / 2, 12, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(
            this.font,
            "Detected hardware: " + formatDetectedTierLabel(),
            this.width / 2,
            26,
            0xFFA0A0A0
        );
        guiGraphics.drawCenteredString(
            this.font,
            "Recommended: " + formatRecommendedInfoLabel(),
            this.width / 2,
            38,
            0xFFA0A0A0
        );
        guiGraphics.drawCenteredString(
            this.font,
            "Selected: " + formatSelectedPresetLabel(),
            this.width / 2,
            50,
            0xFFA0A0A0
        );
    }

    private void addPresetButton(PresetProfile preset, int x, int y, int width, int height) {
        Button presetButton = Button.builder(Component.literal(formatPresetButtonLabel(preset)), button -> {
                runMenuAction(
                    "Failed to apply the " + preset.displayName() + " preset. AutoTune left your current settings unchanged.",
                    () -> this.presetApplier.applyPresetAndRemember(this.minecraft, preset)
                );
            })
            .bounds(x, y, width, height)
            .build();
        if (preset != PresetProfile.OFF) {
            presetButton.active = this.presetApplier.config().selectedPreset != preset;
        }
        this.addRenderableWidget(presetButton);
    }

    private String formatPresetButtonLabel(PresetProfile preset) {
        if (preset == PresetProfile.OFF) {
            return "Restore (" + formatRestoreStateLabel() + ")";
        }

        String baseLabel = preset.displayName();
        boolean isSelected = this.presetApplier.config().selectedPreset == preset;
        boolean isRecommended = this.hardwareProfile != null
            && this.hardwareProfile.recommendedPreset() == preset;
        if (isRecommended && isSelected) {
            return baseLabel + " (Recommended, Selected)";
        }
        if (isRecommended) {
            return baseLabel + " (Recommended)";
        }
        if (isSelected) {
            return baseLabel + " (Selected)";
        }
        return baseLabel;
    }

    private String formatRecommendedInfoLabel() {
        if (this.hardwareProfile == null) {
            return "Unknown";
        }
        return this.hardwareProfile.recommendedPreset().displayName();
    }

    private String formatDetectedTierLabel() {
        if (this.hardwareProfile == null) {
            return "Unknown";
        }
        String tierName = this.hardwareProfile.tier().name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(tierName.charAt(0)) + tierName.substring(1) + " tier";
    }

    private String formatRestoreStateLabel() {
        return this.presetApplier.hasRestoreSnapshot() ? "Available" : "None";
    }

    private String formatSelectedPresetLabel() {
        PresetProfile selectedPreset = this.presetApplier.config().selectedPreset;
        if (selectedPreset == null || !selectedPreset.isEnabled()) {
            return "None";
        }
        return selectedPreset.displayName();
    }

    private String formatAutoApplyButtonLabel() {
        return "Auto-apply: " + (this.presetApplier.isAutoApplyEnabled() ? "On" : "Off");
    }

    private String formatAggressiveModeButtonLabel() {
        return "Aggressive mode: " + (this.presetApplier.isAggressiveModeEnabled() ? "On" : "Off");
    }

    private void onAutoApplyButtonPressed(Button button) {
        toggleAutoApply();
    }

    private void onAggressiveModeButtonPressed(Button button) {
        toggleAggressiveMode();
    }

    private void onDoneButtonPressed(Button button) {
        onClose();
    }

    private void toggleAutoApply() {
        if (this.minecraft == null) {
            return;
        }

        String message = this.presetApplier.toggleAutoApply();
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(message), false);
        }
        this.minecraft.setScreen(new PresetSelectionScreen(this.lastScreen, this.presetApplier));
    }

    private void toggleAggressiveMode() {
        if (this.minecraft == null) {
            return;
        }

        String message = this.presetApplier.toggleAggressiveMode();
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(message), false);
        }
        this.minecraft.setScreen(new PresetSelectionScreen(this.lastScreen, this.presetApplier));
    }

    private void runMenuAction(String failureMessage, Supplier<String> action) {
        if (this.minecraft == null) {
            return;
        }

        net.minecraft.client.Minecraft client = this.minecraft;
        this.onClose();
        OptimizerCommands.schedulePresetApply(client, failureMessage, action);
    }
}
