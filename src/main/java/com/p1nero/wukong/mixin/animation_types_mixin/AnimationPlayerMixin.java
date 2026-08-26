package com.p1nero.wukong.mixin.animation_types_mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = AnimationPlayer.class, remap = false)
public abstract class AnimationPlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(LivingEntityPatch<?> entitypatch, CallbackInfo ci) {
//
//        if (entitypatch.getOriginal().hasEffect(WuKongEffect.DING.get())) {
//            // ci.cancel();
//        }
//

    }
}
