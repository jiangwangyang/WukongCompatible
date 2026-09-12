package com.p1nero.wukong.client.particle;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.effects.DingEntityAfterImageParticle;
import com.p1nero.wukong.effects.DingParticle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// 粒子类型注册类, 定义定身与残影粒子并在客户端注册其渲染器
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class WuKongParticles {
    // 粒子类型注册表
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, WukongMoveset.MOD_ID);
    // 定身粒子类型(配合 Ding 药剂效果使用)
    public static final RegistryObject<SimpleParticleType> DING =
            PARTICLES.register("ding", () -> new SimpleParticleType(true));
    // 实体残影粒子类型
    public static final RegistryObject<SimpleParticleType> ENTITY_AFTER_IMAGE =
            PARTICLES.register("shen", () -> new SimpleParticleType(true));

    // 空构造, 供 Forge 注册事件订阅器实例
    public WuKongParticles() {}

    // 为定身与残影粒子注册精灵集(sprite set)渲染器
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void RP(RegisterParticleProvidersEvent event) {
        // 弃用API迁移: ParticleEngine.register已弃用, 改用本事件自带的registerSpriteSet(同一函数式接口, 行为一致)
        event.registerSpriteSet(DING.get(), DingParticle.DangerParticleProvider::new);
        event.registerSpriteSet(
                ENTITY_AFTER_IMAGE.get(), DingEntityAfterImageParticle.DangerParticleProvider::new);
    }
}
