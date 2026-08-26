package com.p1nero.wukong.epicfight.weapon;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

public enum WukongWeaponCategories implements WeaponCategory {
    WK_STAFF;
    private WukongWeaponCategories(){
        this.id = WeaponCategory.ENUM_MANAGER.assign(this);
    }
    final int id;
    @Override
    public int universalOrdinal() {
        return this.id;
    }

    /**
     * 判断武器是否是悟空棍子类型
     */
    public static boolean isWeaponValid(LivingEntityPatch<?> playerPatch){
        return playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND).getWeaponCategory().equals(WukongWeaponCategories.WK_STAFF);
    }

    public static boolean isWeaponValid(LivingEntity entity) {
        return isWeaponValid(entity.getMainHandItem());
    }

    public static boolean isWeaponValid(ItemStack itemStack) {
        CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(itemStack);
        return capability != null && capability.getWeaponCategory().equals(WukongWeaponCategories.WK_STAFF);
    }

}
