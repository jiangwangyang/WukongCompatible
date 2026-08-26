package com.p1nero.wukong.item.client;


import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.item.DaShengArmorItem;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class DaShengArmorRenderer extends GeoArmorRenderer<DaShengArmorItem> {
    public DaShengArmorRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(WukongMoveset.MOD_ID, "armor/dasheng")));
    }
}

