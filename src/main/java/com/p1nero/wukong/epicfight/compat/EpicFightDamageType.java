package com.p1nero.wukong.epicfight.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public final class EpicFightDamageType {
    public static final TagKey<DamageType> PARTIAL_DAMAGE =
            TagKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("epicfight", "partial_damage"));

    private EpicFightDamageType() {}
}
