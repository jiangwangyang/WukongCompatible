package com.p1nero.wukong.epicfight.skill.custom.fashu;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.WukongSkillCategories;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.skill.*;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import java.util.List;

// 法术: 安身法(地面法阵)技能
public class FashuAnshenfaSkill extends Skill {

    private static final String ORIGIN_X = "wukong_anshen_origin_x"; // 施法原点X坐标持久化键名
    private static final String ORIGIN_Y = "wukong_anshen_origin_y"; // 施法原点Y坐标持久化键名
    private static final String ORIGIN_Z = "wukong_anshen_origin_z"; // 施法原点Z坐标持久化键名
    private static final int ACTIVE_TICKS = 510; // 法阵生效总时长(tick)
    private static final int WARMUP_TICKS = 15; // 开始周期脉冲前的预热时长(tick)
    private static final int PULSE_INTERVAL = 5; // 脉冲效果触发间隔(tick)
    private static final double EFFECT_RADIUS = 4.7D; // 法阵作用半径
    private static final int CIRCLE_PARTICLES = 48; // 火圈粒子数量

    protected StaticAnimationProvider deriveAnimation1; // 记录的衍生(施法)动画

    // 创建技能构建器并设置分类与资源类型
    public static Builder create() {
        return new Builder()
                .setCategory(WukongSkillCategories.FASHU_STYLE)
                .setResource(Resource.NONE);
    }

    // 构造安身法技能, 记录衍生(施法)动画
    public FashuAnshenfaSkill(Builder builder) {
        super(builder);
        deriveAnimation1 = builder.derive;
    }

    // 服务端执行: 冷却且可用时播放衍生(施法)动画, 记录施法原点并开启生效/冷却计时
    // 参见 FashuAnshenfaSkill#updateContainer(SkillContainer)
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        if (executer == null) {
            return;
        }
        // WukongMoveset.LOGGER.info("安身 {}", "executeOnServer");
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();

        if (dataManager.getDataValue(WukongSkillDataKeys.ASF_COOLING_ATTACK.get())
                && dataManager.getDataValue(WukongSkillDataKeys.ASF_YINGSHEN_ZT.get())) {
            executer.playSound(WuKongSounds.FASHU_ASS.get(), 0.0F, 0.0F);
            executer.playAnimationSynchronized(deriveAnimation1.get(), 0F);
            dataManager.setDataSync(WukongSkillDataKeys.ASF_YINGSHEN_ZT.get(), false);
            dataManager.setDataSync(WukongSkillDataKeys.ASF_DERIVE_TIMER.get(), ACTIVE_TICKS);
            dataManager.setDataSync(WukongSkillDataKeys.ASF_COOLING_TIMER.get(), 1000);
            dataManager.setDataSync(WukongSkillDataKeys.ASF_COOLING_ATTACK.get(), false);
            Vec3 origin = player.position();
            player.getPersistentData().putDouble(ORIGIN_X, origin.x);
            player.getPersistentData().putDouble(ORIGIN_Y, origin.y);
            player.getPersistentData().putDouble(ORIGIN_Z, origin.z);
        } else {
            player.sendSystemMessage(Component.literal("Anshenfa is cooling down."));
        }

        super.executeOnServer(container, args);
    }

    @Override
    // 技能初始化(此处仅调用父类逻辑)
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
    }

    @Override
    // 技能移除(此处仅调用父类逻辑)
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
    }

    // 在玩家周围按半径生成一圈火焰粒子
    private void createFireCircle(ServerPlayer player, Vec3 position) {
        ServerLevel level = (ServerLevel) player.getCommandSenderWorld();
        for (int i = 0; i < CIRCLE_PARTICLES; i++) {
            double angle = i * (Math.PI * 2.0D / CIRCLE_PARTICLES);
            double x = position.x + EFFECT_RADIUS * Math.cos(angle);
            double z = position.z + EFFECT_RADIUS * Math.sin(angle);
            level.sendParticles(ParticleTypes.FLAME, x, position.y, z, 1, 0, 0, 0, 0);
        }
    }

    // 在生效范围内将怪物点燃并沿径向击退
    private void createRepelCircle(ServerPlayer player, Vec3 position) {
        double knockbackStrength = 0.24D;
        AABB area = new AABB(position, position).inflate(EFFECT_RADIUS);
        List<LivingEntity> nearbyEntities =
                player.level()
                        .getEntitiesOfClass(
                                LivingEntity.class,
                                area,
                                entity ->
                                        entity != player
                                                && entity.isAlive()
                                                && entity instanceof Monster);
        for (LivingEntity entity : nearbyEntities) {
            Vec3 offset = entity.position().subtract(position);
            double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            if (horizontalDistance <= EFFECT_RADIUS && horizontalDistance > 1.0E-4D) {
                entity.setSecondsOnFire(1);
                entity.push(
                        offset.x / horizontalDistance * knockbackStrength,
                        0.05D,
                        offset.z / horizontalDistance * knockbackStrength);
            }
        }
    }

    // 玩家处于法阵内时回复武器技能资源并治疗
    private void restoreHealthAndFocus(
            ServerPlayer player, Vec3 position, ServerPlayerPatch serverPlayerPatch) {
        if (player.distanceToSqr(position) <= EFFECT_RADIUS * EFFECT_RADIUS) {
            SkillContainer weaponContainer = serverPlayerPatch.getSkill(SkillSlots.WEAPON_INNATE);
            if (weaponContainer != null
                    && !weaponContainer.isEmpty()
                    && weaponContainer.getSkill() != null) {
                Skill weaponSkill = weaponContainer.getSkill();
                weaponSkill.setConsumptionSynchronize(
                        weaponContainer,
                        weaponContainer.getResource()
                                + Config.CHARGING_SPEED.get().floatValue() * PULSE_INTERVAL);
            }
            player.heal(0.2F * PULSE_INTERVAL);
        }
    }

    @Override
    // 每帧更新: 递减生效/冷却计时, 生效期间周期生成火圈/击退/回血, 结束时复位状态
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();
        if (!container.getExecutor().isLogicalClient()) {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();
            if (!dataManager.getDataValue(WukongSkillDataKeys.ASF_YINGSHEN_ZT.get())) {
                int remaining =
                        Math.max(
                                dataManager.getDataValue(WukongSkillDataKeys.ASF_DERIVE_TIMER.get())
                                        - 1,
                                0);
                dataManager.setDataSync(WukongSkillDataKeys.ASF_DERIVE_TIMER.get(), remaining);
                if (remaining <= ACTIVE_TICKS - WARMUP_TICKS && remaining % PULSE_INTERVAL == 0) {
                    Vec3 origin =
                            new Vec3(
                                    serverPlayer.getPersistentData().getDouble(ORIGIN_X),
                                    serverPlayer.getPersistentData().getDouble(ORIGIN_Y),
                                    serverPlayer.getPersistentData().getDouble(ORIGIN_Z));
                    createFireCircle(serverPlayer, origin);
                    createRepelCircle(serverPlayer, origin);
                    restoreHealthAndFocus(serverPlayer, origin, serverPlayerPatch);
                }
                if (remaining == 0) {
                    dataManager.setDataSync(WukongSkillDataKeys.ASF_YINGSHEN_ZT.get(), true);
                    serverPlayer.getPersistentData().remove(ORIGIN_X);
                    serverPlayer.getPersistentData().remove(ORIGIN_Y);
                    serverPlayer.getPersistentData().remove(ORIGIN_Z);
                }
            }

            if (!dataManager.getDataValue(WukongSkillDataKeys.ASF_COOLING_ATTACK.get())) {
                int cooldown =
                        Math.max(
                                dataManager.getDataValue(
                                                WukongSkillDataKeys.ASF_COOLING_TIMER.get())
                                        - 1,
                                0);
                dataManager.setDataSync(WukongSkillDataKeys.ASF_COOLING_TIMER.get(), cooldown);
                if (cooldown == 0) {
                    dataManager.setDataSync(WukongSkillDataKeys.ASF_COOLING_ATTACK.get(), true);
                }
            }
        }
    }

    @Override
    // 仅当装备合法武器时绘制该技能
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }

    @Override
    // 注册技能属性到动画(此处直接返回自身)
    public Skill registerPropertiesToAnimation() {
        return this;
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
                        WukongMoveset.MOD_ID, "textures/gui/skills/spell_asf.png");
        if (container.getDataManager().getDataValue(WukongSkillDataKeys.ASF_COOLING_ATTACK.get())
                && container
                        .getDataManager()
                        .getDataValue(WukongSkillDataKeys.ASF_YINGSHEN_ZT.get())) {
            alpha = 255;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        guiGraphics.blit(styleTexture, pos.x - 32, pos.y - 20, 20, 20, 0.0f, 0f, 1, 1, 1, 1);
        if (!container
                .getDataManager()
                .getDataValue(WukongSkillDataKeys.ASF_COOLING_ATTACK.get())) {
            float second =
                    (container
                                    .getDataManager()
                                    .getDataValue(WukongSkillDataKeys.ASF_COOLING_TIMER.get())
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

    // 安身法技能构建器
    public static class Builder extends SkillBuilder<FashuAnshenfaSkill> {
        protected StaticAnimationProvider[] animationProviders; // 普通动画列表
        protected StaticAnimationProvider derive; // 衍生(施法)动画

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

        // 设置衍生(施法)动画
        public Builder setDeriveAnimations(StaticAnimationProvider derive) {
            this.derive = derive;
            return this;
        }
    }
}
