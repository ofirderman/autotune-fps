package com.secondmod.autotunefps.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class PresetSelectionScreen extends Screen {
    private static final int CONTENT_TOP = 74;
    private static final int FOOTER_HEIGHT = 46;
    private static final int SCROLL_BOTTOM_PADDING = 8;
    private static final int MAIN_BUTTON_WIDTH = 320;
    private static final int MAIN_BUTTON_HEIGHT = 26;
    private static final int ROW_STEP = 32;
    private static final int SCROLL_STEP = 26;

    private final Screen lastScreen;
    private final AutoTuneClientCoordinator coordinator;
    private final PresetApplier presetApplier;
    private final OptimizationEngine optimizationEngine;
    private final List<GlassButton> fixedButtons = new ArrayList<>();
    private final List<ScrollEntry> scrollEntries = new ArrayList<>();
    private HardwareProfile hardwareProfile;
    private SmartPresetRecommendation smartRecommendation;
    private int scrollOffset;
    private int contentHeight;
    private int optimizationHeadingY = -1;

    public PresetSelectionScreen(Screen lastScreen, AutoTuneClientCoordinator coordinator) {
        super(Component.literal("AutoTune Presets"));
        this.lastScreen = lastScreen;
        this.coordinator = coordinator;
        this.presetApplier = coordinator.presetApplier();
        this.optimizationEngine = coordinator.optimizationEngine();
    }

    @Override
    protected void init() {
        this.fixedButtons.clear();
        this.scrollEntries.clear();
        this.scrollOffset = 0;
        this.optimizationHeadingY = -1;
        if (this.minecraft != null) {
            this.hardwareProfile = this.presetApplier.detectProfile(this.minecraft);
            this.smartRecommendation = this.presetApplier.smartRecommendation(this.minecraft);
        }

        int compactHeight = 24;
        int compactWidth = 92;
        int compactGap = 6;
        int compactX = (this.width - (compactWidth * 3 + compactGap * 2)) / 2;
        int compactY = 45;
        addFixedButton("Apply rec", compactX, compactY, compactWidth, compactHeight, this::onSmartApplyButtonPressed);
        addFixedButton("Preview rec", compactX + compactWidth + compactGap, compactY, compactWidth, compactHeight, this::onSmartPreviewButtonPressed);
        addFixedButton("Re-scan", compactX + (compactWidth + compactGap) * 2, compactY, compactWidth, compactHeight, this::onSmartRescanButtonPressed);

        int x = (this.width - MAIN_BUTTON_WIDTH) / 2;
        int row = 0;
        addPresetButton(PresetProfile.ULTIMATE_PERFORMANCE, x, row++ * ROW_STEP);
        addPresetButton(PresetProfile.PERFORMANCE, x, row++ * ROW_STEP);
        addPresetButton(PresetProfile.BALANCED, x, row++ * ROW_STEP);
        addPresetButton(PresetProfile.QUALITY, x, row++ * ROW_STEP);
        addPresetButton(PresetProfile.OFF, x, row++ * ROW_STEP);
        addScrollButton(
            new GlassButton(x, 0, MAIN_BUTTON_WIDTH, MAIN_BUTTON_HEIGHT, Component.literal(formatAutoApplyButtonLabel()), this::onAutoApplyButtonPressed, false),
            row++ * ROW_STEP
        );

        GlassButton engineButton = new GlassButton(
            x,
            0,
            MAIN_BUTTON_WIDTH,
            MAIN_BUTTON_HEIGHT,
            Component.literal(formatOptimizationEngineButtonLabel()),
            this::onOptimizationEngineButtonPressed,
            false
        );
        engineButton.setTooltip(Tooltip.create(Component.literal(this.optimizationEngine.mode().tooltip())));
        addScrollButton(engineButton, row++ * ROW_STEP);

        if (this.optimizationEngine.mode() != OptimizationEngineMode.OFF) {
            this.optimizationHeadingY = row * ROW_STEP + 4;
            row++;
            GlassButton particleButton = new GlassButton(
                x,
                0,
                MAIN_BUTTON_WIDTH,
                MAIN_BUTTON_HEIGHT,
                Component.literal(formatParticleReductionButtonLabel()),
                this::onParticleReductionButtonPressed,
                this.optimizationEngine.particleReduction().isEnabled()
            );
            particleButton.setActive(this.optimizationEngine.particleReduction().isSupported());
            particleButton.setTooltip(Tooltip.create(Component.literal(this.optimizationEngine.particleReduction().tooltip())));
            addScrollButton(particleButton, row++ * ROW_STEP);
        }
        this.contentHeight = Math.max(0, row * ROW_STEP - (ROW_STEP - MAIN_BUTTON_HEIGHT) + SCROLL_BOTTOM_PADDING);

        int doneY = this.height - MAIN_BUTTON_HEIGHT - 7;
        addFixedGlassButton(new GlassButton(
            x,
            doneY,
            MAIN_BUTTON_WIDTH,
            MAIN_BUTTON_HEIGHT,
            Component.literal("Done"),
            this::onDoneButtonPressed,
            false
        ));
        updateScrollLayout();
    }

    private void addFixedButton(String label, int x, int y, int width, int height, Button.OnPress onPress) {
        addFixedGlassButton(new GlassButton(x, y, width, height, Component.literal(label), onPress, false));
    }

    private void addFixedGlassButton(GlassButton button) {
        this.addWidget(button.widget());
        this.fixedButtons.add(button);
    }

    private void addPresetButton(PresetProfile preset, int x, int contentY) {
        boolean selected = preset != PresetProfile.OFF && isSelectedPreset(preset);
        GlassButton button = new GlassButton(
            x,
            0,
            MAIN_BUTTON_WIDTH,
            MAIN_BUTTON_HEIGHT,
            Component.literal(formatPresetButtonLabel(preset)),
            ignored -> runMenuAction(
                "Failed to apply the " + preset.displayName() + " preset. AutoTune left your current settings unchanged.",
                () -> this.presetApplier.applyPresetAndRemember(this.minecraft, preset)
            ),
            selected
        );
        if (preset != PresetProfile.OFF) {
            button.setActive(!selected);
        }
        addScrollButton(button, contentY);
    }

    private void addScrollButton(GlassButton button, int contentY) {
        button.setVerticalClip(CONTENT_TOP, contentBottom());
        this.addWidget(button.widget());
        this.scrollEntries.add(new ScrollEntry(button, contentY));
    }

    private int contentBottom() {
        return this.height - FOOTER_HEIGHT;
    }

    private int viewportHeight() {
        return Math.max(1, contentBottom() - CONTENT_TOP);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - viewportHeight());
    }

    private void updateScrollLayout() {
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll()));
        for (ScrollEntry entry : scrollEntries) {
            entry.button().setY(CONTENT_TOP + entry.contentY() - scrollOffset);
            entry.button().setVerticalClip(CONTENT_TOP, contentBottom());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY >= CONTENT_TOP && mouseY < contentBottom() && maxScroll() > 0) {
            int previous = scrollOffset;
            scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset - (int) Math.round(verticalAmount * SCROLL_STEP)));
            updateScrollLayout();
            return previous != scrollOffset;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            MinecraftScreenCompat.setScreen(this.minecraft, this.lastScreen);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xB809090A, 0xD0101012);
        drawFixedChrome(guiGraphics);
        for (GlassButton button : fixedButtons) {
            button.widget().extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            button.extractGlass(guiGraphics);
        }

        guiGraphics.enableScissor(0, CONTENT_TOP, this.width, contentBottom());
        drawOptimizationSettingsHeading(guiGraphics);
        for (ScrollEntry entry : scrollEntries) {
            entry.button().widget().extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            entry.button().extractGlass(guiGraphics);
        }
        guiGraphics.disableScissor();
        drawScrollbar(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawFixedChrome(GuiGraphicsExtractor guiGraphics) {
        int panelLeft = Math.max(8, this.width / 2 - 178);
        int panelRight = Math.min(this.width - 8, this.width / 2 + 178);
        guiGraphics.fill(panelLeft, 7, panelRight, 70, 0x8A111214);
        guiGraphics.fill(panelLeft, 7, panelRight, 8, 0x607A7C80);
        guiGraphics.fill(panelLeft, 69, panelRight, 70, 0x60000000);
        guiGraphics.centeredText(this.font, Component.literal("AutoTune Presets"), this.width / 2, 13, 0xFFFFFFFF);
        guiGraphics.centeredText(
            this.font,
            Component.literal("Recommended preset: " + formatSmartRecommendationLabel()),
            this.width / 2,
            29,
            0xFFD2D2D3
        );
        guiGraphics.fill(0, contentBottom(), this.width, this.height, 0xB80B0C0E);
        guiGraphics.fill(0, contentBottom(), this.width, contentBottom() + 1, 0x60717478);
    }

    private void drawOptimizationSettingsHeading(GuiGraphicsExtractor guiGraphics) {
        if (optimizationHeadingY < 0) {
            return;
        }
        int y = CONTENT_TOP + optimizationHeadingY - scrollOffset;
        if (y < CONTENT_TOP - 10 || y >= contentBottom()) {
            return;
        }
        String heading = this.optimizationEngine.mode() == OptimizationEngineMode.AGGRESSIVE
            ? "Aggressive Optimization Settings"
            : "Optimization Settings";
        guiGraphics.centeredText(this.font, Component.literal(heading), this.width / 2, y, 0xFFE1E1E2);
    }

    private void drawScrollbar(GuiGraphicsExtractor guiGraphics) {
        if (maxScroll() <= 0) {
            return;
        }
        int trackX = this.width / 2 + MAIN_BUTTON_WIDTH / 2 + 8;
        int trackHeight = viewportHeight();
        int thumbHeight = Math.max(24, trackHeight * trackHeight / contentHeight);
        int thumbTravel = trackHeight - thumbHeight;
        int thumbY = CONTENT_TOP + (thumbTravel * scrollOffset / maxScroll());
        guiGraphics.fill(trackX, CONTENT_TOP, trackX + 3, contentBottom(), 0x50242527);
        guiGraphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xB096989B);
        guiGraphics.fill(trackX, thumbY, trackX + 1, thumbY + thumbHeight, 0xD0E2E2E3);
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

    private String formatOptimizationEngineButtonLabel() {
        return "Optimization Engine: " + this.optimizationEngine.mode().displayName();
    }

    private String formatParticleReductionButtonLabel() {
        ParticleReductionModule module = this.optimizationEngine.particleReduction();
        return "Particle Reduction: " + (module.isEnabled() ? "On" : "Off")
            + " (FPS: " + module.fpsImpact().displayName()
            + ", Visual: " + module.visualImpact().displayName() + ")";
    }

    private void onAutoApplyButtonPressed(Button button) {
        toggleAutoApply();
    }

    private void onOptimizationEngineButtonPressed(Button button) {
        updateOptimizationEngineMode();
    }

    private void onParticleReductionButtonPressed(Button button) {
        toggleParticleReduction();
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
            MinecraftScreenCompat.setScreen(this.minecraft, new PresetSelectionScreen(this.lastScreen, this.coordinator));
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
        MinecraftScreenCompat.setScreen(this.minecraft, new PresetSelectionScreen(this.lastScreen, this.coordinator));
    }

    private void updateOptimizationEngineMode() {
        if (this.minecraft == null) {
            return;
        }
        String message = this.optimizationEngine.cycleMode();
        if (this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(Component.literal(message));
        }
        MinecraftScreenCompat.setScreen(this.minecraft, new PresetSelectionScreen(this.lastScreen, this.coordinator));
    }

    private void toggleParticleReduction() {
        if (this.minecraft == null) {
            return;
        }
        String message = this.optimizationEngine.toggleParticleReduction();
        if (this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(Component.literal(message));
        }
        MinecraftScreenCompat.setScreen(this.minecraft, new PresetSelectionScreen(this.lastScreen, this.coordinator));
    }

    private void runMenuAction(String failureMessage, Supplier<String> action) {
        if (this.minecraft == null) {
            return;
        }
        net.minecraft.client.Minecraft client = this.minecraft;
        this.onClose();
        this.coordinator.schedulePresetApply(client, failureMessage, action);
    }

    private record ScrollEntry(GlassButton button, int contentY) {
    }
}
