package com.p1nero.wukong.mixin;

import com.p1nero.wukong.epicfight.skill.custom.wukong.GreatSageHeavyAttack;
import com.p1nero.wukong.epicfight.skill.custom.wukong.PillarHeavyAttack;
import com.p1nero.wukong.epicfight.skill.custom.wukong.SmashHeavyAttack;
import com.p1nero.wukong.epicfight.skill.custom.wukong.ThrustHeavyAttack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;

@Mixin(value = BattleModeGui.class, remap = false)
public class BattleModeGuiMixin {

    /**
     * 取消绘制技能图标，我要自己画！
     */
    @Inject(method = "renderWeaponInnateSkill", at = @At(value = "HEAD"), cancellable = true)
    private void modifyTexture(ForgeGui gui, GuiGraphics guiGraphics, float partialTicks, int screenWidth, int screenHeight, CallbackInfo ci) {
        if (ClientEngine.getInstance().getPlayerPatch() == null) {
            return;
        }

        SkillContainer container = ClientEngine.getInstance().getPlayerPatch().getSkill(SkillSlots.WEAPON_INNATE);
        if (container == null || container.isEmpty()) {
            return;
        }

        Skill skill = container.getSkill();
        ResourceLocation registryName = skill.getRegistryName();
        if (skill instanceof GreatSageHeavyAttack || skill instanceof ThrustHeavyAttack || skill instanceof PillarHeavyAttack || skill instanceof SmashHeavyAttack || registryName != null && registryName.getPath().equals("common")) {
            ci.cancel();
        }
    }

}
