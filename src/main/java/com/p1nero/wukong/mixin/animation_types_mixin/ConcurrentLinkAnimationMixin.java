package com.p1nero.wukong.mixin.animation_types_mixin;

import com.p1nero.wukong.client.particle.WuKongEffect;

import net.minecraft.world.effect.MobEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

// Mixin 注入到 EpicFight 的 AttackAnimation (并发链接动画), 覆盖播放速度以处理发光与定身效果
@Mixin(value = yesman.epicfight.api.animation.types.AttackAnimation.class, remap = false)
public class ConcurrentLinkAnimationMixin {
    // 在 getPlaySpeed 头部拦截: 发光效果将播放速度降到 0.05, 定身效果 (DING) 将播放速度设为 0
    @Inject(method = "getPlaySpeed", at = @At("HEAD"), cancellable = true)
    public void getPlaySpeed(
            LivingEntityPatch<?> entitypatch,
            DynamicAnimation animation,
            CallbackInfoReturnable<Float> cir) {
        if (entitypatch.getOriginal().hasEffect(MobEffects.GLOWING)) cir.setReturnValue(0.05f);
        if (entitypatch.getOriginal().hasEffect(WuKongEffect.DING.get())) cir.setReturnValue(0.0f);
    }
}
