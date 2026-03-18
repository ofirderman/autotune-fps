package com.secondmod.autotunefps.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.secondmod.autotunefps.AutoTuneFps;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class OptimizerCommands {
    private static PresetApplier pendingMenuPresetApplier;
    private static int pendingMenuDelayTicks = -1;

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
                    .executes(context -> feedback(context.getSource(), presetApplier.applyRecommendedAndRemember(context.getSource().getClient()))))
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
                    .then(buildPresetLiteral("off", PresetProfile.OFF, presetApplier)))
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> buildPresetLiteral(
        String literal,
        PresetProfile preset,
        PresetApplier presetApplier
    ) {
        return ClientCommandManager.literal(literal)
            .executes(context -> {
                String message = presetApplier.applyPresetAndRemember(context.getSource().getClient(), preset);
                return feedback(context.getSource(), message);
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

    public static void openPendingMenu(Minecraft client) {
        if (pendingMenuPresetApplier == null) {
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
    }

    private static int showHelp(FabricClientCommandSource source) {
        return feedbackLines(
            source,
            "AutoTune FPS Commands",
            "- /optimizer help -> show command help",
            "- /optimizer menu -> open the optimizer menu",
            "- /optimizer recommend -> detect hardware and apply the recommended preset",
            "- /optimizer preset ultimate_performance / performance / balanced / quality / off",
            "- /optimizer status -> show current optimizer state"
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
}
