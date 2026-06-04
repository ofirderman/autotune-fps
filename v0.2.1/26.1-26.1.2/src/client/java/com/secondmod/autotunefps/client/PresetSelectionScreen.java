package com.secondmod.autotunefps.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public final class PresetSelectionScreen extends Screen {
    private final Screen lastScreen;
    private final AutoTuneClientCoordinator coordinator;
    private final PresetApplier presetApplier;
    private HardwareProfile hardwareProfile;
    private SmartPresetRecommendation smartRecommendation;

    public PresetSelectionScreen(Screen lastScreen, AutoTuneClientCoordinator coordinator) {
        super(Component.literal("AutoTune Presets"));
        this.lastScreen = lastScreen;
        this.coordinator = coordinator;
        this.presetApplier = coordinator.presetApplier();
    }

    @Override
    protected void init() {
        if (this.minecraft != null) {
            this.hardwareProfile = this.presetApplier.detectProfile(this.minecraft);
            this.smartRecommendation = this.presetApplier.smartRecommendation(this.minecraft);
        }

        int buttonWidth = 220;
        int buttonHeight = 18;
        int x = (this.width - buttonWidth) / 2;
        int y = 91;
        int buttonStep = 19;
        int smartButtonY = 67;
        int smartButtonWidth = 70;
        int smartButtonGap = 5;

        this.addRenderableWidget(Button.builder(Component.literal("Apply rec"), this::onSmartApplyButtonPressed)
            .bounds(x, smartButtonY, smartButtonWidth, buttonHeight)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal("Preview rec"), this::onSmartPreviewButtonPressed)
            .bounds(x + smartButtonWidth + smartButtonGap, smartButtonY, smartButtonWidth, buttonHeight)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal("Re-scan"), this::onSmartRescanButtonPressed)
            .bounds(x + (smartButtonWidth + smartButtonGap) * 2, smartButtonY, smartButtonWidth, buttonHeight)
            .build());

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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, Component.literal("AutoTune Presets"), this.width / 2, 12, 0xFFFFFFFF);
        guiGraphics.centeredText(
            this.font,
            Component.literal("Recommended preset: " + formatSmartRecommendationLabel()),
            this.width / 2,
            30,
            0xFFA0A0A0
        );
    }

    private void addPresetButton(PresetProfile preset, int x, int y, int width, int height) {
        Button presetButton = Button.builder(Component.literal(formatPresetButtonLabel(preset)), button ->
                runMenuAction(
                    "Failed to apply the " + preset.displayName() + " preset. AutoTune left your current settings unchanged.",
                    () -> this.presetApplier.applyPresetAndRemember(this.minecraft, preset)
                ))
            .bounds(x, y, width, height)
            .build();
        if (preset != PresetProfile.OFF) {
            presetButton.active = !isSelectedPreset(preset);
        }
        this.addRenderableWidget(presetButton);
    }

    private boolean isSelectedPreset(PresetProfile preset) {
        return this.presetApplier.config().selectedPreset == preset;
    }

    private boolean isRecommendedPreset(PresetProfile preset) {
        return this.smartRecommendation != null && this.smartRecommendation.basePreset() == preset;
    }

    private String formatPresetButtonLabel(PresetProfile preset) {
        if (preset == PresetProfile.OFF) {
            return "Restore";
        }
        boolean selected = isSelectedPreset(preset);
        boolean recommended = isRecommendedPreset(preset);
        if (selected && recommended) {
            return preset.displayName() + " (Current, Recommended)";
        }
        if (selected) {
            return preset.displayName() + " (Current)";
        }
        if (recommended) {
            return preset.displayName() + " (Recommended)";
        }
        return preset.displayName();
    }

    private String formatSmartRecommendationLabel() {
        return this.smartRecommendation == null ? "Unknown" : this.smartRecommendation.basePreset().displayName();
    }

    private String formatAutoApplyButtonLabel() {
        return "Auto-apply: " + this.presetApplier.autoApplyMode().displayName();
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

    private void onSmartApplyButtonPressed(Button button) {
        runMenuAction(
            "Failed to apply the recommended preset. AutoTune left your current settings unchanged.",
            () -> this.presetApplier.applySmartRecommendedAndRemember(this.minecraft)
        );
    }

    private void onSmartPreviewButtonPressed(Button button) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        String[] lines = this.presetApplier.previewSmartRecommendedPresetLines(this.minecraft);
        this.onClose();
        for (String line : lines) {
            this.minecraft.player.sendSystemMessage(Component.literal(line));
        }
    }

    private void onSmartRescanButtonPressed(Button button) {
        if (this.minecraft != null) {
            SmartPresetRecommendation recommendation = this.presetApplier.smartRecommendation(this.minecraft);
            if (this.minecraft.player != null) {
                this.minecraft.player.sendSystemMessage(
                    Component.literal("Re-scanned. Recommended preset: " + recommendation.basePreset().displayName() + ".")
                );
            }
            this.minecraft.setScreen(new PresetSelectionScreen(this.lastScreen, this.coordinator));
        }
    }

    private void toggleAutoApply() {
        if (this.minecraft == null) {
            return;
        }

        String message = this.presetApplier.toggleAutoApply();
        if (this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(Component.literal(message));
        }
        this.minecraft.setScreen(new PresetSelectionScreen(this.lastScreen, this.coordinator));
    }

    private void toggleAggressiveMode() {
        if (this.minecraft == null) {
            return;
        }

        String message = this.presetApplier.toggleAggressiveMode();
        if (this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(Component.literal(message));
        }
        this.minecraft.setScreen(new PresetSelectionScreen(this.lastScreen, this.coordinator));
    }

    private void runMenuAction(String failureMessage, Supplier<String> action) {
        if (this.minecraft == null) {
            return;
        }

        net.minecraft.client.Minecraft client = this.minecraft;
        this.onClose();
        this.coordinator.schedulePresetApply(client, failureMessage, action);
    }
}
