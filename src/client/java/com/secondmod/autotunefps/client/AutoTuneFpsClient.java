package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class AutoTuneFpsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutoTuneConfig config = AutoTuneConfig.load();
        PresetApplier presetApplier = new PresetApplier(config);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            OptimizerCommands.register(dispatcher, presetApplier)
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String message = presetApplier.applyConfiguredPresetOnJoin(client);
            if (message != null) {
                AutoTuneFps.LOGGER.info(message);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(OptimizerCommands::openPendingMenu);

        AutoTuneFps.LOGGER.info("{} initialized", AutoTuneFps.MOD_NAME);
    }
}
