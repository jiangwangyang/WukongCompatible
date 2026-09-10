package com.p1nero.wukong.client.event;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.item.DaShengArmorItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import yesman.epicfight.api.client.forgeevent.AnimatedArmorTextureEvent;

// EpicFight 战斗模式下指定大圣盔甲使用 GeckoLib 贴图 (与 EpicFight GeckolibCompat 的解析结果一致, 双保险)
@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, value = Dist.CLIENT)
public class DaShengArmorTextureHandler {
    // 当渲染大圣盔甲时, 指定其使用 GeckoLib 贴图
    @SubscribeEvent
    public static void onAnimatedArmorTexture(AnimatedArmorTextureEvent event) {
        if (event.getItemstack().getItem() instanceof DaShengArmorItem) {
            event.setResultLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            WukongMoveset.MOD_ID, "textures/item/armor/dasheng.png"));
        }
    }
}
