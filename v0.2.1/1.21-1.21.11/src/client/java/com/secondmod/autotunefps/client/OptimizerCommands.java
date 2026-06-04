package com.secondmod.autotunefps.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public final class OptimizerCommands {
    private OptimizerCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, AutoTuneClientCoordinator coordinator) {
        PresetApplier presetApplier = coordinator.presetApplier();

        dispatcher.register(
            ClientCommandManager.literal("optimizer")
                .executes(context -> showHelp(context.getSource()))
                .then(ClientCommandManager.literal("help")
                    .executes(context -> showHelp(context.getSource())))
                .then(ClientCommandManager.literal("status")
                    .executes(context -> feedbackLines(
                        context.getSource(),
                        presetApplier.describeStatusLines(context.getSource().getClient())
                    )))
                .then(ClientCommandManager.literal("menu")
                    .executes(context -> openMenu(context.getSource(), coordinator)))
                .then(ClientCommandManager.literal("presets")
                    .executes(context -> feedbackLines(
                        context.getSource(),
                        presetApplier.describePresetGuideLines(context.getSource().getClient())
                    )))
                .then(ClientCommandManager.literal("recommend")
                    .executes(context -> {
                        Minecraft client = context.getSource().getClient();
                        return applyWithDeferredFeedback(
                            context.getSource(),
                            coordinator,
                            "Failed to apply the recommended preset. AutoTune left your current settings unchanged.",
                            () -> presetApplier.applyRecommendedAndRemember(client)
                        );
                    }))
                .then(ClientCommandManager.literal("autoapply")
                    .then(ClientCommandManager.literal("on")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApply(true))))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApply(false))))
                    .then(ClientCommandManager.literal("selected")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApplyMode(AutoApplyMode.SELECTED_PRESET))))
                    .then(ClientCommandManager.literal("recommended")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApplyMode(AutoApplyMode.SMART_RECOMMENDATION))))
                    .then(ClientCommandManager.literal("smart")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApplyMode(AutoApplyMode.SMART_RECOMMENDATION)))))
                .then(ClientCommandManager.literal("aggressive")
                    .then(ClientCommandManager.literal("on")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAggressiveMode(true))))
                    .then(ClientCommandManager.literal("off")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAggressiveMode(false)))))
                .then(ClientCommandManager.literal("preview")
                    .then(ClientCommandManager.literal("recommended")
                        .executes(context -> feedbackLines(
                            context.getSource(),
                            presetApplier.previewRecommendedPresetLines(context.getSource().getClient())
                        )))
                    .then(buildPreviewLiteral("ultimate_performance", PresetProfile.ULTIMATE_PERFORMANCE, coordinator))
                    .then(buildPreviewLiteral("performance", PresetProfile.PERFORMANCE, coordinator))
                    .then(buildPreviewLiteral("balanced", PresetProfile.BALANCED, coordinator))
                    .then(buildPreviewLiteral("quality", PresetProfile.QUALITY, coordinator))
                    .then(buildPreviewLiteral("restore", PresetProfile.OFF, coordinator)))
                .then(ClientCommandManager.literal("preset")
                    .then(buildPresetLiteral("ultimate_performance", PresetProfile.ULTIMATE_PERFORMANCE, coordinator))
                    .then(buildPresetLiteral("performance", PresetProfile.PERFORMANCE, coordinator))
                    .then(buildPresetLiteral("balanced", PresetProfile.BALANCED, coordinator))
                    .then(buildPresetLiteral("quality", PresetProfile.QUALITY, coordinator))
                    .then(buildPresetLiteral("restore", PresetProfile.OFF, coordinator)))
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> buildPresetLiteral(
        String literal,
        PresetProfile preset,
        AutoTuneClientCoordinator coordinator
    ) {
        return ClientCommandManager.literal(literal)
            .executes(context -> {
                Minecraft client = context.getSource().getClient();
                return applyWithDeferredFeedback(
                    context.getSource(),
                    coordinator,
                    "Failed to apply the " + preset.displayName() + " preset. AutoTune left your current settings unchanged.",
                    () -> coordinator.presetApplier().applyPresetAndRemember(client, preset)
                );
            });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> buildPreviewLiteral(
        String literal,
        PresetProfile preset,
        AutoTuneClientCoordinator coordinator
    ) {
        return ClientCommandManager.literal(literal)
            .executes(context -> feedbackLines(
                context.getSource(),
                coordinator.presetApplier().previewPresetLines(context.getSource().getClient(), preset)
            ));
    }

    private static int openMenu(FabricClientCommandSource source, AutoTuneClientCoordinator coordinator) {
        Minecraft client = source.getClient();
        if (client == null) {
            return feedback(source, "Failed to open optimizer menu");
        }

        feedback(source, "Opening optimizer menu...");
        coordinator.queueMenuOpen();
        return Command.SINGLE_SUCCESS;
    }

    private static int showHelp(FabricClientCommandSource source) {
        return feedbackLines(
            source,
            "AutoTune FPS",
            "- /optimizer help -> shows all optimizer commands",
            "- /optimizer menu -> open the preset menu",
            "- /optimizer presets -> show preset summaries and Restore state",
            "- /optimizer recommend -> apply the recommended preset",
            "- /optimizer preview recommended -> preview the recommended preset",
            "- /optimizer preview <preset> -> preview setting changes",
            "- /optimizer preset <preset> -> applies a preset manually",
            "- /optimizer autoapply off/selected/recommended -> choose join-time auto-apply",
            "- /optimizer aggressive on/off -> toggle extra tuning for more FPS",
            "- /optimizer status -> show mode, recommendation, Restore, and auto-apply",
            "- Restore your original settings from before AutoTune FPS changed them",
            "- Restore reuses the same original snapshot after use",
            "- Restore is only available after AutoTune has saved settings for you",
            "- Recommended preset never picks Ultimate Performance"
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

    private static int applyWithDeferredFeedback(
        FabricClientCommandSource source,
        AutoTuneClientCoordinator coordinator,
        String failureMessage,
        Supplier<String> action
    ) {
        Minecraft client = source.getClient();
        if (client == null) {
            return feedback(source, failureMessage);
        }

        coordinator.schedulePresetApply(client, failureMessage, action);
        return Command.SINGLE_SUCCESS;
    }

}
