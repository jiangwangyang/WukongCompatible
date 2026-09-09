package com.p1nero.wukong.mixin.animation_types_mixin;

import com.p1nero.wukong.client.particle.WuKongEffect;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = yesman.epicfight.api.animation.types.AttackAnimation.class, remap = false)
public class AttackAnimation {
    @Inject(method = "getPlaySpeed", at = @At("HEAD"), cancellable = true)
    public void getPlaySpeed(
            LivingEntityPatch<?> entitypatch,
            DynamicAnimation animation,
            CallbackInfoReturnable<Float> cir) {
        if (entitypatch.getOriginal().hasEffect(WuKongEffect.DING.get())) cir.setReturnValue(0.0f);
    }
}
