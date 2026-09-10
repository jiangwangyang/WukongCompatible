package com.p1nero.wukong.epicfight.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

// 伤害类型标签常量: 提供与 epicfight 兼容的伤害类型标签
public final class EpicFightDamageType {
    // 部分伤害标签: 标记可被格挡/部分减免的伤害类型
    public static final TagKey<DamageType> PARTIAL_DAMAGE =
            TagKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("epicfight", "partial_damage"));

    // 禁止实例化
    private EpicFightDamageType() {}
}
