package com.p1nero.wukong.mixin;

import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPEntityPairingPacket;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.passive.TechnicianSkill;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.UUID;

// Mixin 注入到 EpicFight 的 TechnicianSkill, 替换其残影事件监听, 使棍类武器不再触发残影
@Mixin(value = TechnicianSkill.class, remap = false)
public abstract class TechnicianSkillMixin {
    // 固定的事件监听器 UUID, 用于唯一标识并移除/替换原 TechnicianSkill 的动画开始事件监听
    private static final UUID TECHNICIAN_EVENT_UUID =
            UUID.fromString("99e5c782-fdaf-11eb-9a03-0242ac130003");

    // 在技能初始化 (onInitiate) 尾部替换动画开始事件监听: 仅在非棍类武器且触发的动画为闪避时, 向周围玩家广播残影激活包
    @Inject(method = "onInitiate", at = @At("TAIL"))
    private void wukong$replaceTechnicianAfterimageListener(
            SkillContainer container, CallbackInfo ci) {
        PlayerEventListener listener = container.getExecutor().getEventListener();
        listener.removeListener(
                PlayerEventListener.EventType.ANIMATION_BEGIN_EVENT, TECHNICIAN_EVENT_UUID);
        listener.addEventListener(
                PlayerEventListener.EventType.ANIMATION_BEGIN_EVENT,
                TECHNICIAN_EVENT_UUID,
                event -> {
                    if (container.getExecutor().isLogicalClient()
                            || !(event.getAnimation() instanceof DodgeAnimation)
                            || WukongWeaponCategories.isWeaponValid(container.getExecutor())) {
                        return;
                    }

                    ServerPlayer player = container.getServerExecutor().getOriginal();
                    EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(
                            new SPEntityPairingPacket(
                                    player.getId(), EntityPairingPacketTypes.TECHNICIAN_ACTIVATED),
                            player);
                });
    }
}
