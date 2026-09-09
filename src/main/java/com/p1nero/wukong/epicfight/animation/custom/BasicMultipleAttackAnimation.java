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

/** Compatibility wrapper for multi-phase basic attacks. */
public class BasicMultipleAttackAnimation extends AttackAnimation {
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

    public BasicMultipleAttackAnimation(
            float convertTime,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature,
            boolean coordSetter,
            AttackAnimation.Phase... phases) {
        this(convertTime, accessor, armature, phases);
    }

    public BasicMultipleAttackAnimation(
            float convertTime,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature,
            AttackAnimation.Phase... phases) {
        super(convertTime, accessor, armature, phases);
        this.newTimePair(0.0F, Float.MAX_VALUE);
        this.addStateRemoveOld(EntityState.TURNING_LOCKED, false);
    }

    @Override
    public void postInit() {
        super.postInit();
        if (!this.properties.containsKey(AttackAnimationProperty.BASIS_ATTACK_SPEED)) {
            float basisSpeed =
                    Float.parseFloat(String.format(Locale.US, "%.2f", 1.0F / this.getTotalTime()));
            this.addProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED, basisSpeed);
        }
    }

    public boolean isBasicAttackAnimation() {
        return true;
    }
}
