package com.p1nero.wukong.epicfight.animation.custom;

import static com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys.PLAYING_STAFF_SPIN;

import com.p1nero.wukong.client.event.CameraAnim;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import net.minecraft.client.player.LocalPlayer;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

// 尝试修改动画播放时的移动锁定, 后续改为直接监听输入事件来取消 input (棍花旋转攻击)
public class StaffSpinAttackAnimation extends BasicMultipleAttackAnimation {

    // 构造棍花旋转攻击: 4 段相位按 damageMultiplier 加伤, 不可被移动取消, 播放速度固定 1.5 倍, 双手时开局拉近镜头
    public StaffSpinAttackAnimation(
            float end,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> accessor,
            HumanoidArmature biped,
            float damageMultiplier,
            boolean isTwoHand) {
        super(
                0,
                accessor,
                Armatures.BIPED,
                new AttackAnimation.Phase(0.0F, 0.00F, 0.25F, end, 0.26F, biped.toolR, null)
                        .addProperty(
                                AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER,
                                ValueModifier.multiplier(damageMultiplier)),
                new AttackAnimation.Phase(0.24F, 0.25F, 0.50F, end, 0.51F, biped.toolR, null)
                        .addProperty(
                                AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER,
                                ValueModifier.multiplier(damageMultiplier)),
                new AttackAnimation.Phase(0.49F, 0.50F, 0.75F, end, 0.76F, biped.toolR, null)
                        .addProperty(
                                AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER,
                                ValueModifier.multiplier(damageMultiplier)),
                new AttackAnimation.Phase(0.74F, 0.74F, 1.0F, end, end, biped.toolR, null)
                        .addProperty(
                                AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER,
                                ValueModifier.multiplier(damageMultiplier)));
        this.addProperty(AnimationProperty.ActionAnimationProperty.CANCELABLE_MOVE, false);
        this.addProperty(
                        AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER,
                        ((dynamicAnimation, livingEntityPatch, v, v1, v2) -> 1.5F))
                .addEvents(
                        AnimationProperty.StaticAnimationProperty.ON_BEGIN_EVENTS,
                        AnimationEvent.SimpleEvent.create(
                                ((livingEntityPatch, staticAnimation, objects) -> {
                                    if (isTwoHand
                                            && livingEntityPatch.getOriginal()
                                                    instanceof LocalPlayer) {
                                        CameraAnim.zoomIn(new Vec3f(-1.0F, 0.0F, 1.25F), 20);
                                    }
                                }),
                                AnimationEvent.Side.CLIENT));
    }

    // 动画开始: 若玩家武器合法, 标记正在播放棍花旋转(PLAYING_STAFF_SPIN)
    @Override
    public void begin(LivingEntityPatch<?> entityPatch) {
        super.begin(entityPatch);
        if (entityPatch instanceof ServerPlayerPatch serverPlayerPatch
                && WukongWeaponCategories.isWeaponValid(serverPlayerPatch)) {
            SkillContainer passiveContainer = serverPlayerPatch.getSkill(SkillSlots.WEAPON_PASSIVE);
            passiveContainer.getDataManager().setDataSync(PLAYING_STAFF_SPIN.get(), true);
        }
    }

    // 动画结束: 取消播放棍花旋转标记, 客户端若仍在瞄准则复位镜头
    @Override
    public void end(
            LivingEntityPatch<?> entityPatch,
            AssetAccessor<? extends DynamicAnimation> nextAnimation,
            boolean isEnd) {
        super.end(entityPatch, nextAnimation, isEnd);
        if (entityPatch instanceof ServerPlayerPatch serverPlayerPatch
                && WukongWeaponCategories.isWeaponValid(serverPlayerPatch)) {
            SkillContainer passiveContainer = serverPlayerPatch.getSkill(SkillSlots.WEAPON_PASSIVE);
            passiveContainer.getDataManager().setDataSync(PLAYING_STAFF_SPIN.get(), false);
        }
        if (entityPatch.isLogicalClient() && CameraAnim.isAiming()) {
            CameraAnim.zoomOut(20); // 兜底拉远相机
        }
    }
}
