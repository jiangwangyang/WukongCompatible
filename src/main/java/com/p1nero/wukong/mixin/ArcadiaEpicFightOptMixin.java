package com.p1nero.wukong.mixin;

import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.entity.eventlistener.DodgeSuccessEvent;

@Pseudo
@Mixin(targets = "com.arcadia.epicfightopt.ArcadiaEpicFightOpt", remap = false)
public abstract class ArcadiaEpicFightOptMixin {
    @Inject(
            method = "lambda$registerSkillRewardListeners$3(Lyesman/epicfight/world/entity/eventlistener/DodgeSuccessEvent;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void wukong$suppressDuplicatePerfectDodgeSound(DodgeSuccessEvent event, CallbackInfo ci) {
        if (WukongWeaponCategories.isWeaponValid(event.getPlayerPatch())) {
            ci.cancel();
        }
    }
}
