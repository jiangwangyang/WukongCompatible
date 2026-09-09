package com.p1nero.wukong.listener;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.entity.WukongEntities;
import com.p1nero.wukong.entity.client.FakeWukongRenderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(
        modid = WukongMoveset.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModBusEvent {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(WukongEntities.FAKE_WUKONG_ENTITY.get(), FakeWukongRenderer::new);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderPatched(PatchedRenderersEvent.Add event) {
        EntityRendererProvider.Context context = event.getContext();
        event.addPatchedEntityRenderer(
                WukongEntities.FAKE_WUKONG_ENTITY.get(),
                (entityType) ->
                        new PHumanoidRenderer<>(Meshes.ALEX, context, entityType)
                                .initLayerLast(context, entityType));
    }
}
