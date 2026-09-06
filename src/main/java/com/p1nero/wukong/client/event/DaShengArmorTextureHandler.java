package com.p1nero.wukong.client.event;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.item.DaShengArmorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.client.forgeevent.AnimatedArmorTextureEvent;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// EpicFight 战斗模式下盔甲贴图默认按原版路径解析(材质名 da_sheng 无对应文件), 这里指定为 GeckoLib 贴图
@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, value = Dist.CLIENT)
public class DaShengArmorTextureHandler {
    // 临时诊断: 每件盔甲只记录一次实际使用的网格信息, 验证后移除
    private static final Set<Item> LOGGED_ITEMS = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onAnimatedArmorTexture(AnimatedArmorTextureEvent event) {
        if (event.getItemstack().getItem() instanceof DaShengArmorItem item) {
            event.setResultLocation(new ResourceLocation(WukongMoveset.MOD_ID, "textures/item/armor/dasheng.png"));
            if (LOGGED_ITEMS.add(item)) {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                boolean jsonVisible = Minecraft.getInstance().getResourceManager()
                        .getResource(new ResourceLocation(itemId.getNamespace(), "animmodels/armor/" + itemId.getPath() + ".json"))
                        .isPresent();
                SkinnedMesh mesh = WearableItemLayer.getCachedModel(item);
                if (mesh == null) {
                    WukongMoveset.LOGGER.info("[DaShengDiag] {} jsonVisible={}, mesh=null", itemId, jsonVisible);
                } else {
                    WukongMoveset.LOGGER.info("[DaShengDiag] {} jsonVisible={}, positions={}, maxJointCount={}, weights={}",
                            itemId, jsonVisible, mesh.positions().length / 3, mesh.getMaxJointCount(), mesh.weights().length);
                }
            }
        }
    }
}
