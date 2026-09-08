package com.p1nero.wukong.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.fashu.ShenfaJuxingsanqiSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;


@Mixin(value = PatchedLivingEntityRenderer.class, remap = false)

public class PatchedLivingEntityRendererMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatchedLivingEntityRendererMixin.class);

    /**
     * 记录当前正在渲染的实体, 用于在 modifyArg 中判断透明度修改是否只应作用于本地玩家。
     * 聚形散气隐身只影响玩家自己, 不应让周围怪物也隐身。
     */
    private static final ThreadLocal<LivingEntity> CURRENT_RENDERING_ENTITY = new ThreadLocal<>();

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;IF)V",
            at = @At("HEAD"))
    private void captureRenderingEntity(LivingEntity entity, LivingEntityPatch<?> patch, @SuppressWarnings("rawtypes") net.minecraft.client.renderer.entity.LivingEntityRenderer renderer,
                                        net.minecraft.client.renderer.MultiBufferSource bufferSource, PoseStack poseStack, int packedLight,
                                        float partialTick, CallbackInfo ci) {
        CURRENT_RENDERING_ENTITY.set(entity);
    }

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;IF)V",
            at = @At(value = "INVOKE", target = "Lyesman/epicfight/api/client/model/SkinnedMesh;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;IFFFFILyesman/epicfight/api/model/Armature;[Lyesman/epicfight/api/utils/math/OpenMatrix4f;)V"),
            index = 7)
    private float modifyAlpha(float alpha) {
        // 只有本地玩家自己才应用聚形散气的隐身透明度, 避免周围怪物跟着隐身
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null || CURRENT_RENDERING_ENTITY.get() != localPlayer) {
            return alpha;
        }
        LocalPlayer player = localPlayer;
            LocalPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, LocalPlayerPatch.class);
        if (patch == null) {
            return alpha;
        }
//            if (!isHoldingJingubang(player)) {
//                return 1.0F;
//            }
        SkillContainer shenFa = patch.getSkill(WukongSkillSlots.SHENFA_SKILL_SLOT);
        if (shenFa != null) {
            SkillDataManager manager = shenFa.getDataManager();
            Integer timerValue = manager.getDataValue(WukongSkillDataKeys.JXSQ_YINGSHEN_TIMER.get());
            Boolean ztValue = manager.getDataValue(WukongSkillDataKeys.JXSQ_YINGSHEN_ZT.get());
            if (timerValue != null && ztValue != null && timerValue > 1 && !ztValue) {
                float maxTimer = ShenfaJuxingsanqiSkill.MAX_TRANSPARENT_TIMER;
                if (maxTimer <= 0) {
                    return alpha;
                }

                float ratio = timerValue / maxTimer;
                ratio = Math.max(0, Math.min(1, ratio));

                if (ratio >= 0.95) {
                    return (float) (1 - 0.8 * (1 - ratio) / 0.05);
                } else if (ratio <= 0.05) {
                    return (float) (0.2 + 0.8 * ((0.05 - ratio) / 0.05));
                } else {
                    return 0.2F;
                }
            }
        }
        return alpha;
    }

    private boolean isHoldingJingubang(LocalPlayer player) {
        return isJingubang(player.getMainHandItem()) || isJingubang(player.getOffhandItem());
    }
    private boolean isJingubang(ItemStack item) {
        return item.getDescriptionId().equals("item.wukong.jingubang");
    }

}
