package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public final class AutoTuneClientCoordinator {
    private final PresetApplier presetApplier;
    private final OptimizationEngine optimizationEngine;
    private final ArrayDeque<Runnable> pendingClientActions = new ArrayDeque<>();
    private boolean pendingMenuOpen = false;
    private int pendingMenuDelayTicks = -1;
    private int pendingClientActionDelayTicks = -1;

    public AutoTuneClientCoordinator(PresetApplier presetApplier, OptimizationEngine optimizationEngine) {
        this.presetApplier = presetApplier;
        this.optimizationEngine = optimizationEngine;
    }

    public PresetApplier presetApplier() {
        return presetApplier;
    }

    public OptimizationEngine optimizationEngine() {
        return optimizationEngine;
    }

    public String[] describeStatusLines(Minecraft client) {
        List<String> lines = new ArrayList<>(Arrays.asList(presetApplier.describeStatusLines(client)));
        lines.addAll(Arrays.asList(optimizationEngine.describeStatusLines()));
        return lines.toArray(String[]::new);
    }

    public void queueMenuOpen() {
        pendingMenuOpen = true;
        pendingMenuDelayTicks = 1;
    }

    public void scheduleClientAction(Runnable action) {
        pendingClientActions.add(action);
        if (pendingClientActionDelayTicks < 0) {
            pendingClientActionDelayTicks = 1;
        }
    }

    public void schedulePresetApply(Minecraft client, String failureMessage, Supplier<String> action) {
        scheduleClientAction(() -> {
            try {
                displayClientMessage(client, action.get());
            } catch (RuntimeException exception) {
                AutoTuneFps.LOGGER.warn(failureMessage, exception);
                displayClientMessage(client, failureMessage);
            }
        });
    }

    public void onWorldJoin(Minecraft client) {
        String message = presetApplier.applyConfiguredPresetOnJoin(client);
        if (message != null) {
            displayClientMessage(client, message);
        }
    }

    public void onEndClientTick(Minecraft client) {
        optimizationEngine.onClientTick(client);
        runPendingMenuOpen(client);
        runPendingClientActions();
    }

    private void runPendingMenuOpen(Minecraft client) {
        if (!pendingMenuOpen) {
            return;
        }

        if (pendingMenuDelayTicks > 0) {
            pendingMenuDelayTicks--;
            return;
        }

        pendingMenuOpen = false;
        pendingMenuDelayTicks = -1;

        try {
            client.setScreen(new PresetSelectionScreen(client.screen, this));
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("Optimizer menu opened."));
            }
        } catch (RuntimeException exception) {
            AutoTuneFps.LOGGER.warn("Failed to open optimizer menu", exception);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("Failed to open optimizer menu"));
            }
        }
    }

    private void runPendingClientActions() {
        if (pendingClientActions.isEmpty()) {
            pendingClientActionDelayTicks = -1;
            return;
        }

        if (pendingClientActionDelayTicks > 0) {
            pendingClientActionDelayTicks--;
            return;
        }

        while (!pendingClientActions.isEmpty()) {
            Runnable action = pendingClientActions.poll();
            if (action == null) {
                continue;
            }

            try {
                action.run();
            } catch (RuntimeException exception) {
                AutoTuneFps.LOGGER.warn("Failed to run deferred AutoTune action", exception);
            }
        }
        pendingClientActionDelayTicks = -1;
    }

    private static void displayClientMessage(Minecraft client, String message) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }
}
