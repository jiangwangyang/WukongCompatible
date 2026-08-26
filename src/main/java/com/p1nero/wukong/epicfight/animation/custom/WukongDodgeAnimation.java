package com.p1nero.wukong.epicfight.animation.custom;

import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * 鏃犳晫鏃堕棿缂╃煭鍒板悗鎽囩粨鏉? */
public class WukongDodgeAnimation extends DodgeAnimation {

    public WukongDodgeAnimation(float convertTime, float delayTime, AnimationManager.AnimationAccessor<? extends DodgeAnimation> accessor, float width, float height, AssetAccessor<? extends Armature> armature, boolean isPerfect) {
        super(convertTime, delayTime, accessor, width, height, armature);
        this.addEvents(AnimationEvent.InTimeEvent.create(delayTime, ((livingEntityPatch, staticAnimation, objects) -> {
            if (livingEntityPatch instanceof ServerPlayerPatch serverPlayerPatch && WukongWeaponCategories.isWeaponValid(livingEntityPatch)) {
                serverPlayerPatch.getOriginal().getCapability(WKCapabilityProvider.WK_PLAYER).ifPresent(wkPlayer -> {
                    SkillContainer weaponInnate = serverPlayerPatch.getSkill(SkillSlots.WEAPON_INNATE);
                    if (weaponInnate == null || weaponInnate.isEmpty()) {
                        return;
                    }
                    var dataManager = weaponInnate.getDataManager();
                    boolean isCommonCharging = dataManager.hasData(WukongSkillDataKeys.IS_CHARGING.get())
                            && dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get());
                    boolean isThrustCharging = dataManager.hasData(WukongSkillDataKeys.Thrust_IS_CHARGING.get())
                            && dataManager.getDataValue(WukongSkillDataKeys.Thrust_IS_CHARGING.get());
                    if ((isCommonCharging || isThrustCharging) && wkPlayer.isPerfectDodge() && !isPerfect) {
                        weaponInnate.getSkill().setConsumptionSynchronize(weaponInnate, 1);
                        weaponInnate.getSkill().setStackSynchronize(weaponInnate, 0);
                    }
                    if (dataManager.hasData(WukongSkillDataKeys.IS_CHARGING.get())) {
                        dataManager.setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false, serverPlayerPatch.getOriginal());
                    }
                    if (dataManager.hasData(WukongSkillDataKeys.Thrust_IS_CHARGING.get())) {
                        dataManager.setDataSync(WukongSkillDataKeys.Thrust_IS_CHARGING.get(), false, serverPlayerPatch.getOriginal());
                    }
                });
            }
        }), AnimationEvent.Side.SERVER));



        this.stateSpectrumBlueprint.clear()
                .newTimePair(0.0F, delayTime)
                .addState(EntityState.TURNING_LOCKED, true)
                .addState(EntityState.MOVEMENT_LOCKED, true)
                .addState(EntityState.UPDATE_LIVING_MOTION, false)
                .addState(EntityState.CAN_BASIC_ATTACK, false)
                .addState(EntityState.CAN_SKILL_EXECUTION, false)
                .addState(EntityState.INACTION, true)
                .newTimePair(0.0F, delayTime)
                .addState(EntityState.ATTACK_RESULT, DODGEABLE_SOURCE_VALIDATOR);
    }

    public WukongDodgeAnimation(float convertTime, float delayTime, AnimationManager.AnimationAccessor<? extends DodgeAnimation> accessor, float width, float height, AssetAccessor<? extends Armature> armature) {
        this(convertTime, delayTime, accessor, width, height, armature, false);
    }

    /**
     * 瑙﹀彂瀹岀編闂伩鎵嶆敼鐘舵€?     */
    @Override
    public void begin(LivingEntityPatch<?> entityPatch) {
        super.begin(entityPatch);
        if(entityPatch instanceof ServerPlayerPatch playerPatch){
            playerPatch.getOriginal().getCapability(WKCapabilityProvider.WK_PLAYER).ifPresent(wkPlayer -> wkPlayer.setPerfectDodge(false));
        }
    }

}
