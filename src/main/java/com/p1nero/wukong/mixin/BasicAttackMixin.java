package com.p1nero.wukong.mixin;

import static yesman.epicfight.skill.BasicAttack.setComboCounterWithEvent;

import com.p1nero.wukong.Config;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent;

// Mixin 注入到 EpicFight 的 BasicAttack, 重写普攻连击计数器的过期逻辑
@Mixin(value = BasicAttack.class, remap = false)
public class BasicAttackMixin {

    // 拦截 updateContainer 并取消原逻辑: 当距离上次行动已超过配置的普攻间隔且连击数大于 0 时, 以 TIME_EXPIRED 原因将连击计数清零
    @Inject(method = "updateContainer", at = @At("HEAD"), cancellable = true)
    private void modifyExpiredTicks(SkillContainer container, CallbackInfo ci) {
        if (!container.getExecutor().isLogicalClient()
                && container.getExecutor().getTickSinceLastAction()
                        > Config.BASIC_ATTACK_INTERVAL_TICKS.get()
                && (Integer)
                                container
                                        .getDataManager()
                                        .getDataValue(
                                                (SkillDataKey) SkillDataKeys.COMBO_COUNTER.get())
                        > 0) {
            setComboCounterWithEvent(
                    ComboCounterHandleEvent.Causal.TIME_EXPIRED,
                    (ServerPlayerPatch) container.getExecutor(),
                    container,
                    null,
                    0);
        }
        ci.cancel();
    }
}
