package com.p1nero.wukong.epicfight.animation.custom;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;

// 特殊动作动画: 在动作期间锁定移动/基础攻击/技能执行并固定朝向, 覆盖普通动作动画
public class SpecialActionAnimation extends ActionAnimation {
    // 构造特殊动作动画, 后摇(postDelay)默认取无限大
    public SpecialActionAnimation(
            float convertTime,
            AnimationManager.AnimationAccessor<? extends ActionAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        this(convertTime, Float.MAX_VALUE, accessor, armature);
    }

    // 构造特殊动作动画, 显式指定后摇(postDelay)时长
    public SpecialActionAnimation(
            float convertTime,
            float postDelay,
            AnimationManager.AnimationAccessor<? extends ActionAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        super(convertTime, postDelay, accessor, armature);
        // 配置状态谱: 动作期间不更新行走动画/禁止普攻/禁止技能, 0.01s 后锁定转向, 全程标记为非行动状态
        this.stateSpectrumBlueprint
                .clear()
                .newTimePair(0.0F, postDelay)
                .addState(EntityState.UPDATE_LIVING_MOTION, false)
                .addState(EntityState.CAN_BASIC_ATTACK, false)
                .addState(EntityState.CAN_SKILL_EXECUTION, false)
                .newTimePair(0.01F, postDelay)
                .addState(EntityState.TURNING_LOCKED, true)
                .newTimePair(0.0F, Float.MAX_VALUE)
                .addState(EntityState.INACTION, true);
    }

    // 添加动作动画属性并返回自身, 支持链式调用
    public <V> SpecialActionAnimation addProperty(
            AnimationProperty.ActionAnimationProperty<V> propertyType, V value) {
        this.properties.put(propertyType, value);
        return this;
    }

    // 判断动作期间是否允许移动, 默认始终允许
    protected boolean shouldMove(float currentTime) {
        return true;
    }
}
