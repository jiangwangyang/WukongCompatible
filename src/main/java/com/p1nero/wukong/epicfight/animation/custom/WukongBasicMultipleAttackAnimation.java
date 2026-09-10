package com.p1nero.wukong.epicfight.animation.custom;

import net.minecraft.world.InteractionHand;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;

import javax.annotation.Nullable;

// 悟空专用的多段基础攻击动画: 在兼容封装基础上不附加额外逻辑, 仅提供悟空专用的构造入口
public class WukongBasicMultipleAttackAnimation extends BasicMultipleAttackAnimation {
    // 构造悟空多段基础攻击, 不单独指定 preDelay 时取前摇时长
    public WukongBasicMultipleAttackAnimation(
            float convertTime,
            float antic,
            float contact,
            float recovery,
            @Nullable Collider collider,
            Joint colliderJoint,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        this(
                convertTime,
                antic,
                antic,
                contact,
                recovery,
                collider,
                colliderJoint,
                accessor,
                armature);
    }

    // 构造悟空多段基础攻击, 显式指定前摇(antic)与接触前延迟(preDelay)
    public WukongBasicMultipleAttackAnimation(
            float convertTime,
            float antic,
            float preDelay,
            float contact,
            float recovery,
            @Nullable Collider collider,
            Joint colliderJoint,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        this(
                convertTime,
                accessor,
                armature,
                new AttackAnimation.Phase(
                        0.0F,
                        antic,
                        preDelay,
                        contact,
                        recovery,
                        Float.MAX_VALUE,
                        colliderJoint,
                        collider));
    }

    // 构造悟空多段基础攻击, 指定主手(hand)与目标关节(colliderJoint)
    public WukongBasicMultipleAttackAnimation(
            float convertTime,
            float antic,
            float contact,
            float recovery,
            InteractionHand hand,
            @Nullable Collider collider,
            Joint colliderJoint,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        this(
                convertTime,
                accessor,
                armature,
                new AttackAnimation.Phase(
                        0.0F,
                        antic,
                        antic,
                        contact,
                        recovery,
                        Float.MAX_VALUE,
                        hand,
                        colliderJoint,
                        collider));
    }

    // 基于已构建的阶段(Phase)构造悟空多段攻击, coordSetter 参数预留未使用
    public WukongBasicMultipleAttackAnimation(
            float convertTime,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature,
            boolean coordSetter,
            AttackAnimation.Phase... phases) {
        this(convertTime, accessor, armature, phases);
    }

    // 悟空多段攻击核心构造: 直接调用父类完成构建
    public WukongBasicMultipleAttackAnimation(
            float convertTime,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature,
            AttackAnimation.Phase... phases) {
        super(convertTime, accessor, armature, phases);
    }
}
