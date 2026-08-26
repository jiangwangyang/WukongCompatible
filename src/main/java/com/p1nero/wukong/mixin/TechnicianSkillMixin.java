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

@Mixin(value = TechnicianSkill.class, remap = false)
public abstract class TechnicianSkillMixin {
    private static final UUID TECHNICIAN_EVENT_UUID =
            UUID.fromString("99e5c782-fdaf-11eb-9a03-0242ac130003");

    @Inject(
            method = "onInitiate",
            at = @At("TAIL")
    )
    private void wukong$replaceTechnicianAfterimageListener(SkillContainer container, CallbackInfo ci) {
        PlayerEventListener listener = container.getExecutor().getEventListener();
        listener.removeListener(PlayerEventListener.EventType.ANIMATION_BEGIN_EVENT, TECHNICIAN_EVENT_UUID);
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
                            new SPEntityPairingPacket(player.getId(), EntityPairingPacketTypes.TECHNICIAN_ACTIVATED),
                            player
                    );
                }
        );
    }
}
