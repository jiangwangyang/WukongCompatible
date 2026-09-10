package com.p1nero.wukong.epicfight.weapon;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.animation.WukongGreatSageAnimations;
import com.p1nero.wukong.epicfight.skill.WukongSkills;
import com.p1nero.wukong.epicfight.skill.custom.wukong.StaffStance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

import java.util.function.Function;

@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
// 悟空棍武器能力预设: 定义棍类武器的碰撞体/命中音效粒子, 以及各棍式(劈棍/戳棍/立棍/大圣)的连段/固有重击技能与生活动作动画
public class WukongWeaponCapabilityPresets {
    // 说明:
    // 本类使用的WeaponCapability$Builder的livingMotionModifier/innateSkill/newStyleCombo/styleProvider/comboCancel/passiveSkill
    // 在当前依赖EpicFight 20.14.17(MC 1.20.1)中虽被标记@Deprecated(since=1.21.1, forRemoval=true)但不存在替代API,
    // EpicFight官方的WeaponCapabilityPresets与WeaponTypeReloadListener同样在使用这些方法, 属于1.20.1下的标准用法
    // 因此保留现状与编译弃用警告, 待将来升级EpicFight至1.21.1+版本时再迁移, 请勿在此处添加SuppressWarnings

    // 标准棍预设: 支持劈棍/戳棍/立棍/大圣四种棍式的连段/重击与生活动作, 附带棍花被动技能
    public static final Function<Item, CapabilityItem.Builder> STAFF =
            (item) ->
                    (CapabilityItem.Builder)
                            WeaponCapability.builder()
                                    .category(WukongWeaponCategories.WK_STAFF)
                                    .styleProvider(
                                            (livingEntityPatch) -> {
                                                if (livingEntityPatch
                                                        instanceof PlayerPatch<?> playerPatch) {
                                                    SkillContainer container =
                                                            playerPatch.getSkill(
                                                                    WukongSkillSlots.STAFF_STYLE);

                                                    if (container != null
                                                            && container.getSkill()
                                                                    instanceof StaffStance style) {
                                                        return style.getStyle(container);
                                                    } else {
                                                        return WukongStyles.SMASH;
                                                    }
                                                }
                                                // 默认风格
                                                return WukongStyles.SMASH;
                                            })
                                    .collider(WukongColliders.WK_STAFF)
                                    .hitSound(EpicFightSounds.BLUNT_HIT.get())
                                    .hitParticle(EpicFightParticles.HIT_BLUNT.get())
                                    .canBePlacedOffhand(false)
                                    .comboCancel((style) -> false)
                                    .passiveSkill(WukongSkills.STAFF_SPIN)
                                    .newStyleCombo(
                                            WukongStyles.SMASH, // 劈棍
                                            WukongAnimations.STAFF_AUTO1,
                                            WukongAnimations.STAFF_AUTO2,
                                            WukongAnimations.STAFF_AUTO3,
                                            WukongAnimations.STAFF_AUTO4,
                                            WukongAnimations.STAFF_AUTO5,
                                            WukongAnimations.STAFF_AUTO1_DASH,
                                            WukongAnimations.JUMP_ATTACK_LIGHT)
                                    .innateSkill(
                                            WukongStyles.SMASH,
                                            (itemstack) -> WukongSkills.SMASH_HEAVY_ATTACK)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.IDLE,
                                            WukongAnimations.IDLE)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.WALK,
                                            WukongAnimations.RUN)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.CHASE,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.RUN,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.SWIM,
                                            WukongAnimations.WALK)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.JUMP,
                                            WukongAnimations.JUMP)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.FALL,
                                            WukongAnimations.FALL)
                                    .newStyleCombo(
                                            WukongStyles.THRUST, // 戳棍
                                            WukongAnimations.STAFF_AUTO1,
                                            WukongAnimations.STAFF_AUTO2,
                                            WukongAnimations.STAFF_AUTO3,
                                            WukongAnimations.STAFF_AUTO4,
                                            WukongAnimations.STAFF_AUTO5,
                                            WukongAnimations.STAFF_AUTO1_DASH,
                                            WukongAnimations.JUMP_ATTACK_LIGHT)
                                    .innateSkill(
                                            WukongStyles.THRUST,
                                            (itemstack) -> WukongSkills.THRUST_HEAVY_ATTACK)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.IDLE,
                                            WukongAnimations.IDLE)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.WALK,
                                            WukongAnimations.RUN)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.CHASE,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.RUN,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.SWIM,
                                            WukongAnimations.WALK)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.JUMP,
                                            WukongAnimations.JUMP)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.FALL,
                                            WukongAnimations.FALL)
                                    .newStyleCombo(
                                            WukongStyles.PILLAR, // 立棍
                                            WukongAnimations.STAFF_AUTO1,
                                            WukongAnimations.STAFF_AUTO2,
                                            WukongAnimations.STAFF_AUTO3,
                                            WukongAnimations.STAFF_AUTO4,
                                            WukongAnimations.STAFF_AUTO5,
                                            WukongAnimations.STAFF_AUTO1,
                                            WukongAnimations.STAFF_AUTO1_DASH,
                                            WukongAnimations.JUMP_ATTACK_LIGHT)
                                    .innateSkill(
                                            WukongStyles.PILLAR,
                                            (itemstack) -> WukongSkills.PILLAR_HEAVY_ATTACK)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.IDLE,
                                            WukongAnimations.IDLE)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.WALK,
                                            WukongAnimations.RUN)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.CHASE,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.RUN,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.SWIM,
                                            WukongAnimations.WALK)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.JUMP,
                                            WukongAnimations.JUMP)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.FALL,
                                            WukongAnimations.FALL)
                                    .newStyleCombo(
                                            WukongStyles.GREATSAGE, // 大圣模型
                                            WukongGreatSageAnimations.STAFF_AUTO1,
                                            WukongGreatSageAnimations.STAFF_AUTO2,
                                            WukongGreatSageAnimations.STAFF_AUTO3,
                                            WukongGreatSageAnimations.STAFF_AUTO4,
                                            WukongGreatSageAnimations.STAFF_AUTO5,
                                            WukongGreatSageAnimations.STAFF_AUTO1_DASH,
                                            WukongAnimations.JUMP_ATTACK_LIGHT)
                                    .innateSkill(
                                            WukongStyles.GREATSAGE,
                                            (itemstack) -> WukongSkills.GREATSAGE_HEAVY_ATTACK)
                                    .livingMotionModifier(
                                            WukongStyles.GREATSAGE,
                                            LivingMotions.IDLE,
                                            WukongAnimations.IDLE)
                                    .livingMotionModifier(
                                            WukongStyles.GREATSAGE,
                                            LivingMotions.WALK,
                                            WukongAnimations.RUN)
                                    .livingMotionModifier(
                                            WukongStyles.GREATSAGE,
                                            LivingMotions.CHASE,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.GREATSAGE,
                                            LivingMotions.RUN,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.GREATSAGE,
                                            LivingMotions.SWIM,
                                            WukongAnimations.WALK)
                                    .livingMotionModifier(
                                            WukongStyles.GREATSAGE,
                                            LivingMotions.JUMP,
                                            WukongAnimations.JUMP)
                                    .livingMotionModifier(
                                            WukongStyles.GREATSAGE,
                                            LivingMotions.FALL,
                                            WukongAnimations.FALL);

    // 三棍式预设: 支持劈棍/戳棍/立棍三种棍式(不含大圣), 其余配置与STAFF一致
    public static final Function<Item, CapabilityItem.Builder> PILLAR_ONLY =
            (item) ->
                    (CapabilityItem.Builder)
                            WeaponCapability.builder()
                                    .category(WukongWeaponCategories.WK_STAFF)
                                    .styleProvider(
                                            (livingEntityPatch) -> {
                                                if (livingEntityPatch
                                                        instanceof PlayerPatch<?> playerPatch) {
                                                    SkillContainer container =
                                                            playerPatch.getSkill(
                                                                    WukongSkillSlots.STAFF_STYLE);

                                                    if (container != null
                                                            && container.getSkill()
                                                                    instanceof StaffStance style) {
                                                        return style.getStyle(container);
                                                    } else {
                                                        return WukongStyles.SMASH;
                                                    }
                                                }
                                                // 默认风格
                                                return WukongStyles.SMASH;
                                            })
                                    .collider(WukongColliders.WK_STAFF)
                                    .hitSound(EpicFightSounds.BLUNT_HIT.get())
                                    .hitParticle(EpicFightParticles.HIT_BLUNT.get())
                                    .canBePlacedOffhand(false)
                                    .comboCancel((style) -> false)
                                    .passiveSkill(WukongSkills.STAFF_SPIN)
                                    // 劈棍
                                    .newStyleCombo(
                                            WukongStyles.SMASH,
                                            WukongAnimations.STAFF_AUTO1,
                                            WukongAnimations.STAFF_AUTO2,
                                            WukongAnimations.STAFF_AUTO3,
                                            WukongAnimations.STAFF_AUTO4,
                                            WukongAnimations.STAFF_AUTO5,
                                            WukongAnimations.STAFF_AUTO1_DASH,
                                            WukongAnimations.JUMP_ATTACK_LIGHT)
                                    .innateSkill(
                                            WukongStyles.SMASH,
                                            (itemstack) -> WukongSkills.SMASH_HEAVY_ATTACK)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.IDLE,
                                            WukongAnimations.IDLE)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.WALK,
                                            WukongAnimations.RUN)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.CHASE,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.RUN,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.SWIM,
                                            WukongAnimations.WALK)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.JUMP,
                                            WukongAnimations.JUMP)
                                    .livingMotionModifier(
                                            WukongStyles.SMASH,
                                            LivingMotions.FALL,
                                            WukongAnimations.FALL)
                                    // 戳棍
                                    .newStyleCombo(
                                            WukongStyles.THRUST,
                                            WukongAnimations.STAFF_AUTO1,
                                            WukongAnimations.STAFF_AUTO2,
                                            WukongAnimations.STAFF_AUTO3,
                                            WukongAnimations.STAFF_AUTO4,
                                            WukongAnimations.STAFF_AUTO5,
                                            WukongAnimations.STAFF_AUTO1_DASH,
                                            WukongAnimations.JUMP_ATTACK_LIGHT)
                                    .innateSkill(
                                            WukongStyles.THRUST,
                                            (itemstack) -> WukongSkills.THRUST_HEAVY_ATTACK)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.IDLE,
                                            WukongAnimations.IDLE)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.WALK,
                                            WukongAnimations.RUN)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.CHASE,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.RUN,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.SWIM,
                                            WukongAnimations.WALK)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.JUMP,
                                            WukongAnimations.JUMP)
                                    .livingMotionModifier(
                                            WukongStyles.THRUST,
                                            LivingMotions.FALL,
                                            WukongAnimations.FALL)
                                    // 立棍
                                    .newStyleCombo(
                                            WukongStyles.PILLAR,
                                            WukongAnimations.STAFF_AUTO1,
                                            WukongAnimations.STAFF_AUTO2,
                                            WukongAnimations.STAFF_AUTO3,
                                            WukongAnimations.STAFF_AUTO4,
                                            WukongAnimations.STAFF_AUTO5,
                                            WukongAnimations.STAFF_AUTO1,
                                            WukongAnimations.STAFF_AUTO1_DASH,
                                            WukongAnimations.JUMP_ATTACK_LIGHT)
                                    .innateSkill(
                                            WukongStyles.PILLAR,
                                            (itemstack) -> WukongSkills.PILLAR_HEAVY_ATTACK)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.IDLE,
                                            WukongAnimations.IDLE)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.WALK,
                                            WukongAnimations.RUN)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.CHASE,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.RUN,
                                            WukongAnimations.DASH)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.SWIM,
                                            WukongAnimations.WALK)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.JUMP,
                                            WukongAnimations.JUMP)
                                    .livingMotionModifier(
                                            WukongStyles.PILLAR,
                                            LivingMotions.FALL,
                                            WukongAnimations.FALL);

    // 将标准棍预设注册到EpicFight的武器能力预设表(键为wk_staff)
    @SubscribeEvent
    public static void register(WeaponCapabilityPresetRegistryEvent event) {
        event.getTypeEntry()
                .put(
                        ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "wk_staff"),
                        STAFF);
    }
}
