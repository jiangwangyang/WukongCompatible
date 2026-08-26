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

public class FakeWukongEntityPatch extends HumanoidMobPatch<FakeWukongEntity> {

    public static final CombatBehaviors.Builder<HumanoidMobPatch<?>> WK_STAFF = CombatBehaviors.<HumanoidMobPatch<?>>builder()
            .newBehaviorSeries(
                    CombatBehaviors.BehaviorSeries.<HumanoidMobPatch<?>>builder().weight(100.0F).canBeInterrupted(false).looping(false)
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(WukongAnimations.STAFF_AUTO1).withinEyeHeight().withinDistance(0.0D, 2.5D))
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(WukongAnimations.STAFF_AUTO2).withinEyeHeight().withinDistance(0.0D, 2.5D))
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(WukongAnimations.STAFF_AUTO3).withinEyeHeight().withinDistance(0.0D, 2.5D))
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(WukongAnimations.STAFF_AUTO4).withinEyeHeight().withinDistance(0.0D, 2.5D))
                            .nextBehavior(CombatBehaviors.Behavior.<HumanoidMobPatch<?>>builder().animationBehavior(WukongAnimations.STAFF_AUTO5).withinEyeHeight().withinDistance(0.0D, 2.5D))
            );

    public FakeWukongEntityPatch() {
        super(Factions.UNDEAD);
    }

    @Override
    public void initAnimator(Animator animator) {
        animator.addLivingAnimation(LivingMotions.IDLE, WukongAnimations.IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, WukongAnimations.RUN);
        animator.addLivingAnimation(LivingMotions.CHASE, WukongAnimations.RUN);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_COMMON_NEUTRALIZED);
    }

    @Override
    public void updateMotion(boolean b) {
        super.commonAggressiveMobUpdateMotion(b);
    }

    protected void setWeaponMotions() {
        this.weaponAttackMotions = Maps.newHashMap();
        this.weaponAttackMotions.put(WukongWeaponCategories.WK_STAFF, ImmutableMap.of(WukongStyles.SMASH, WK_STAFF));
    }

    @Override
    public float getModifiedBaseDamage(float baseDamage) {
        return 0.3F * baseDamage;
    }


}
