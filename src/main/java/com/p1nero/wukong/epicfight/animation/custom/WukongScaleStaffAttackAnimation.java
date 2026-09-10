package com.p1nero.wukong.epicfight.animation.custom;

import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import net.minecraft.nbt.CompoundTag;

import org.jetbrains.annotations.Nullable;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.BasicAttackAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

// 悟空变长棍攻击动画: 通过 nbt 标记驱动客户端缩放/位移棍子模型, 结束时复位状态
public class WukongScaleStaffAttackAnimation extends BasicAttackAnimation {
    // 构造变长棍攻击动画, 不单独指定 preDelay 时 preDelay 取前摇时长
    public WukongScaleStaffAttackAnimation(
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

    // 构造变长棍攻击动画, 显式指定前摇(antic)与接触前延迟(preDelay)
    public WukongScaleStaffAttackAnimation(
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

    // 构造变长棍攻击动画, 基于单一阶段(Phase), 各时间节点均取前摇时长
    public WukongScaleStaffAttackAnimation(
            float convertTime,
            float antic,
            Joint colliderJoint,
            AnimationManager.AnimationAccessor<? extends BasicAttackAnimation> accessor,
            AssetAccessor<? extends Armature> armature) {
        super(
                convertTime,
                accessor,
                armature,
                new AttackAnimation.Phase(
                        0.0F, antic, antic, antic, antic, Float.MAX_VALUE, colliderJoint, null));
    }

    // 取消加棍势(本动画不计入基础攻击连段)
    @Override
    public boolean isBasicAttackAnimation() {
        return false;
    }

    // 保险: 结束或打断时复位棍子的缩放/位移, 并清除伤害减免标记
    @Override
    public void end(
            LivingEntityPatch<?> entityPatch,
            AssetAccessor<? extends DynamicAnimation> nextAnimation,
            boolean isEnd) {
        super.end(entityPatch, nextAnimation, isEnd);
        if (WukongWeaponCategories.isWeaponValid(entityPatch)) {
            CompoundTag tag = entityPatch.getOriginal().getMainHandItem().getOrCreateTag();
            tag.putBoolean("WK_shouldScaleItem", false);
            tag.putBoolean("WK_shouldTranslateItem", false);

            if (entityPatch instanceof ServerPlayerPatch serverPlayerPatch) {
                var dataManager =
                        serverPlayerPatch.getSkill(SkillSlots.WEAPON_INNATE).getDataManager();
                if (dataManager.hasData(WukongSkillDataKeys.DAMAGE_REDUCE.get())) {
                    dataManager.setData(WukongSkillDataKeys.DAMAGE_REDUCE.get(), -1.0F);
                }
            }
        }
    }
}
