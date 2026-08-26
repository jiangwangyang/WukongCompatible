package com.p1nero.wukong.item.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.AnimationJudge;
import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.WukongSkills;
import com.p1nero.wukong.epicfight.skill.custom.wukong.StaffStance;
import com.p1nero.wukong.item.JinGuBang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.entity.EpicFightEntities;


public class JinGuBangRenderer extends GeoItemRenderer<JinGuBang> {
    public static int MAX_CZLT = 1;
    private int currentTextureIndex = 0;
    private static final int TOTAL_TEXTURES = 20;
    public JinGuBangRenderer() {
        super(new DefaultedItemGeoModel<JinGuBang>(new ResourceLocation(WukongMoveset.MOD_ID, "jingubang")));
    }


    @Override
    public ResourceLocation getTextureLocation(JinGuBang jinGuBang) {
        final Minecraft mc = Minecraft.getInstance();

        final ResourceLocation[] textures = {
                new ResourceLocation(WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang0.png"),
                new ResourceLocation(WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang1.png"),
                new ResourceLocation(WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang2.png"),
                new ResourceLocation(WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang3.png"),
                new ResourceLocation(WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang4.png"),
        };

        LocalPlayerPatch lpp = EpicFightCapabilities.getEntityPatch(mc.player, LocalPlayerPatch.class);
        if (lpp != null && (AnimationJudge.isCharging(lpp) && lpp.getSkill(SkillSlots.WEAPON_INNATE).getStack() >= 1  || (lpp.getAnimator().getPlayerFor(null).getAnimation() instanceof StaticAnimation staticAnimation && AnimationJudge.isGlow(staticAnimation)))) {
            currentTextureIndex = (currentTextureIndex + 1) % TOTAL_TEXTURES;
            if (currentTextureIndex>TOTAL_TEXTURES-2)MAX_CZLT++;
            if (MAX_CZLT>4)MAX_CZLT=1;
        } else {
            MAX_CZLT = 0;
        }
        SkillContainer containe = lpp.getSkill(WukongSkillSlots.STAFF_STYLE);
        if (containe != null && containe.getSkill() instanceof StaffStance style) {
            if ( style.getStyle(containe) ==  WukongStyles.GREATSAGE){
                return  new ResourceLocation(WukongMoveset.MOD_ID, "textures/item/jingubang/jingubang6.png");
            }
        }


        return textures[MAX_CZLT];

    }


    @Override
    public void preRender(PoseStack poseStack, JinGuBang jinGuBang, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, jinGuBang, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, JinGuBang jinGuBang, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
       // RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);

        final Minecraft mc = Minecraft.getInstance();
        LocalPlayerPatch lpp = EpicFightCapabilities.getEntityPatch(mc.player, LocalPlayerPatch.class);
        SkillContainer containe = EpicFightCapabilities.getEntityPatch(mc.player, LocalPlayerPatch.class).getSkill(WukongSkillSlots.STAFF_STYLE);
        if (containe != null && containe.getSkill() instanceof StaffStance style) {
            if ( style.getStyle(containe) ==  WukongStyles.GREATSAGE){
                red = 1.0f;green = 70f / 255f;blue = 30f / 255f;alpha = 0.5f;packedLight = 0xf000ff;
            }
        }


        if (lpp != null && ((lpp.getAnimator().getPlayerFor(null).getAnimation() instanceof StaticAnimation staticAnimation && AnimationJudge.isGlow(staticAnimation)) && (lpp.getEntityState().getLevel() != 3) || (AnimationJudge.isCharging(lpp) && lpp.getSkill(SkillSlots.WEAPON_INNATE).getStack() >= 1)))
            packedLight = 0xf000ff;
        if (lpp != null && (lpp.getAnimator().getPlayerFor(null).getAnimation() instanceof StaticAnimation staticAnimation && AnimationJudge.isQie(staticAnimation) && (lpp.getEntityState().getLevel() != 3))) {
            green = 222f/255f;
            blue = 200f/255f;
            red = 1.0f;
        }
        if (lpp != null && ((lpp.getAnimator().getPlayerFor(null).getAnimation() instanceof StaticAnimation staticAnimation && AnimationJudge.isTwo(staticAnimation) && (lpp.getEntityState().getLevel() != 3))
                || (AnimationJudge.isCharging(lpp) && lpp.getSkill(SkillSlots.WEAPON_INNATE).getStack() == 2))) {
            red = 1.0f;
            green = 215f / 255f;
            blue = 50f / 255f;

        }
        if (lpp != null && ((lpp.getAnimator().getPlayerFor(null).getAnimation() instanceof StaticAnimation staticAnimation && AnimationJudge.isThree(staticAnimation) && (lpp.getEntityState().getLevel() != 3))
                || (AnimationJudge.isCharging(lpp) && lpp.getSkill(SkillSlots.WEAPON_INNATE).getStack() == 3))) {
            green = 152f/225f;
            blue = 24f/225f;
            red = 1f;
        }
        if (lpp != null && ((lpp.getAnimator().getPlayerFor(null).getAnimation() instanceof StaticAnimation staticAnimation && AnimationJudge.isFour(staticAnimation) && (lpp.getEntityState().getLevel() != 3))
                || (AnimationJudge.isCharging(lpp) && lpp.getSkill(SkillSlots.WEAPON_INNATE).getStack() == 4))) {
        red = 1.0f;
        green = 70f / 255f;
        blue = 30f / 255f;
        }
        super.actuallyRender(poseStack, jinGuBang, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);


    }
}
