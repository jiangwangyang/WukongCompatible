package com.p1nero.wukong.client.particle;

import static com.p1nero.wukong.WukongMoveset.MOD_ID;

import com.p1nero.wukong.entity.client.Ding;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class WuKongEffect {
    public static final DeferredRegister<MobEffect> REGISTRY =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MOD_ID);
    public static final RegistryObject<MobEffect> DING = REGISTRY.register("ding", Ding::new);
}
