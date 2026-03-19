package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import net.fabricmc.api.ClientModInitializer;

public final class AutoTuneFpsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutoTuneConfig config = AutoTuneConfig.load();
        PresetApplier presetApplier = new PresetApplier(config);

        ClientCommandCompat.register(presetApplier);
        ClientLifecycleCompat.registerJoin(presetApplier);
        ClientLifecycleCompat.registerEndClientTick();

        AutoTuneFps.LOGGER.info("{} initialized", AutoTuneFps.MOD_NAME);
    }
}
