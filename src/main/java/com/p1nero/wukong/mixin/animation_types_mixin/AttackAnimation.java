package com.p1nero.wukong.mixin.animation_types_mixin;

import com.p1nero.wukong.client.particle.WuKongEffect;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

// Mixin 注入到 EpicFight 的 AttackAnimation, 用于在播放速度计算时覆盖定身效果
@Mixin(value = yesman.epicfight.api.animation.types.AttackAnimation.class, remap = false)
public class AttackAnimation {
    // 在 getPlaySpeed 头部拦截: 若实体处于定身效果 (DING), 则将播放速度设为 0 使其完全静止
    @Inject(method = "getPlaySpeed", at = @At("HEAD"), cancellable = true)
    public void getPlaySpeed(
            LivingEntityPatch<?> entitypatch,
            DynamicAnimation animation,
            CallbackInfoReturnable<Float> cir) {
        if (entitypatch.getOriginal().hasEffect(WuKongEffect.DING.get())) cir.setReturnValue(0.0f);
    }
}
