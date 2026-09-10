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

// 悟空闪避动画: 缩短无敌(可闪避)时间至后摇结束, 并根据是否完美闪避决定是否保留棍势
public class WukongDodgeAnimation extends DodgeAnimation {

    // 构造悟空闪避动画: 在后摇结束(delayTime)处处理蓄力/突刺蓄力清除, 非完美闪避清空棍势, 并配置状态谱
    public WukongDodgeAnimation(
            float convertTime,
            float delayTime,
            AnimationManager.AnimationAccessor<? extends DodgeAnimation> accessor,
            float width,
            float height,
            AssetAccessor<? extends Armature> armature,
            boolean isPerfect) {
        super(convertTime, delayTime, accessor, width, height, armature);
        this.addEvents(
                AnimationEvent.InTimeEvent.create(
                        delayTime,
                        ((livingEntityPatch, staticAnimation, objects) -> {
                            if (livingEntityPatch instanceof ServerPlayerPatch serverPlayerPatch
                                    && WukongWeaponCategories.isWeaponValid(livingEntityPatch)) {
                                serverPlayerPatch
                                        .getOriginal()
                                        .getCapability(WKCapabilityProvider.WK_PLAYER)
                                        .ifPresent(
                                                wkPlayer -> {
                                                    SkillContainer weaponInnate =
                                                            serverPlayerPatch.getSkill(
                                                                    SkillSlots.WEAPON_INNATE);
                                                    if (weaponInnate == null
                                                            || weaponInnate.isEmpty()) {
                                                        return;
                                                    }
                                                    var dataManager = weaponInnate.getDataManager();
                                                    boolean isCommonCharging =
                                                            dataManager.hasData(
                                                                            WukongSkillDataKeys
                                                                                    .IS_CHARGING
                                                                                    .get())
                                                                    && dataManager.getDataValue(
                                                                            WukongSkillDataKeys
                                                                                    .IS_CHARGING
                                                                                    .get());
                                                    boolean isThrustCharging =
                                                            dataManager.hasData(
                                                                            WukongSkillDataKeys
                                                                                    .Thrust_IS_CHARGING
                                                                                    .get())
                                                                    && dataManager.getDataValue(
                                                                            WukongSkillDataKeys
                                                                                    .Thrust_IS_CHARGING
                                                                                    .get());
                                                    // 非完美闪避才清空棍势, 完美闪避保留棍势
                                                    if ((isCommonCharging || isThrustCharging)
                                                            && !wkPlayer.isPerfectDodge()
                                                            && !isPerfect) {
                                                        weaponInnate
                                                                .getSkill()
                                                                .setConsumptionSynchronize(
                                                                        weaponInnate, 1);
                                                        weaponInnate
                                                                .getSkill()
                                                                .setStackSynchronize(
                                                                        weaponInnate, 0);
                                                    }
                                                    if (dataManager.hasData(
                                                            WukongSkillDataKeys.IS_CHARGING
                                                                    .get())) {
                                                        dataManager.setDataSync(
                                                                WukongSkillDataKeys.IS_CHARGING
                                                                        .get(),
                                                                false);
                                                    }
                                                    if (dataManager.hasData(
                                                            WukongSkillDataKeys.Thrust_IS_CHARGING
                                                                    .get())) {
                                                        dataManager.setDataSync(
                                                                WukongSkillDataKeys
                                                                        .Thrust_IS_CHARGING.get(),
                                                                false);
                                                    }
                                                });
                            }
                        }),
                        AnimationEvent.Side.SERVER));

        this.stateSpectrumBlueprint
                .clear()
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

    // 构造悟空闪避动画, 默认按非完美闪避处理
    public WukongDodgeAnimation(
            float convertTime,
            float delayTime,
            AnimationManager.AnimationAccessor<? extends DodgeAnimation> accessor,
            float width,
            float height,
            AssetAccessor<? extends Armature> armature) {
        this(convertTime, delayTime, accessor, width, height, armature, false);
    }

    // 动画开始: 复位完美闪避标记, 只有本次实际触发完美闪避时能力侧才会重新置为 true
    @Override
    public void begin(LivingEntityPatch<?> entityPatch) {
        super.begin(entityPatch);
        if (entityPatch instanceof ServerPlayerPatch playerPatch) {
            playerPatch
                    .getOriginal()
                    .getCapability(WKCapabilityProvider.WK_PLAYER)
                    .ifPresent(wkPlayer -> wkPlayer.setPerfectDodge(false));
        }
    }
}
