package com.p1nero.wukong.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

import yesman.epicfight.world.item.WeaponItem;

// 棍武器物品: 简单的EpicFight武器物品, 不可损耗耐久
public class TestStaff extends WeaponItem {
    // 构造方法
    public TestStaff(Tier tier, int damageIn, float speedIn, Properties builder) {
        super(tier, damageIn, speedIn, builder);
    }

    // 物品不可损耗耐久
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }
}
