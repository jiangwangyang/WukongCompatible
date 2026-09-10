package com.p1nero.wukong.item;

import net.minecraft.world.item.Tier;

import yesman.epicfight.world.item.WeaponItem;

// 测试棍物品: 简单的EpicFight武器物品, 无额外逻辑
public class TestStaff extends WeaponItem {
    // 构造方法
    public TestStaff(Tier tier, int damageIn, float speedIn, Properties builder) {
        super(tier, damageIn, speedIn, builder);
    }
}
