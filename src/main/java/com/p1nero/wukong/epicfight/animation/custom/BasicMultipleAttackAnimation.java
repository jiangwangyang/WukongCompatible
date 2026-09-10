package com.p1nero.wukong.epicfight.animation.custom;

import net.minecraft.world.InteractionHand;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;

import java.util.Locale;

import javax.annotation.Nullable;

// 多段基础攻击动画的兼容封装类
public class BasicMultipleAttackAnimation extends AttackAnimation {
    // 构造多段基础攻击动画, 不单独指定 preDelay 时 preDelay 取前摇时长
    public BasicMultipleAttackAnimation(
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

    // 构造多段基础攻击动画, 显式指定前摇(antic)与接触前延迟(preDelay)
    public BasicMultipleAttackAnimation(
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

    // 构造多段基础攻击动画, 指定主手(hand)与目标关节(colliderJoint)
    public BasicMultipleAttackAnimation(
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

    // 基于已构建的阶段(Phase)构造多段攻击, coordSetter 参数预留未使用
    public BasicMultipleAttackAnimation(
            float convertTime,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature,
            boolean coordSetter,
            AttackAnimation.Phase... phases) {
        this(convertTime, accessor, armature, phases);
    }

    // 多段攻击核心构造: 调用父类并解除转向锁定, 将动画时间区间设为 0~无限
    public BasicMultipleAttackAnimation(
            float convertTime,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature,
            AttackAnimation.Phase... phases) {
        super(convertTime, accessor, armature, phases);
        this.newTimePair(0.0F, Float.MAX_VALUE);
        this.addStateRemoveOld(EntityState.TURNING_LOCKED, false);
    }

    // 初始化: 若未设置基础攻速属性, 按动画总时长计算并写入 (攻速 = 1 / 总时长, 保留两位小数)
    @Override
    public void postInit() {
        super.postInit();
        if (!this.properties.containsKey(AttackAnimationProperty.BASIS_ATTACK_SPEED)) {
            float basisSpeed =
                    Float.parseFloat(String.format(Locale.US, "%.2f", 1.0F / this.getTotalTime()));
            this.addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, basisSpeed);
        }
    }

    // 标记本动画属于基础攻击动画
    public boolean isBasicAttackAnimation() {
        return true;
    }
}
