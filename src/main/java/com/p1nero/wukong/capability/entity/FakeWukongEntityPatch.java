package com.p1nero.wukong.capability.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.p1nero.wukong.entity.FakeWukongEntity;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;

// 假悟空分身的 EpicFight 实体补丁: 配置持棍分身的战斗 AI 行为(连段/重击/闪避等)
public class FakeWukongEntityPatch extends HumanoidMobPatch<FakeWukongEntity> {

    public static final CombatBehaviors.Builder<HumanoidMobPatch<?>> WK_STAFF =
            CombatBehaviors.<HumanoidMobPatch<?>>builder()
                    .newBehaviorSeries(
                            CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder()
                                    .weight(100.0F)
                                    .canBeInterrupted(false)
                                    .looping(false)
                                    .nextBehavior(
                                            CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                                    .animationBehavior(WukongAnimations.STAFF_AUTO1)
                                                    .withinEyeHeight()
                                                    .withinDistance(0.0D, 2.5D))
                                    .nextBehavior(
                                            CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                                    .animationBehavior(WukongAnimations.STAFF_AUTO2)
                                                    .withinEyeHeight()
                                                    .withinDistance(0.0D, 2.5D))
                                    .nextBehavior(
                                            CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                                    .animationBehavior(WukongAnimations.STAFF_AUTO3)
                                                    .withinEyeHeight()
                                                    .withinDistance(0.0D, 2.5D))
                                    .nextBehavior(
                                            CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                                    .animationBehavior(WukongAnimations.STAFF_AUTO4)
                                                    .withinEyeHeight()
                                                    .withinDistance(0.0D, 2.5D))
                                    .nextBehavior(
                                            CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder()
                                                    .animationBehavior(WukongAnimations.STAFF_AUTO5)
                                                    .withinEyeHeight()
                                                    .withinDistance(0.0D, 2.5D)));

    // 构造补丁, 阵营设为 UNDEAD 以避免分身被同类战斗 AI 主动攻击
    public FakeWukongEntityPatch() {
        super(Factions.UNDEAD);
    }

    // 初始化动画器: 注册待机/移动/追击/死亡动作
    @Override
    public void initAnimator(Animator animator) {
        animator.addLivingAnimation(LivingMotions.IDLE, WukongAnimations.IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, WukongAnimations.RUN);
        animator.addLivingAnimation(LivingMotions.CHASE, WukongAnimations.RUN);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_COMMON_NEUTRALIZED);
    }

    // 更新运动状态, 复用好战型生物的通用更新逻辑
    @Override
    public void updateMotion(boolean b) {
        super.commonAggressiveMobUpdateMotion(b);
    }

    // 配置武器对应的攻击动作: WK_STAFF 类武器在劈棍式下使用 WK_STAFF 行为
    protected void setWeaponMotions() {
        this.weaponAttackMotions = Maps.newHashMap();
        this.weaponAttackMotions.put(
                WukongWeaponCategories.WK_STAFF, ImmutableMap.of(WukongStyles.SMASH, WK_STAFF));
    }

    @Override
    // 将基础伤害缩放为 0.3 倍作为假悟空的修正伤害
    public float getModifiedBaseDamage(float baseDamage) {
        return 0.3F * baseDamage;
    }
}
