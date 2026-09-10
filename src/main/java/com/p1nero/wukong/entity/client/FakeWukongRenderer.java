package com.p1nero.wukong.entity.client;

import com.p1nero.wukong.entity.FakeWukongEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import org.jetbrains.annotations.NotNull;

// 假悟空实体渲染器: 复用玩家人体模型与盔甲层, 贴图优先采用拥有者皮肤
public class FakeWukongRenderer
        extends HumanoidMobRenderer<FakeWukongEntity, HumanoidModel<FakeWukongEntity>> {
    // 构造渲染器: 使用玩家模型与内外盔甲层, 阴影大小 0.5
    public FakeWukongRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        this.addLayer(
                new HumanoidArmorLayer<>(
                        this,
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                        context.getModelManager()));
    }

    // 获取贴图: 拥有者为玩家则使用其皮肤, 为类人怪物则复用其渲染器贴图, 否则回退到 steve
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull FakeWukongEntity entity) {
        if (Minecraft.getInstance().level != null) {
            Entity owner = entity.getOwner();
            if (owner instanceof AbstractClientPlayer abstractClientPlayer) {
                return abstractClientPlayer.getSkinTextureLocation();
            }
            if (owner != null) {
                EntityRenderer<?> renderer =
                        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(owner);
                if (renderer instanceof HumanoidMobRenderer<?, ?>) {
                    return Minecraft.getInstance()
                            .getEntityRenderDispatcher()
                            .getRenderer(owner)
                            .getTextureLocation(owner);
                }
            }
        }
        return ResourceLocation.parse("minecraft:textures/entity/player/steve.png");
    }
}
