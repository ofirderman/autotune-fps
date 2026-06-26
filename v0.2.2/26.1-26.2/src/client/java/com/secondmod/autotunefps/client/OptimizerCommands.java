package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public final class OptimizerCommands {
    private static final boolean BOTCHECK_COMMAND_LOG = Boolean.getBoolean("autotune.botcheck.commandLog");

    private OptimizerCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, AutoTuneClientCoordinator coordinator) {
        PresetApplier presetApplier = coordinator.presetApplier();
        OptimizationEngine optimizationEngine = coordinator.optimizationEngine();

        dispatcher.register(
            ClientCommands.literal("optimizer")
                .executes(context -> showHelp(context.getSource()))
                .then(ClientCommands.literal("help")
                    .executes(context -> showHelp(context.getSource())))
                .then(ClientCommands.literal("status")
                    .executes(context -> feedbackLines(
                        context.getSource(),
                        coordinator.describeStatusLines(context.getSource().getClient())
                    )))
                .then(ClientCommands.literal("menu")
                    .executes(context -> openMenu(context.getSource(), coordinator)))
                .then(ClientCommands.literal("presets")
                    .executes(context -> feedbackLines(
                        context.getSource(),
                        presetApplier.describePresetGuideLines(context.getSource().getClient())
                    )))
                .then(ClientCommands.literal("recommend")
                    .executes(context -> {
                        Minecraft client = context.getSource().getClient();
                        return applyWithDeferredFeedback(
                            context.getSource(),
                            coordinator,
                            "Failed to apply the recommended preset. AutoTune left your current settings unchanged.",
                            () -> presetApplier.applyRecommendedAndRemember(client)
                        );
                    }))
                .then(ClientCommands.literal("autoapply")
                    .then(ClientCommands.literal("on")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApply(true))))
                    .then(ClientCommands.literal("off")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApply(false))))
                    .then(ClientCommands.literal("selected")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApplyMode(AutoApplyMode.SELECTED_PRESET))))
                    .then(ClientCommands.literal("recommended")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApplyMode(AutoApplyMode.SMART_RECOMMENDATION))))
                    .then(ClientCommands.literal("smart")
                        .executes(context -> feedback(context.getSource(), presetApplier.setAutoApplyMode(AutoApplyMode.SMART_RECOMMENDATION)))))
                .then(ClientCommands.literal("aggressive")
                    .then(ClientCommands.literal("on")
                        .executes(context -> feedback(
                            context.getSource(),
                            optimizationEngine.setMode(OptimizationEngineMode.AGGRESSIVE)
                        )))
                    .then(ClientCommands.literal("off")
                        .executes(context -> feedback(
                            context.getSource(),
                            optimizationEngine.setMode(OptimizationEngineMode.OFF)
                        ))))
                .then(ClientCommands.literal("engine")
                    .then(ClientCommands.literal("off")
                        .executes(context -> feedback(context.getSource(), optimizationEngine.setMode(OptimizationEngineMode.OFF))))
                    .then(ClientCommands.literal("optimized")
                        .executes(context -> feedback(context.getSource(), optimizationEngine.setMode(OptimizationEngineMode.OPTIMIZED))))
                    .then(ClientCommands.literal("aggressive")
                        .executes(context -> feedback(context.getSource(), optimizationEngine.setMode(OptimizationEngineMode.AGGRESSIVE)))))
                .then(ClientCommands.literal("module")
                    .then(ClientCommands.literal("particle_reduction")
                        .then(ClientCommands.literal("on")
                            .executes(context -> feedback(
                                context.getSource(),
                                optimizationEngine.setParticleReductionEnabled(true)
                            )))
                        .then(ClientCommands.literal("off")
                            .executes(context -> feedback(
                                context.getSource(),
                                optimizationEngine.setParticleReductionEnabled(false)
                            )))))
                .then(ClientCommands.literal("preview")
                    .then(ClientCommands.literal("recommended")
                        .executes(context -> feedbackLines(
                            context.getSource(),
                            presetApplier.previewRecommendedPresetLines(context.getSource().getClient())
                        )))
                    .then(buildPreviewLiteral("ultimate_performance", PresetProfile.ULTIMATE_PERFORMANCE, coordinator))
                    .then(buildPreviewLiteral("performance", PresetProfile.PERFORMANCE, coordinator))
                    .then(buildPreviewLiteral("balanced", PresetProfile.BALANCED, coordinator))
                    .then(buildPreviewLiteral("quality", PresetProfile.QUALITY, coordinator))
                    .then(buildPreviewLiteral("restore", PresetProfile.OFF, coordinator)))
                .then(ClientCommands.literal("preset")
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
        return ClientCommands.literal(literal)
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
        return ClientCommands.literal(literal)
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
            "- /optimizer engine off/optimized/aggressive -> choose runtime optimization",
            "- /optimizer module particle_reduction on/off -> particle module kill switch",
            "- /optimizer aggressive on/off -> legacy alias for Aggressive/Off engine mode",
            "- /optimizer status -> show mode, recommendation, Restore, and auto-apply",
            "- Restore your original settings from before AutoTune FPS changed them",
            "- Restore reuses the same original snapshot after use",
            "- Restore is only available after AutoTune has saved settings for you",
            "- Recommended preset never picks Ultimate Performance"
        );
    }

    private static int feedback(FabricClientCommandSource source, String message) {
        if (BOTCHECK_COMMAND_LOG) {
            AutoTuneFps.LOGGER.info("[BOTCHECK_CHAT] {}", message);
        }
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
