package com.p1nero.wukong.epicfight.skill.custom.wukong;

import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.skill.WukongSkills;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.network.packet.BasePacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeSkill;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

// 切换棍式的网络数据包: 客户端请求服务端将武器固有技能切换为指定棍式,
// 切换时保留棍势(星数)与耐力, 并向周围玩家广播技能变更
public record UpdateWeaponInnatePacket(int styleIndex) implements BasePacket {
    // 将棍式索引写入网络缓冲
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(styleIndex);
    }

    // 从网络缓冲读取棍式索引并构造数据包
    public static UpdateWeaponInnatePacket decode(FriendlyByteBuf buf) {
        return new UpdateWeaponInnatePacket(buf.readVarInt());
    }

    // 服务端执行: 校验后切换STAFF_STYLE槽位的棍式技能并刷新武器固有技能,
    // 把切换前的棍势与耐力同步回新的技能容器, 最后广播SPChangeSkill包
    @Override
    public void execute(Player player) {
        if (player == null || !player.isAlive()) {
            return;
        }

        ServerPlayerPatch patch =
                EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        Skill selectedStyle = getStyle(styleIndex);
        if (patch == null
                || selectedStyle == null
                || !WukongWeaponCategories.isWeaponValid(patch)) {
            return;
        }

        SkillContainer styleContainer = patch.getSkill(WukongSkillSlots.STAFF_STYLE);
        SkillContainer weaponContainer = patch.getSkill(SkillSlots.WEAPON_INNATE);
        if (styleContainer == null || weaponContainer == null) {
            return;
        }

        float resource = weaponContainer.getResource();
        int stack = weaponContainer.getStack();
        styleContainer.setSkill(selectedStyle);
        patch.getSkillCapability().addLearnedSkill(selectedStyle);

        ItemStack heldItem = player.getMainHandItem();
        CapabilityItem itemCapability = EpicFightCapabilities.getItemStackCapability(heldItem);
        if (itemCapability == null) {
            return;
        }
        itemCapability.changeWeaponInnateSkill(patch, heldItem);

        SkillContainer refreshedWeaponContainer = patch.getSkill(SkillSlots.WEAPON_INNATE);
        if (refreshedWeaponContainer != null && !refreshedWeaponContainer.isEmpty()) {
            refreshedWeaponContainer
                    .getSkill()
                    .setStackSynchronize(refreshedWeaponContainer, stack);
            refreshedWeaponContainer
                    .getSkill()
                    .setConsumptionSynchronize(refreshedWeaponContainer, resource);
        }

        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(
                new SPChangeSkill(WukongSkillSlots.STAFF_STYLE, player.getId(), selectedStyle),
                patch.getOriginal());
    }

    // 根据索引返回对应棍式技能(0劈棍/1戳棍/2立棍/3大圣, 越界返回null)
    private static Skill getStyle(int index) {
        return switch (index) {
            case 0 -> WukongSkills.SMASH_STYLE;
            case 1 -> WukongSkills.THRUST_STYLE;
            case 2 -> WukongSkills.PILLAR_STYLE;
            case 3 -> WukongSkills.GREATSAGE_STYLE;
            default -> null;
        };
    }
}
