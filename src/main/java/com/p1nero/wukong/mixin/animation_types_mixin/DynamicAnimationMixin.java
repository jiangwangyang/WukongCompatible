package com.p1nero.wukong.mixin.animation_types_mixin;


import com.p1nero.wukong.client.particle.WuKongEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = DynamicAnimation.class,remap = false)
public class DynamicAnimationMixin {
    @Inject(method = "getPlaySpeed",at = @At("HEAD"), cancellable = true)
    public void getPlaySpeed(LivingEntityPatch<?> entitypatch, DynamicAnimation animation, CallbackInfoReturnable<Float> cir) {
        if (entitypatch.getOriginal().hasEffect(MobEffects.GLOWING)) cir.setReturnValue(0.05f);
        if (entitypatch.getOriginal().hasEffect(WuKongEffect.DING.get())) cir.setReturnValue(0.0f);
    }
}
