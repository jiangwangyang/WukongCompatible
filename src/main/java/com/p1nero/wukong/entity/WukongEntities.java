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
public class WukongEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, WukongMoveset.MOD_ID);
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

    @SubscribeEvent
    public static void entityAttributeCreationEvent(EntityAttributeCreationEvent event) {
        event.put(
                CLOUD_STEP_LEFT_ENTITY.get(),
                LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 1).build());
    }

    @SubscribeEvent
    public static void setPatch(EntityPatchRegistryEvent event) {
        event.getTypeEntry().put(FAKE_WUKONG_ENTITY.get(), (entity) -> FakeWukongEntityPatch::new);
        Armatures.registerEntityTypeArmature(FAKE_WUKONG_ENTITY.get(), Armatures.BIPED);
    }

    @SubscribeEvent
    public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
        event.put(FAKE_WUKONG_ENTITY.get(), FakeWukongEntity.createAttributes().build());
    }
}
