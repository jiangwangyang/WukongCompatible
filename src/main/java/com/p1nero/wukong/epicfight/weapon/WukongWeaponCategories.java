package com.p1nero.wukong.epicfight.weapon;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

// 悟空武器类别枚举: 定义悟空棍(WK_STAFF)这一武器分类, 并提供多种重载的判定方法
public enum WukongWeaponCategories implements WeaponCategory {
    WK_STAFF;

    // 构造枚举项: 向管理器申请唯一 ID
    private WukongWeaponCategories() {
        this.id = WeaponCategory.ENUM_MANAGER.assign(this);
    }

    // 枚举管理器分配的唯一 ID
    final int id;

    // 返回枚举管理器分配的唯一 ID
    @Override
    public int universalOrdinal() {
        return this.id;
    }

    // 判断手持主手武器是否为悟空棍类型
    public static boolean isWeaponValid(LivingEntityPatch<?> playerPatch) {
        return playerPatch
                .getHoldingItemCapability(InteractionHand.MAIN_HAND)
                .getWeaponCategory()
                .equals(WukongWeaponCategories.WK_STAFF);
    }

    // 判断实体主手物品是否为悟空棍类型
    public static boolean isWeaponValid(LivingEntity entity) {
        return isWeaponValid(entity.getMainHandItem());
    }

    // 判断物品堆是否为悟空棍类型
    public static boolean isWeaponValid(ItemStack itemStack) {
        CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(itemStack);
        return capability != null
                && capability.getWeaponCategory().equals(WukongWeaponCategories.WK_STAFF);
    }
}
