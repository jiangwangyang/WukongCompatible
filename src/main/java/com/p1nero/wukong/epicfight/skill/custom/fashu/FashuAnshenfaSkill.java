package com.p1nero.wukong.epicfight.skill.custom.fashu;

import yesman.epicfight.skill.SkillBuilder;


import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.WukongSkillCategories;
import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;

import com.p1nero.wukong.epicfight.skill.custom.wukong.ThrustHeavyAttack;
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
import org.jetbrains.annotations.NotNull;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.*;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.List;
import java.util.UUID;

/**
 * 法术：安身法
 */
public class FashuAnshenfaSkill extends Skill {

    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0142ac114510");
    private static final String ORIGIN_X = "wukong_anshen_origin_x";
    private static final String ORIGIN_Y = "wukong_anshen_origin_y";
    private static final String ORIGIN_Z = "wukong_anshen_origin_z";
    private static final int ACTIVE_TICKS = 510;
    private static final int WARMUP_TICKS = 15;
    private static final int PULSE_INTERVAL = 5;
    private static final double EFFECT_RADIUS = 4.7D;
    private static final int CIRCLE_PARTICLES = 48;

    protected StaticAnimationProvider deriveAnimation1;
    protected StaticAnimationProvider deriveAnimation2;

    public static Builder create() {
        return new Builder().setCategory(WukongSkillCategories.FASHU_STYLE).setResource(Resource.NONE);
    }


    public FashuAnshenfaSkill(Builder builder) {
        super(builder);
        deriveAnimation1 = builder.derive;

    }

    /**
     *  {@link FashuAnshenfaSkill#updateContainer(SkillContainer)}
     */
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        if (executer == null) {
            return;
        }
        //WukongMoveset.LOGGER.info("安身 {}", "executeOnServer");
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();

        if (dataManager.getDataValue(WukongSkillDataKeys.ASF_COOLING_ATTACK.get()) && dataManager.getDataValue(WukongSkillDataKeys.ASF_YINGSHEN_ZT.get())) {
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
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
    }
    @Override
    public void onRemoved(SkillContainer container) {
       /* PlayerPatch<?> executer = container.getExecutor();
        if (executer.getOriginal() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) container.getExecutor().getOriginal();
            SkillDataManager dataManager = container.getDataManager();
            dataManager.setDataSync(WukongSkillDataKeys.ASF_YINGSHEN_ZT.get(), false, player);
            dataManager.setDataSync(WukongSkillDataKeys.ASF_DERIVE_TIMER.get(), 0, player);
            dataManager.setDataSync(WukongSkillDataKeys.ASF_COOLING_TIMER.get(), 0, player);
            dataManager.setDataSync(WukongSkillDataKeys.ASF_COOLING_ATTACK.get(), false, player);
        }
        */
        PlayerEventListener listener = container.getExecutor().getEventListener();
        //listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID);
        super.onRemoved(container);
    }

    private void createFireCircle(ServerPlayer player, Vec3 position) {
        ServerLevel level = (ServerLevel) player.getCommandSenderWorld();
        for (int i = 0; i < CIRCLE_PARTICLES; i++) {
            double angle = i * (Math.PI * 2.0D / CIRCLE_PARTICLES);
            double x = position.x + EFFECT_RADIUS * Math.cos(angle);
            double z = position.z + EFFECT_RADIUS * Math.sin(angle);
            level.sendParticles(ParticleTypes.FLAME, x, position.y, z, 1, 0, 0, 0, 0);
        }
    }

    private void createRepelCircle(ServerPlayer player, Vec3 position) {
        double knockbackStrength = 0.24D;
        AABB area = new AABB(position, position).inflate(EFFECT_RADIUS);
        List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != player && entity.isAlive() && entity instanceof Monster
        );
        for (LivingEntity entity : nearbyEntities) {
            Vec3 offset = entity.position().subtract(position);
            double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            if (horizontalDistance <= EFFECT_RADIUS && horizontalDistance > 1.0E-4D) {
                entity.setSecondsOnFire(1);
                entity.push(
                        offset.x / horizontalDistance * knockbackStrength,
                        0.05D,
                        offset.z / horizontalDistance * knockbackStrength
                );
            }
        }
    }

    private void restoreHealthAndFocus(ServerPlayer player, Vec3 position, ServerPlayerPatch serverPlayerPatch) {
        if (player.distanceToSqr(position) <= EFFECT_RADIUS * EFFECT_RADIUS) {
            SkillContainer weaponContainer = serverPlayerPatch.getSkill(SkillSlots.WEAPON_INNATE);
            if (weaponContainer != null && !weaponContainer.isEmpty() && weaponContainer.getSkill() != null) {
                Skill weaponSkill = weaponContainer.getSkill();
                weaponSkill.setConsumptionSynchronize(
                        weaponContainer,
                        weaponContainer.getResource() + Config.CHARGING_SPEED.get().floatValue() * PULSE_INTERVAL
                );
            }
            player.heal(0.2F * PULSE_INTERVAL);
        }
    }
    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();
        if (!container.getExecutor().isLogicalClient()) {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();
            if (!dataManager.getDataValue(WukongSkillDataKeys.ASF_YINGSHEN_ZT.get())) {
                int remaining = Math.max(dataManager.getDataValue(WukongSkillDataKeys.ASF_DERIVE_TIMER.get()) - 1, 0);
                dataManager.setDataSync(WukongSkillDataKeys.ASF_DERIVE_TIMER.get(), remaining);
                if (remaining <= ACTIVE_TICKS - WARMUP_TICKS && remaining % PULSE_INTERVAL == 0) {
                    Vec3 origin = new Vec3(
                            serverPlayer.getPersistentData().getDouble(ORIGIN_X),
                            serverPlayer.getPersistentData().getDouble(ORIGIN_Y),
                            serverPlayer.getPersistentData().getDouble(ORIGIN_Z)
                    );
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
                int cooldown = Math.max(dataManager.getDataValue(WukongSkillDataKeys.ASF_COOLING_TIMER.get()) - 1, 0);
                dataManager.setDataSync(WukongSkillDataKeys.ASF_COOLING_TIMER.get(), cooldown);
                if (cooldown == 0) {
                    dataManager.setDataSync(WukongSkillDataKeys.ASF_COOLING_ATTACK.get(), true);
                }
            }
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
    /**
     * 根据技能状态绘制自定义技能图标与冷却显示
     * 本方法完全重写 Epic Fight 默认的技能图标绘制, 战斗模式 HUD 仅显示此自定义画面
     */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
        Window sr = Minecraft.getInstance().getWindow();
        int width = sr.getGuiScaledWidth();
        int height = sr.getGuiScaledHeight();
        int alpha = 128; // 50% 透明度
        Vec2i pos = ClientConfig.getWeaponInnatePosition(width, height);
        ResourceLocation styleTexture = ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "textures/gui/skills/spell_asf.png");
        if (container.getDataManager().getDataValue(WukongSkillDataKeys.ASF_COOLING_ATTACK.get()) && container.getDataManager().getDataValue(WukongSkillDataKeys.ASF_YINGSHEN_ZT.get())) {
            alpha = 255;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        guiGraphics.blit(styleTexture, pos.x - 32, pos.y -20, 20, 20, 0.0f, 0f, 1, 1, 1, 1);
        if (!container.getDataManager().getDataValue(WukongSkillDataKeys.ASF_COOLING_ATTACK.get()) ) {
            float second = (container.getDataManager().getDataValue(WukongSkillDataKeys.ASF_COOLING_TIMER.get()) / 20.0F);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 255.0f);
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    String.format("%.1f", second),
                    pos.x - 32 + (20 -  Minecraft.getInstance().font.width(String.format("%.1f", second))) / 2,pos.y - 20 + (20 -  Minecraft.getInstance().font.lineHeight) / 2,
                    16777215
            );
        }
    }


    public static class Builder extends SkillBuilder<FashuAnshenfaSkill> {
        protected StaticAnimationProvider[] animationProviders;
        protected StaticAnimationProvider derive;
        public Builder() {
        }

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
        public Builder setDeriveAnimations(StaticAnimationProvider derive) {
            this.derive = derive;
            return this;
        }

    }
}
