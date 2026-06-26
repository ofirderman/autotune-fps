package com.secondmod.autotunefps.client.mixin;

import com.secondmod.autotunefps.client.OptimizationEngineRuntime;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void autotuneFps$markParticleHookAvailable(CallbackInfo callback) {
        OptimizationEngineRuntime.markParticleHookAvailable();
    }

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 1)
    private void autotuneFps$applyParticleBudget(
        ParticleOptions particleOptions,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ,
        CallbackInfoReturnable<Particle> callback
    ) {
        OptimizationEngineRuntime.markParticleHookAvailable();
        if (!OptimizationEngineRuntime.allowParticle()) {
            callback.setReturnValue(null);
        }
    }
}
