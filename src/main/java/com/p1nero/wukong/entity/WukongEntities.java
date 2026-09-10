package com.p1nero.wukong.entity;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.capability.entity.FakeWukongEntityPatch;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import yesman.epicfight.api.forgeevent.EntityPatchRegistryEvent;
import yesman.epicfight.gameasset.Armatures;

@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
// 本模组实体注册中心: 注册假悟空与筋斗云残留实体, 并绑定属性与 Patch
public class WukongEntities {
    // 实体类型延迟注册表
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WukongMoveset.MOD_ID);
    // 假悟空实体类型: 怪物分类, 体型 0.9x3.5, 防火, 客户端追踪范围 10
    public static final RegistryObject<EntityType<FakeWukongEntity>> FAKE_WUKONG_ENTITY =
            ENTITIES.register(
                    "fake_wukong_entity",
                    () ->
                            EntityType.Builder.<FakeWukongEntity>of(
                                            FakeWukongEntity::new, MobCategory.MONSTER)
                                    .fireImmune()
                                    .sized(0.9F, 3.5F)
                                    .clientTrackingRange(10)
                                    .build("fake_wukong_entity"));
    // 筋斗云残留实体类型: 杂项分类, 体型 1x1, 不召唤不保存, 高频同步
    public static final RegistryObject<EntityType<CloudStepLeftEntity>> CLOUD_STEP_LEFT_ENTITY =
            ENTITIES.register(
                    "cloud_step_left_entity",
                    () ->
                            EntityType.Builder.<CloudStepLeftEntity>of(
                                            CloudStepLeftEntity::new, MobCategory.MISC)
                                    .sized(1.0F, 1.0F)
                                    .clientTrackingRange(6)
                                    .updateInterval(1)
                                    .noSummon()
                                    .noSave()
                                    .build("cloud_step_left_entity"));

    // 为筋斗云残留实体注册基础生命属性(最大生命 1)
    @SubscribeEvent
    public static void entityAttributeCreationEvent(EntityAttributeCreationEvent event) {
        event.put(
                CLOUD_STEP_LEFT_ENTITY.get(),
                LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 1).build());
    }

    // 为假悟空实体绑定 EntityPatch 与人体骨骼(Armature)
    @SubscribeEvent
    public static void setPatch(EntityPatchRegistryEvent event) {
        event.getTypeEntry().put(FAKE_WUKONG_ENTITY.get(), (entity) -> FakeWukongEntityPatch::new);
        Armatures.registerEntityTypeArmature(FAKE_WUKONG_ENTITY.get(), Armatures.BIPED);
    }

    // 为假悟空实体注册完整属性(含战斗框架属性)
    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(FAKE_WUKONG_ENTITY.get(), FakeWukongEntity.createAttributes().build());
    }
}
