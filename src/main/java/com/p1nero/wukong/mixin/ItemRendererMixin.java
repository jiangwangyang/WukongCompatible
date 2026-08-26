package com.p1nero.wukong.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * 缩放的备案，在yesman修复bug之前这玩意儿真好使
 */
@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void render(ItemStack itemStack, ItemDisplayContext p_270188_, boolean p_115146_, PoseStack poseStack, MultiBufferSource p_115148_, int p_115149_, int p_115150_, BakedModel p_115151_, CallbackInfo ci) {
        // 获取物品的能力
        if (p_270188_ == ItemDisplayContext.NONE || p_270188_ == ItemDisplayContext.GUI) {
            return; // 如果是物品栏中的物品，不进行任何修改
        }

        itemStack.getCapability(EpicFightCapabilities.CAPABILITY_ITEM).ifPresent((capabilityItem) -> {
            // 判断是否是 Staff 类型的武器
            if (capabilityItem.getWeaponCategory().equals(WukongWeaponCategories.WK_STAFF)) {
                CompoundTag tag = itemStack.getOrCreateTag();

                // 如果应该进行缩放，获取缩放值
                if (tag.getBoolean("WK_shouldScaleItem")) {
                    poseStack.scale(tag.getFloat("WK_XScale"), tag.getFloat("WK_YScale"), tag.getFloat("WK_ZScale"));
                }

                // 如果应该进行平移，获取平移值
                if (tag.getBoolean("WK_shouldTranslateItem")) {
                    float tx = tag.getFloat("WK_XTranslation");
                    float ty = tag.getFloat("WK_YTranslation");
                    float tz = tag.getFloat("WK_ZTranslation");
                    poseStack.translate(tx, ty, tz);  // 平移物品
                }
            }
        });
    }

}
