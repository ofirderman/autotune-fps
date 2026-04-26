package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import net.fabricmc.api.ClientModInitializer;

public final class AutoTuneFpsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutoTuneConfig config = AutoTuneConfig.load();
        AutoTuneRuntimeState runtimeState = AutoTuneRuntimeState.load();
        String recoverySummary = runtimeState.recoverIfInterrupted(config);
        PresetApplier presetApplier = new PresetApplier(config, runtimeState);

        ClientCommandCompat.register(presetApplier);
        ClientLifecycleCompat.registerJoin(presetApplier);
        ClientLifecycleCompat.registerEndClientTick();

        if (recoverySummary != null) {
            AutoTuneFps.LOGGER.warn("AutoTune entered recovery mode after an interrupted preset apply ({})", recoverySummary);
        }
        AutoTuneFps.LOGGER.info("{} initialized", AutoTuneFps.MOD_NAME);
    }
}
