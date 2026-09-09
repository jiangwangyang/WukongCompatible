package com.p1nero.wukong.epicfight.skill.custom.fashu;

import static yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.STAMINA;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.WukongSkillCategories;
import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.wukong.StaffStance;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.skill.*;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.UUID;

/** 法术：铜头铁臂 */
public class ShenfaTongtoutiebiSkill extends Skill {
    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0272ac114513");
    protected StaticAnimationProvider deriveAnimation1;
    protected StaticAnimationProvider deriveAnimation2;

    public static Builder create() {
        return new Builder()
                .setCategory(WukongSkillCategories.SHENFA_STYLE)
                .setResource(Resource.NONE);
    }

    public ShenfaTongtoutiebiSkill(Builder builder) {
        super(builder);
        deriveAnimation1 = builder.derive1;
        deriveAnimation2 = builder.derive2;
    }

    /** {@link ShenfaTongtoutiebiSkill#updateContainer(SkillContainer)} */
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();
        if (dataManager.getDataValue(WukongSkillDataKeys.TTTB_COOLING_ATTACK.get())) {
            dataManager.setDataSync(WukongSkillDataKeys.TTTB_COOLING_ATTACK.get(), false);
            dataManager.setDataSync(WukongSkillDataKeys.TTTB_RESTORE_ZT.get(), true);
            dataManager.setDataSync(WukongSkillDataKeys.TTTB_RESTORE_TIMER.get(), 18);
            dataManager.setDataSync(WukongSkillDataKeys.TTTB_COOLING_TIMER.get(), 300);
            dataManager.setDataSync(WukongSkillDataKeys.TTTB_INVINCIBLE_TIMER.get(), 0);
            executer.playAnimationSynchronized(deriveAnimation1.get(), 0F);
        } else {
            player.sendSystemMessage(Component.literal("Tongtoutiebi is cooling down."));
        }
        super.executeOnServer(container, args);
    }

    @Override
    public void onInitiate(SkillContainer container) {
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK,
                        EVENT_UUID,
                        (event) -> {
                            ServerPlayerPatch serverPlayerPatch =
                                    ((ServerPlayerPatch) container.getExecutor());
                            if (container
                                            .getDataManager()
                                            .getDataValue(
                                                    WukongSkillDataKeys.TTTB_INVINCIBLE_TIMER.get())
                                    > 0) {
                                event.setResult(AttackResult.ResultType.MISSED);
                                event.setCanceled(true);
                                event.setCanceled(true);
                                return;
                            } else if (container
                                    .getDataManager()
                                    .getDataValue(WukongSkillDataKeys.TTTB_RESTORE_ZT.get())) {
                                container
                                        .getDataManager()
                                        .setDataSync(
                                                WukongSkillDataKeys.TTTB_RESTORE_ZT.get(), false);
                                if (container
                                                .getDataManager()
                                                .getDataValue(
                                                        WukongSkillDataKeys.TTTB_RESTORE_TIMER
                                                                .get())
                                        > 0) {
                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.TTTB_INVINCIBLE_TIMER.get(),
                                                    30);
                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.TTTB_RESTORE_TIMER.get(),
                                                    0);
                                    serverPlayerPatch.playSound(
                                            WuKongSounds.SHENFA_TTTB.get(), 1, 1);

                                    SkillContainer containe =
                                            serverPlayerPatch.getSkill(
                                                    WukongSkillSlots.STAFF_STYLE);
                                    if (containe != null
                                            && containe.getSkill() instanceof StaffStance style) {
                                        modifyStamina(event.getPlayerPatch().getOriginal(), 5.0F);
                                        SkillContainer weaponContainer =
                                                serverPlayerPatch.getSkill(
                                                        SkillSlots.WEAPON_INNATE);
                                        if (weaponContainer == null || weaponContainer.isEmpty()) {
                                            event.setResult(AttackResult.ResultType.MISSED);
                                            event.setCanceled(true);
                                            return;
                                        }
                                        if (style.getStyle(containe) == WukongStyles.SMASH) {
                                            weaponContainer
                                                    .getDataManager()
                                                    .setDataSync(
                                                            WukongSkillDataKeys.SMASH_FASHU_STACK
                                                                    .get(),
                                                            true);
                                        } else if (style.getStyle(containe)
                                                == WukongStyles.PILLAR) {
                                            weaponContainer
                                                    .getDataManager()
                                                    .setDataSync(
                                                            WukongSkillDataKeys.PILLAR_FASHU_STACK
                                                                    .get(),
                                                            true);
                                        } else if (style.getStyle(containe)
                                                == WukongStyles.THRUST) {
                                            weaponContainer
                                                    .getDataManager()
                                                    .setDataSync(
                                                            WukongSkillDataKeys.THRUST_FASHU_STACK
                                                                    .get(),
                                                            true);
                                        } else if (style.getStyle(containe)
                                                == WukongStyles.GREATSAGE) {
                                            weaponContainer
                                                    .getDataManager()
                                                    .setDataSync(
                                                            WukongSkillDataKeys
                                                                    .GREATSAGE_FASHU_STACK
                                                                    .get(),
                                                            true);
                                        }
                                    }

                                    event.setResult(AttackResult.ResultType.MISSED);
                                    event.setCanceled(true);
                                    return;
                                } else {
                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.TTTB_RESTORE_TIMER.get(),
                                                    0);
                                    event.getPlayerPatch()
                                            .playAnimationSynchronized(
                                                    deriveAnimation2.get(), 0.0F);
                                }
                            }
                        });

        super.onInitiate(container);
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
    }

    public void modifyStamina(LivingEntity livingentity, float staminaChange) {
        float currentStamina = livingentity.getEntityData().get(STAMINA);
        float newStamina = Math.max(0.0F, currentStamina + staminaChange);
        livingentity.getEntityData().set(STAMINA, newStamina);
    }

    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();
        if (container.getExecutor().isLogicalClient()) {

        } else {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();

            if (dataManager.getDataValue(WukongSkillDataKeys.TTTB_INVINCIBLE_TIMER.get()) != 0) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.TTTB_INVINCIBLE_TIMER.get(),
                        dataManager.getDataValue(WukongSkillDataKeys.TTTB_INVINCIBLE_TIMER.get())
                                - 1);
            }
            if (dataManager.getDataValue(WukongSkillDataKeys.TTTB_RESTORE_ZT.get())) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.TTTB_RESTORE_TIMER.get(),
                        dataManager.getDataValue(WukongSkillDataKeys.TTTB_RESTORE_TIMER.get()) - 1);
                if (dataManager.getDataValue(WukongSkillDataKeys.TTTB_RESTORE_TIMER.get()) == 0) {
                    container
                            .getDataManager()
                            .setDataSync(WukongSkillDataKeys.TTTB_RESTORE_ZT.get(), false);
                }
            }
            if (!dataManager.getDataValue(WukongSkillDataKeys.TTTB_COOLING_ATTACK.get())) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.TTTB_COOLING_TIMER.get(),
                        Math.max(
                                dataManager.getDataValue(
                                                WukongSkillDataKeys.TTTB_COOLING_TIMER.get())
                                        - 1,
                                0));
                if (dataManager.getDataValue(WukongSkillDataKeys.TTTB_COOLING_TIMER.get()) == 0)
                    dataManager.setDataSync(WukongSkillDataKeys.TTTB_COOLING_ATTACK.get(), true);
            }
        }
    }

    /** 根据技能状态绘制自定义技能图标与冷却显示 本方法完全重写 Epic Fight 默认的技能图标绘制, 战斗模式 HUD 仅显示此自定义画面 */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawOnGui(
            BattleModeGui gui,
            SkillContainer container,
            GuiGraphics guiGraphics,
            float x,
            float y,
            float partialTick) {
        Window sr = Minecraft.getInstance().getWindow();
        int width = sr.getGuiScaledWidth();
        int height = sr.getGuiScaledHeight();
        int alpha = 128;
        Vec2i pos = ClientConfig.getWeaponInnatePosition(width, height);
        ResourceLocation styleTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/gui/skills/spell_tttb.png");
        if (container
                .getDataManager()
                .getDataValue(WukongSkillDataKeys.TTTB_COOLING_ATTACK.get())) {
            alpha = 255;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // 使用默认的透明度混合模式
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        guiGraphics.blit(styleTexture, pos.x - 52, pos.y - 20, 20, 20, 0.0f, 0f, 1, 1, 1, 1);
        if (!container
                .getDataManager()
                .getDataValue(WukongSkillDataKeys.TTTB_COOLING_ATTACK.get())) {
            float second =
                    (container
                                    .getDataManager()
                                    .getDataValue(WukongSkillDataKeys.TTTB_COOLING_TIMER.get())
                            / 20.0F);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 255.0f);
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    String.format("%.1f", second),
                    pos.x
                            - 52
                            + (20
                                            - Minecraft.getInstance()
                                                    .font
                                                    .width(String.format("%.1f", second)))
                                    / 2,
                    pos.y - 20 + (20 - Minecraft.getInstance().font.lineHeight) / 2,
                    16777215);
        }
    }

    @Override
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }

    @Override
    public Skill registerPropertiesToAnimation() {
        return this;
    }

    @Override
    public boolean canExecute(SkillContainer container) {
        return super.canExecute(container);
    }

    // 构建器，用于创建技能实例
    public static class Builder extends SkillBuilder<ShenfaTongtoutiebiSkill> {
        protected StaticAnimationProvider[] animationProviders;
        protected StaticAnimationProvider derive1;
        protected StaticAnimationProvider derive2;

        public Builder() {}

        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        public Builder setActivateType(ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        public Builder setResource(Resource resource) {
            this.resource = resource;
            return this;
        }

        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }

        public Builder setAnimations(StaticAnimationProvider... animationProviders) {
            this.animationProviders = animationProviders;
            return this;
        }

        public Builder setDeriveAnimations(
                StaticAnimationProvider derive1, StaticAnimationProvider derive2) {
            this.derive1 = derive1;
            this.derive2 = derive2;
            return this;
        }
    }
}
