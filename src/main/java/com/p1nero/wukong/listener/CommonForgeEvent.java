package com.p1nero.wukong.listener;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.skill.WukongSkills;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeSkill;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

// 通用Forge事件监听(服务端): 处理棍武器摔落免伤与悟空技能自动学习/默认技能装配
@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID)
public class CommonForgeEvent {
    // 持有棍类武器的玩家受到摔落伤害时取消伤害并重置摔落高度
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!event.getSource().is(DamageTypes.FALL)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CapabilityItem capabilityItem =
                EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
        if (capabilityItem != null
                && capabilityItem.getWeaponCategory().equals(WukongWeaponCategories.WK_STAFF)) {
            player.resetFallDistance();
            event.setCanceled(true);
        }
    }

    // 服务端玩家每tick: 自动学习全部悟空技能, 并为空的技能槽位装配默认技能,
    // 装配棍式槽位时刷新武器固有技能
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        ServerPlayerPatch patch =
                EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (patch == null || !WukongWeaponCategories.isWeaponValid(patch)) {
            return;
        }

        CapabilityItem itemCapability =
                EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
        player.getCapability(EpicFightCapabilities.CAPABILITY_SKILL)
                .ifPresent(
                        skills -> {
                            for (Skill skill : WukongSkills.getSelectableSkills()) {
                                if (skill != null && !skills.hasLearned(skill)) {
                                    skills.addLearnedSkill(skill);
                                }
                            }
                            if (equipDefault(
                                    patch,
                                    skills,
                                    WukongSkillSlots.STAFF_STYLE,
                                    WukongSkills.SMASH_STYLE)) {
                                if (itemCapability != null) {
                                    itemCapability.changeWeaponInnateSkill(
                                            patch, player.getMainHandItem());
                                }
                            }
                            equipDefault(
                                    patch,
                                    skills,
                                    WukongSkillSlots.SHENFA_SKILL_SLOT,
                                    WukongSkills.SPELL_JUXINGSANQI);
                            equipDefault(
                                    patch,
                                    skills,
                                    WukongSkillSlots.FASHU_SKILL_SLOT,
                                    WukongSkills.MAGI_DINGSHENFA);
                            equipDefault(
                                    patch,
                                    skills,
                                    WukongSkillSlots.HAO_MAO,
                                    WukongSkills.SHEN_WAI_SHEN_FA);
                        });
    }

    // 为空槽位装配默认技能并同步给客户端, 返回是否装配成功
    private static boolean equipDefault(
            ServerPlayerPatch patch, CapabilitySkill skills, SkillSlot slot, Skill skill) {
        if (skill == null) {
            return false;
        }

        SkillContainer container = patch.getSkill(slot);
        if (container == null || !container.isEmpty() || !container.setSkill(skill)) {
            return false;
        }

        skills.addLearnedSkill(skill);
        ServerPlayer player = patch.getOriginal();
        EpicFightNetworkManager.sendToPlayer(
                new SPChangeSkill(slot, player.getId(), skill), player);
        return true;
    }
}
