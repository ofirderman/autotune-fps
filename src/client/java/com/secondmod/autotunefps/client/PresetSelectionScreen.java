package com.secondmod.autotunefps.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        int buttonHeight = 20;
        int x = (this.width - buttonWidth) / 2;
        int y = this.height / 4 + 20;
        int buttonStep = 22;

        this.addRenderableWidget(Button.builder(Component.literal("Recommended"), button -> {
                runMenuAction(
                    "Failed to apply the recommended preset. AutoTune left your current settings unchanged.",
                    () -> this.presetApplier.applyRecommendedAndRemember(this.minecraft)
                );
            })
            .bounds(x, y, buttonWidth, buttonHeight)
            .build());

        addPresetButton(PresetProfile.ULTIMATE_PERFORMANCE, x, y + buttonStep, buttonWidth, buttonHeight);
        addPresetButton(PresetProfile.PERFORMANCE, x, y + buttonStep * 2, buttonWidth, buttonHeight);
        addPresetButton(PresetProfile.BALANCED, x, y + buttonStep * 3, buttonWidth, buttonHeight);
        addPresetButton(PresetProfile.QUALITY, x, y + buttonStep * 4, buttonWidth, buttonHeight);
        addPresetButton(PresetProfile.OFF, x, y + buttonStep * 5, buttonWidth, buttonHeight);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), button ->
                onClose())
            .bounds(x, y + buttonStep * 6, buttonWidth, buttonHeight)
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
        // Avoid the built-in blur background path because newer 1.21.x only allows one blur pass per frame.
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(
            this.font,
            Component.literal("Current: " + this.presetApplier.config().selectedPreset.displayName()),
            this.width / 2,
            40,
            0xA0A0A0
        );
        guiGraphics.drawCenteredString(
            this.font,
            Component.literal(this.presetApplier.config().selectedPreset.summary()),
            this.width / 2,
            52,
            0xA0A0A0
        );
        if (this.hardwareProfile != null) {
            guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Detected: " + this.hardwareProfile.compactSummary()),
                this.width / 2,
                64,
                0xA0A0A0
            );
            guiGraphics.drawCenteredString(
                this.font,
                Component.literal(this.hardwareProfile.fabulousSafe() ? "Quality can use Fabulous on this PC." : "Quality will fall back to Fancy when Fabulous is not safe."),
                this.width / 2,
                76,
                0xA0A0A0
            );
        }
    }

    private void addPresetButton(PresetProfile preset, int x, int y, int width, int height) {
        this.addRenderableWidget(Button.builder(Component.literal(preset.displayName()), button -> {
                runMenuAction(
                    "Failed to apply the " + preset.displayName() + " preset. AutoTune left your current settings unchanged.",
                    () -> this.presetApplier.applyPresetAndRemember(this.minecraft, preset)
                );
            })
            .bounds(x, y, width, height)
            .build());
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
