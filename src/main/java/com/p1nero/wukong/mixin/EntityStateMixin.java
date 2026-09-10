package com.p1nero.wukong.mixin;

import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import net.minecraft.world.damagesource.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

// 棍子能对倒地的敌人继续造成伤害
@Mixin(value = EntityState.class, remap = false)
public abstract class EntityStateMixin {
    @Shadow
    // 映射原 EntityState 的 knockDown 方法, 判断实体是否处于击倒状态
    public abstract boolean knockDown();

    // 拦截 attackResult: 若伤害来源使用棍类武器 (WK_STAFF) 且目标处于击倒状态, 则攻击判定结果为成功
    @Inject(method = "attackResult", at = @At("HEAD"), cancellable = true)
    private void inject(
            DamageSource damagesource, CallbackInfoReturnable<AttackResult.ResultType> cir) {
        if (damagesource instanceof EpicFightDamageSource epicFightDamageSource) {
            epicFightDamageSource
                    .getUsedItem()
                    .getCapability(EpicFightCapabilities.CAPABILITY_ITEM)
                    .ifPresent(
                            capabilityItem -> {
                                if (capabilityItem
                                                .getWeaponCategory()
                                                .equals(WukongWeaponCategories.WK_STAFF)
                                        && this.knockDown()) {
                                    cir.setReturnValue(AttackResult.ResultType.SUCCESS);
                                }
                            });
        }
    }
}
