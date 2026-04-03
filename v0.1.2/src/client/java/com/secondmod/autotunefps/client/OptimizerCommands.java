package com.secondmod.autotunefps.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.secondmod.autotunefps.AutoTuneFps;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.function.Supplier;

public final class OptimizerCommands {
    private static PresetApplier pendingMenuPresetApplier;
    private static int pendingMenuDelayTicks = -1;
    private static final ArrayDeque<Runnable> pendingClientActions = new ArrayDeque<>();
    private static int pendingClientActionDelayTicks = -1;

    private OptimizerCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, PresetApplier presetApplier) {
        dispatcher.register(
            ClientCommandManager.literal("optimizer")
                .executes(context -> showHelp(context.getSource()))
                .then(ClientCommandManager.literal("help")
                    .executes(context -> showHelp(context.getSource())))
                .then(ClientCommandManager.literal("status")
                    .executes(context -> feedbackLines(context.getSource(), presetApplier.describeStatusLines(context.getSource().getClient()))))
                .then(ClientCommandManager.literal("menu")
                    .executes(context -> openMenu(context.getSource(), presetApplier)))
                .then(ClientCommandManager.literal("recommend")
                    .executes(context -> {
                        Minecraft client = context.getSource().getClient();
                        return applyWithDeferredFeedback(
                            context.getSource(),
                            "Failed to apply the recommended preset. AutoTune left your current settings unchanged.",
                            () -> presetApplier.applyRecommendedAndRemember(client)
                        );
                    }))
                .then(ClientCommandManager.literal("autoapply")
                    .then(ClientCommandManager.literal("on")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApply(true))))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApply(false)))))
                .then(ClientCommandManager.literal("preset")
                    .then(buildPresetLiteral("ultimate_performance", PresetProfile.ULTIMATE_PERFORMANCE, presetApplier))
                    .then(buildPresetLiteral("performance", PresetProfile.PERFORMANCE, presetApplier))
                    .then(buildPresetLiteral("balanced", PresetProfile.BALANCED, presetApplier))
                    .then(buildPresetLiteral("quality", PresetProfile.QUALITY, presetApplier))
                    .then(buildPresetLiteral("restore", PresetProfile.OFF, presetApplier)))
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> buildPresetLiteral(
        String literal,
        PresetProfile preset,
        PresetApplier presetApplier
    ) {
        return ClientCommandManager.literal(literal)
            .executes(context -> {
                Minecraft client = context.getSource().getClient();
                return applyWithDeferredFeedback(
                    context.getSource(),
                    "Failed to apply the " + preset.displayName() + " preset. AutoTune left your current settings unchanged.",
                    () -> presetApplier.applyPresetAndRemember(client, preset)
                );
            });
    }

    private static int openMenu(FabricClientCommandSource source, PresetApplier presetApplier) {
        Minecraft client = source.getClient();
        if (client == null) {
            return feedback(source, "Failed to open optimizer menu");
        }

        feedback(source, "Opening optimizer menu...");
        pendingMenuPresetApplier = presetApplier;
        pendingMenuDelayTicks = 1;
        return Command.SINGLE_SUCCESS;
    }

    public static void runPendingClientWork(Minecraft client) {
        if (pendingMenuPresetApplier == null) {
            runPendingClientActions();
            return;
        }

        if (pendingMenuDelayTicks > 0) {
            pendingMenuDelayTicks--;
            return;
        }

        PresetApplier presetApplier = pendingMenuPresetApplier;
        pendingMenuPresetApplier = null;
        pendingMenuDelayTicks = -1;

        try {
            client.setScreen(new PresetSelectionScreen(client.screen, presetApplier));
            if (client.player != null) {
                client.player.displayClientMessage(Component.literal("Optimizer menu opened."), false);
            }
        } catch (RuntimeException exception) {
            AutoTuneFps.LOGGER.warn("Failed to open optimizer menu", exception);
            if (client.player != null) {
                client.player.displayClientMessage(Component.literal("Failed to open optimizer menu"), false);
            }
        }

        runPendingClientActions();
    }

    private static int showHelp(FabricClientCommandSource source) {
        return feedbackLines(
            source,
            "AutoTune FPS",
            "- /optimizer help -> shows all optimizer commands",
            "- /optimizer menu -> open the preset menu",
            "- /optimizer recommend -> applies the recommended preset",
            "- /optimizer preset <preset> -> applies a preset manually",
            "- /optimizer autoapply on/off -> toggle auto-apply when joining a world",
            "- /optimizer status -> show preset and safety info",
            "- Restore brings back the settings AutoTune first saved for you",
            "- recommend never picks ultimate_performance"
        );
    }

    private static int feedback(FabricClientCommandSource source, String message) {
        if (source.getClient() != null && source.getClient().player != null) {
            source.sendFeedback(Component.literal(message));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int feedbackLines(FabricClientCommandSource source, String... messages) {
        for (String message : messages) {
            feedback(source, message);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int applyWithDeferredFeedback(FabricClientCommandSource source, String failureMessage, Supplier<String> action) {
        Minecraft client = source.getClient();
        if (client == null) {
            return feedback(source, failureMessage);
        }

        schedulePresetApply(client, failureMessage, action);
        return Command.SINGLE_SUCCESS;
    }

    static void scheduleClientAction(Runnable action) {
        pendingClientActions.add(action);
        if (pendingClientActionDelayTicks < 0) {
            pendingClientActionDelayTicks = 1;
        }
    }

    private static void displayClientMessage(Minecraft client, String message) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }

    static void schedulePresetApply(Minecraft client, String failureMessage, Supplier<String> action) {
        scheduleClientAction(() -> {
            try {
                displayClientMessage(client, action.get());
            } catch (RuntimeException exception) {
                AutoTuneFps.LOGGER.warn(failureMessage, exception);
                displayClientMessage(client, failureMessage);
            }
        });
    }

    private static void runPendingClientActions() {
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
}
