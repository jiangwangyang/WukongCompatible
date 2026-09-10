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

// 法术: 铜头铁臂技能
public class ShenfaTongtoutiebiSkill extends Skill {
    // 事件监听器注册用唯一UUID
    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0272ac114513");
    protected StaticAnimationProvider deriveAnimation1; // 第一衍生动画(施法/格挡)
    protected StaticAnimationProvider deriveAnimation2; // 第二衍生动画(格挡失败)

    // 创建技能构建器并设置分类与资源类型
    public static Builder create() {
        return new Builder()
                .setCategory(WukongSkillCategories.SHENFA_STYLE)
                .setResource(Resource.NONE);
    }

    // 构造铜头铁臂技能, 记录两段衍生动画
    public ShenfaTongtoutiebiSkill(Builder builder) {
        super(builder);
        deriveAnimation1 = builder.derive1;
        deriveAnimation2 = builder.derive2;
    }

    // 服务端执行: 冷却完毕时进入格挡架势并开启恢复/冷却计时, 否则提示冷却中
    // 参见 ShenfaTongtoutiebiSkill#updateContainer(SkillContainer)
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

    // 技能初始化: 注册受击事件监听, 实现无敌帧与格挡反弹跳星
    @Override
    public void onInitiate(SkillContainer container) {
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK,
                        EVENT_UUID,
                        (event) -> {
                            // 无敌帧期间: 本次伤害判定为未命中并取消事件
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
                                    // 恢复窗口内受击: 触发格挡反弹, 进入无敌帧并按当前棍式设置跳星标记
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

    // 技能移除(此处仅调用父类逻辑)
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
    }

    // 修改实体体力值(不允许低于0)
    public void modifyStamina(LivingEntity livingentity, float staminaChange) {
        float currentStamina = livingentity.getEntityData().get(STAMINA);
        float newStamina = Math.max(0.0F, currentStamina + staminaChange);
        livingentity.getEntityData().set(STAMINA, newStamina);
    }

    // 每帧更新: 递减无敌/恢复/冷却计时, 归零时复位对应状态
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

    // 根据技能状态绘制自定义技能图标与冷却显示
    // 完全重写Epic Fight默认绘制, 战斗模式HUD仅显示此自定义画面
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
        int alpha = 128; // 50% 透明度
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

    // 仅当装备合法武器时绘制该技能
    @Override
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }

    // 注册技能属性到动画(此处直接返回自身)
    @Override
    public Skill registerPropertiesToAnimation() {
        return this;
    }

    // 是否可执行的判定(此处直接调用父类逻辑)
    @Override
    public boolean canExecute(SkillContainer container) {
        return super.canExecute(container);
    }

    // 铜头铁臂技能构建器
    public static class Builder extends SkillBuilder<ShenfaTongtoutiebiSkill> {
        protected StaticAnimationProvider[] animationProviders; // 普通动画列表
        protected StaticAnimationProvider derive1; // 第一衍生动画(施法/格挡)
        protected StaticAnimationProvider derive2; // 第二衍生动画(格挡失败)

        public Builder() {}

        // 设置技能分类
        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        // 设置激活类型
        public Builder setActivateType(ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        // 设置技能资源类型
        public Builder setResource(Resource resource) {
            this.resource = resource;
            return this;
        }

        // 设置创造模式标签页
        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }

        // 设置普通动画
        public Builder setAnimations(StaticAnimationProvider... animationProviders) {
            this.animationProviders = animationProviders;
            return this;
        }

        // 设置衍生动画
        public Builder setDeriveAnimations(
                StaticAnimationProvider derive1, StaticAnimationProvider derive2) {
            this.derive1 = derive1;
            this.derive2 = derive2;
            return this;
        }
    }
}
