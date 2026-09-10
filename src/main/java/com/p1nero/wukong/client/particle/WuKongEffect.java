package com.p1nero.wukong.client.particle;

import static com.p1nero.wukong.WukongMoveset.MOD_ID;

import com.p1nero.wukong.entity.client.Ding;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// 模组自定义药剂(状态)效果注册类
public class WuKongEffect {
    // 药剂效果注册表
    public static final DeferredRegister<MobEffect> REGISTRY =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MOD_ID);
    // 定身效果
    public static final RegistryObject<MobEffect> DING = REGISTRY.register("ding", Ding::new);
}
