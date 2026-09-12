package com.p1nero.wukong.epicfight.skill.custom.fashu;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.client.particle.WuKongEffect;
import com.p1nero.wukong.epicfight.WukongSkillCategories;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import com.p1nero.wukong.epicfight.skill.EntitySpeedData;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.skill.*;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import java.util.List;

// 法术: 定身术技能
public class FashuDingshenfaSkill extends Skill {

    private static final String TRACKED_TARGET = "wukong_dingshen_target"; // 锁定目标UUID持久化键名
    private static final double FALLBACK_SEARCH_RADIUS = 64.0D; // 解除定身时未锁定目标, 按此半径搜索范围内被定身实体
    private static final int PARTICLE_INTERVAL = 5; // 定身粒子播放的帧间隔
    protected StaticAnimationProvider deriveAnimation1; // 记录的衍生(施法)动画

    // 创建技能构建器并设置分类与资源类型
    public static Builder create() {
        return new Builder()
                .setCategory(WukongSkillCategories.FASHU_STYLE)
                .setResource(Resource.NONE);
    }

    // 构造定身术技能, 记录衍生(施法)动画
    public FashuDingshenfaSkill(Builder builder) {
        super(builder);
        deriveAnimation1 = builder.derive1;
    }

    // 服务端执行: 冷却完毕时播放衍生(施法)动画, 否则提示冷却中
    // 参见 FashuDingshenfaSkill#updateContainer(SkillContainer)
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        if (executer == null) {
            return;
        }
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();
        if (dataManager.getDataValue(WukongSkillDataKeys.DSF_COOLING_ATTACK.get())) {
            executer.playSound(WuKongSounds.FASHU_DSS.get(), 0.0F, 0.0F);
            executer.playAnimationSynchronized(deriveAnimation1.get(), 0F);

        } else {
            player.sendSystemMessage(Component.literal("Dingshenfa state updated."));
        }

        super.executeOnServer(container, args);
    }

    @Override
    // 技能初始化(此处仅调用父类逻辑)
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
    }

    // 记录被定身术锁定的目标实体UUID
    public static void trackTarget(ServerPlayer player, LivingEntity target) {
        player.getPersistentData().putUUID(TRACKED_TARGET, target.getUUID());
    }

    // 读取并返回当前锁定的目标实体(已失效则返回null)
    private LivingEntity getTrackedTarget(ServerPlayer player) {
        if (!player.getPersistentData().hasUUID(TRACKED_TARGET)) {
            return null;
        }
        if (player.serverLevel().getEntity(player.getPersistentData().getUUID(TRACKED_TARGET))
                instanceof LivingEntity target) {
            return target;
        }
        return null;
    }

    // 向锁定的目标播放定身粒子效果
    private void showDingParticles(ServerPlayer player) {
        LivingEntity target = getTrackedTarget(player);
        if (target != null && target.isAlive() && target.getTags().contains("ding")) {
            player.serverLevel()
                    .sendParticles(
                            ParticleTypes.WAX_OFF,
                            target.getX(),
                            target.getY() + target.getBbHeight() * 0.5D,
                            target.getZ(),
                            3,
                            0.15D,
                            0.25D,
                            0.15D,
                            0.02D);
        }
    }

    // 解除定身: 释放锁定目标, 或解除范围内所有被定身实体
    private void liftDing(ServerPlayer player) {
        LivingEntity trackedTarget = getTrackedTarget(player);
        if (trackedTarget != null) {
            releaseTarget(player, trackedTarget);
        } else {
            List<LivingEntity> nearbyEntities =
                    player.level()
                            .getEntitiesOfClass(
                                    LivingEntity.class,
                                    player.getBoundingBox().inflate(FALLBACK_SEARCH_RADIUS),
                                    entity ->
                                            entity.isAlive() && entity.getTags().contains("ding"));
            nearbyEntities.forEach(entity -> releaseTarget(player, entity));
        }
        player.getPersistentData().remove(TRACKED_TARGET);
    }

    // 解除单个实体的定身效果并恢复状态(末影龙/怪物恢复AI与仇恨)
    private void releaseTarget(ServerPlayer player, LivingEntity entity) {
        EntitySpeedData.restoreOriginalSpeed(entity);
        entity.removeTag("ding");
        entity.removeEffect(WuKongEffect.DING.get());
        entity.removeEffect(MobEffects.GLOWING);
        if (entity instanceof EnderDragon enderDragon) {
            enderDragon.setNoAi(false);
            enderDragon.setAggressive(true);
        } else if (entity instanceof Monster monster) {
            monster.setNoAi(false);
            monster.setAggressive(true);
            monster.setTarget(player);
        }
    }

    @Override
    // 技能移除时解除定身效果(客户端跳过)
    public void onRemoved(SkillContainer container) {
        PlayerPatch<?> executer = container.getExecutor();
        if (!executer.isLogicalClient() && executer.getOriginal() instanceof ServerPlayer player) {
            liftDing(player);
        }
        super.onRemoved(container);
    }

    @Override
    // 每帧更新: 递减定身/冷却计时, 生效期间周期播放粒子, 倒计时归零则解除定身
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();

        if (container.getExecutor().isLogicalClient()) {
            // 客户端执行的逻辑
        } else {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();
            if (dataManager.getDataValue(WukongSkillDataKeys.DSF_YINGSHEN_ZT.get())) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.DSF_DERIVE_TIMER.get(),
                        Math.max(
                                dataManager.getDataValue(WukongSkillDataKeys.DSF_DERIVE_TIMER.get())
                                        - 1,
                                0));
                if (dataManager.getDataValue(WukongSkillDataKeys.DSF_DERIVE_TIMER.get()) == 0) {
                    dataManager.setDataSync(WukongSkillDataKeys.DSF_YINGSHEN_ZT.get(), false);
                    liftDing(serverPlayer);
                }
                if (dataManager.getDataValue(WukongSkillDataKeys.DSF_DERIVE_TIMER.get())
                                % PARTICLE_INTERVAL
                        == 0) {
                    showDingParticles(serverPlayer);
                }
            }
            if (!dataManager.getDataValue(WukongSkillDataKeys.DSF_COOLING_ATTACK.get())) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.DSF_COOLING_TIMER.get(),
                        Math.max(
                                dataManager.getDataValue(
                                                WukongSkillDataKeys.DSF_COOLING_TIMER.get())
                                        - 1,
                                0));
                if (dataManager.getDataValue(WukongSkillDataKeys.DSF_COOLING_TIMER.get()) == 0) {
                    dataManager.setDataSync(WukongSkillDataKeys.DSF_COOLING_ATTACK.get(), true);
                }
            }
        }
    }

    // 根据技能状态绘制自定义技能图标与冷却显示; 完全重写Epic Fight默认绘制, 战斗模式HUD仅显示此自定义画面
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
                        WukongMoveset.MOD_ID, "textures/gui/skills/spell_dsf.png");
        if (container.getDataManager().getDataValue(WukongSkillDataKeys.DSF_COOLING_ATTACK.get())) {
            alpha = 255;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // 使用默认的透明度混合模式
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        guiGraphics.blit(styleTexture, pos.x - 32, pos.y - 20, 20, 20, 0.0f, 0f, 1, 1, 1, 1);
        if (!container
                .getDataManager()
                .getDataValue(WukongSkillDataKeys.DSF_COOLING_ATTACK.get())) {
            float second =
                    (container
                                    .getDataManager()
                                    .getDataValue(WukongSkillDataKeys.DSF_COOLING_TIMER.get())
                            / 20.0F);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 255.0f);
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    String.format("%.1f", second),
                    pos.x
                            - 32
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
    // 仅当装备合法武器时绘制该技能
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }

    @Override
    // 是否可执行的判定(此处直接调用父类逻辑)
    public boolean canExecute(SkillContainer container) {
        return super.canExecute(container);
    }

    // 定身术技能构建器
    public static class Builder extends SkillBuilder<FashuDingshenfaSkill> {
        protected StaticAnimationProvider derive1; // 第一衍生(施法)动画
        protected StaticAnimationProvider derive2; // 第二衍生动画(预留)

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

        // 设置第一衍生(施法)动画
        public Builder setDeriveAnimations(StaticAnimationProvider derive1) {
            this.derive1 = derive1;
            return this;
        }
    }
}
