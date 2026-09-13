package com.p1nero.wukong.item.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.AnimationJudge;
import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.skill.custom.wukong.StaffStance;
import com.p1nero.wukong.item.JinGuBang;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

// 金箍棒物品渲染器: 根据当前棍式/蓄力状态/正在播放的动画动态切换贴图与渲染色调(发光变色效果)
public class JinGuBangRenderer extends GeoItemRenderer<JinGuBang> {
    // 当前金箍棒贴图档位索引(0~4), 发光/蓄力时随动画循环递增切换更亮的贴图
    public static int MAX_CZLT = 1;
    // 发光动画总帧数
    private static final int TOTAL_TEXTURES = 20;
    // 发光动画的内部帧计数器(循环0~TOTAL_TEXTURES-1)
    private int currentTextureIndex = 0;

    // 构造方法, 加载金箍棒模型
    public JinGuBangRenderer() {
        super(
                new DefaultedItemGeoModel<JinGuBang>(
                        ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "jingubang")));
    }

    // 根据状态选择贴图: 大圣棍式时使用专属贴图,
    // 蓄力或播放发光动画时驱动档位计数器循环递增并返回对应档位贴图
    @Override
    public ResourceLocation getTextureLocation(JinGuBang jinGuBang) {
        final Minecraft mc = Minecraft.getInstance();

        final ResourceLocation[] textures = {
            ResourceLocation.fromNamespaceAndPath(
                    WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang0.png"),
            ResourceLocation.fromNamespaceAndPath(
                    WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang1.png"),
            ResourceLocation.fromNamespaceAndPath(
                    WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang2.png"),
            ResourceLocation.fromNamespaceAndPath(
                    WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang3.png"),
            ResourceLocation.fromNamespaceAndPath(
                    WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang4.png"),
        };

        LocalPlayerPatch lpp =
                EpicFightCapabilities.getEntityPatch(mc.player, LocalPlayerPatch.class);
        if (lpp != null
                && (AnimationJudge.isCharging(lpp)
                                && lpp.getSkill(SkillSlots.WEAPON_INNATE).getStack() >= 1
                        || AnimationJudge.isGlow(
                                lpp.getAnimator().getPlayerFor(null).getAnimation()))) {
            currentTextureIndex = (currentTextureIndex + 1) % TOTAL_TEXTURES;
            if (currentTextureIndex > TOTAL_TEXTURES - 2) MAX_CZLT++;
            if (MAX_CZLT > 4) MAX_CZLT = 1;
        } else {
            MAX_CZLT = 0;
        }
        // GUI 渲染时玩家 patch 可能不存在, 需要判空
        SkillContainer containe = lpp == null ? null : lpp.getSkill(WukongSkillSlots.STAFF_STYLE);
        if (containe != null && containe.getSkill() instanceof StaffStance style) {
            if (style.getStyle(containe) == WukongStyles.GREATSAGE) {
                return ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang6.png");
            }
        }

        return textures[MAX_CZLT];
    }

    // 渲染前调整颜色与光照参数: 大圣棍式为半透明红橙自发光,
    // 蓄力或播放特定动画(发光/切手/二/三/四星)时分别调整色调与自发光亮度
    @Override
    public void actuallyRender(
            PoseStack poseStack,
            JinGuBang jinGuBang,
            BakedGeoModel model,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        final Minecraft mc = Minecraft.getInstance();
        LocalPlayerPatch lpp =
                EpicFightCapabilities.getEntityPatch(mc.player, LocalPlayerPatch.class);
        // GUI 渲染时玩家 patch 可能不存在, 需要判空
        SkillContainer containe = lpp == null ? null : lpp.getSkill(WukongSkillSlots.STAFF_STYLE);
        if (containe != null && containe.getSkill() instanceof StaffStance style) {
            // 大圣棍式: 红橙色调+半透明+全亮度自发光
            if (style.getStyle(containe) == WukongStyles.GREATSAGE) {
                red = 1.0f;
                green = 70f / 255f;
                blue = 30f / 255f;
                alpha = 0.5f;
                packedLight = 0xf000ff;
            }
        }

        // 蓄力或播放发光动画: 提升光照为自发光
        if (lpp != null
                && (AnimationJudge.isGlow(lpp.getAnimator().getPlayerFor(null).getAnimation())
                                && (lpp.getEntityState().getLevel() != 3)
                        || (AnimationJudge.isCharging(lpp)
                                && lpp.getSkill(SkillSlots.WEAPON_INNATE).getStack() >= 1)))
            packedLight = 0xf000ff;
        // 切手类动画: 偏白亮色调
        if (lpp != null
                && AnimationJudge.isQie(lpp.getAnimator().getPlayerFor(null).getAnimation())
                && (lpp.getEntityState().getLevel() != 3)) {
            green = 222f / 255f;
            blue = 200f / 255f;
            red = 1.0f;
        }
        // 二星蓄力: 金黄色调
        if (lpp != null
                && ((AnimationJudge.isTwo(lpp.getAnimator().getPlayerFor(null).getAnimation())
                                && (lpp.getEntityState().getLevel() != 3))
                        || (AnimationJudge.isCharging(lpp)
                                && lpp.getSkill(SkillSlots.WEAPON_INNATE).getStack() == 2))) {
            red = 1.0f;
            green = 215f / 255f;
            blue = 50f / 255f;
        }
        // 三星蓄力: 橙色调
        if (lpp != null
                && ((AnimationJudge.isThree(lpp.getAnimator().getPlayerFor(null).getAnimation())
                                && (lpp.getEntityState().getLevel() != 3))
                        || (AnimationJudge.isCharging(lpp)
                                && lpp.getSkill(SkillSlots.WEAPON_INNATE).getStack() == 3))) {
            green = 152f / 225f;
            blue = 24f / 225f;
            red = 1f;
        }
        // 四星蓄力: 红橙色调
        if (lpp != null
                && ((AnimationJudge.isFour(lpp.getAnimator().getPlayerFor(null).getAnimation())
                                && (lpp.getEntityState().getLevel() != 3))
                        || (AnimationJudge.isCharging(lpp)
                                && lpp.getSkill(SkillSlots.WEAPON_INNATE).getStack() == 4))) {
            red = 1.0f;
            green = 70f / 255f;
            blue = 30f / 255f;
        }
        super.actuallyRender(
                poseStack,
                jinGuBang,
                model,
                renderType,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                red,
                green,
                blue,
                alpha);
    }
}
