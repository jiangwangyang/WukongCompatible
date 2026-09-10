package com.p1nero.wukong.epicfight.animation.custom;

import org.jetbrains.annotations.Nullable;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.BasicAttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

// 蓄力攻击动画: 在造成伤害的时间节点给物品写入 nbt 标签, 方便做棍子的人操作缩放; 按棍势加伤尚未实现
public class WukongChargedAttackAnimation extends BasicAttackAnimation {
    // 构造蓄力攻击: 播放速度固定 1.5 倍, 期间锁定基础攻击与技能, 并在前摇/接触时刻写/清 playing_wk_charged 标记
    public WukongChargedAttackAnimation(
            float convertTime,
            float antic,
            float contact,
            float recovery,
            @Nullable Collider collider,
            Joint colliderJoint,
            AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        super(convertTime, antic, contact, recovery, collider, colliderJoint, accessor, armature);
        this.addProperty(
                        AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER,
                        ((dynamicAnimation, livingEntityPatch, v, v1, v2) -> 1.5F))
                .addStateRemoveOld(EntityState.CAN_BASIC_ATTACK, false)
                .addStateRemoveOld(EntityState.CAN_SKILL_EXECUTION, false);
        this.addEvents(
                AnimationEvent.InTimeEvent.create(
                        antic,
                        ((livingEntityPatch, staticAnimation, objects) -> {
                            livingEntityPatch
                                    .getOriginal()
                                    .getMainHandItem()
                                    .getOrCreateTag()
                                    .putBoolean("playing_wk_charged", true);
                        }),
                        AnimationEvent.Side.SERVER),
                AnimationEvent.InTimeEvent.create(
                        contact,
                        ((livingEntityPatch, staticAnimation, objects) -> {
                            livingEntityPatch
                                    .getOriginal()
                                    .getMainHandItem()
                                    .getOrCreateTag()
                                    .putBoolean("playing_wk_charged", false);
                        }),
                        AnimationEvent.Side.SERVER));
    }

    // 设置本次攻击的冲击力(impact)修正值
    public WukongChargedAttackAnimation setImpact(float impact) {
        this.addProperty(
                AnimationProperty.AttackPhaseProperty.IMPACT_MODIFIER,
                ValueModifier.setter(impact));
        return this;
    }

    // TODO
    @Override
    protected void hurtCollidingEntities(
            LivingEntityPatch<?> entitypatch,
            float prevElapsedTime,
            float elapsedTime,
            EntityState prevState,
            EntityState state,
            Phase phase) {
        super.hurtCollidingEntities(
                entitypatch, prevElapsedTime, elapsedTime, prevState, state, phase);
    }
}
