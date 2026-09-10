package com.p1nero.wukong.epicfight.animation.custom;

import net.minecraft.world.InteractionHand;

import org.jetbrains.annotations.Nullable;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.BasicAttackAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;

// 悟空跳跃攻击动画: 基础攻击动画的兼容封装, 不计入基础攻击连段
public class WukongJumpAttackAnimation extends BasicAttackAnimation {
    // 构造跳跃攻击动画, 不单独指定 preDelay 时 preDelay 取前摇时长
    public WukongJumpAttackAnimation(
            float convertTime,
            float antic,
            float contact,
            float recovery,
            @Nullable Collider collider,
            Joint colliderJoint,
            AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        super(convertTime, antic, contact, recovery, collider, colliderJoint, accessor, armature);
    }

    // 构造跳跃攻击动画, 显式指定前摇(antic)与接触前延迟(preDelay)
    public WukongJumpAttackAnimation(
            float convertTime,
            float antic,
            float preDelay,
            float contact,
            float recovery,
            @Nullable Collider collider,
            Joint colliderJoint,
            AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        super(
                convertTime,
                antic,
                preDelay,
                contact,
                recovery,
                collider,
                colliderJoint,
                accessor,
                armature);
    }

    // 构造跳跃攻击动画, 指定主手(hand)与目标关节(colliderJoint)
    public WukongJumpAttackAnimation(
            float convertTime,
            float antic,
            float contact,
            float recovery,
            InteractionHand hand,
            @Nullable Collider collider,
            Joint colliderJoint,
            AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        super(
                convertTime,
                antic,
                contact,
                recovery,
                hand,
                collider,
                colliderJoint,
                accessor,
                armature);
    }

    // 基于阶段(Phase)构造悟空跳跃攻击
    public WukongJumpAttackAnimation(
            float convertTime,
            AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature,
            AttackAnimation.Phase... phases) {
        super(convertTime, accessor, armature, phases);
    }

    // 标记本动画不属于基础攻击动画(跳跃攻击不计入普攻连段)
    @Override
    public boolean isBasicAttackAnimation() {
        return false;
    }
}
