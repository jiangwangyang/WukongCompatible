package com.p1nero.wukong.epicfight.animation.custom;

import com.p1nero.wukong.capability.WKPlayer;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.BasicAttackAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mojang.text2speech.Narrator.LOGGER;

public class WukongScaleStaffAttackAnimation extends BasicAttackAnimation {
    public WukongScaleStaffAttackAnimation(float convertTime, float antic, float contact, float recovery, @Nullable Collider collider, Joint colliderJoint, AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        super(convertTime, antic, contact, recovery, collider, colliderJoint, accessor, armature);
    }

    public WukongScaleStaffAttackAnimation(float convertTime, float antic, float preDelay, float contact, float recovery, @Nullable Collider collider, Joint colliderJoint, AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        super(convertTime, antic, preDelay, contact, recovery, collider, colliderJoint, accessor, armature);
    }

    public WukongScaleStaffAttackAnimation(float convertTime, float antic, Joint colliderJoint, AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
        super(convertTime, accessor, armature, new AttackAnimation.Phase(0.0F, antic, antic, antic, antic, Float.MAX_VALUE, colliderJoint, null));
    }


    /**
     * 取消加棍势
     */
    @Override
    public boolean isBasicAttackAnimation() {
        return false;
    }
    public void CameraResetFov(int durationTicks) {
      /*  Minecraft MC = Minecraft.getInstance();
        float currentFov = MC.options.fov().get();
        float targetFov = 70F;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger tickCount = new AtomicInteger(0);

        Runnable task = () -> {
            int currentTick = tickCount.incrementAndGet();
            if (currentTick > durationTicks) {
                scheduler.shutdown();
                MC.options.fov().set((int) targetFov);
                return;
            }
            float progress = (float) currentTick / durationTicks;
            float newFov = currentFov + (targetFov - currentFov) * progress;
            MC.options.fov().set((int) newFov);
        };
        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.MILLISECONDS);
        */

       // Minecraft.getInstance().options.fov().set(WKPlayer.getFovsz());

    }


    /**
     * 保险，复位棍子的缩放
     */
    @Override
    public void end(LivingEntityPatch<?> entityPatch, AssetAccessor<? extends DynamicAnimation> nextAnimation, boolean isEnd) {
        super.end(entityPatch, nextAnimation, isEnd);
        if(WukongWeaponCategories.isWeaponValid(entityPatch)){
            CompoundTag tag = entityPatch.getOriginal().getMainHandItem().getOrCreateTag();
            tag.putBoolean("WK_shouldScaleItem", false);
            tag.putBoolean("WK_shouldScaleItem", false);
            tag.putBoolean("WK_shouldTranslateItem", false);

            if(entityPatch instanceof ServerPlayerPatch serverPlayerPatch){
                var dataManager = serverPlayerPatch.getSkill(SkillSlots.WEAPON_INNATE).getDataManager();
                if (dataManager.hasData(WukongSkillDataKeys.DAMAGE_REDUCE.get())) {
                    dataManager.setData(WukongSkillDataKeys.DAMAGE_REDUCE.get(), -1.0F);
                }
                CameraResetFov(35);
            }
        }
    }
}
