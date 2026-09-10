package com.p1nero.wukong.item.client;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.item.DaShengArmorItem;

import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

// 大圣套装盔甲渲染器: 基于GeckoLib加载armor/dasheng模型渲染护甲
public class DaShengArmorRenderer extends GeoArmorRenderer<DaShengArmorItem> {
    // 构造方法, 加载大圣护甲模型
    public DaShengArmorRenderer() {
        super(
                new DefaultedItemGeoModel<>(
                        ResourceLocation.fromNamespaceAndPath(
                                WukongMoveset.MOD_ID, "armor/dasheng")));
    }
}
