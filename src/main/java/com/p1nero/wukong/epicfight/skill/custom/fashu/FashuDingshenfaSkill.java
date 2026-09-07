package com.p1nero.wukong.epicfight.skill.custom.fashu;

import yesman.epicfight.skill.SkillBuilder;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.WukongSkillCategories;
import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.skill.EntitySpeedData;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.client.particle.WuKongEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
 * 娉曟湳锛氬畾韬湳
 */

public class FashuDingshenfaSkill extends Skill {

    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0442ac114510");
    private static final String TRACKED_TARGET = "wukong_dingshen_target";
    private static final double FALLBACK_SEARCH_RADIUS = 64.0D;
    private static final int PARTICLE_INTERVAL = 5;
    protected StaticAnimationProvider deriveAnimation1;

    public static Builder create() {
        return new Builder().setCategory(WukongSkillCategories.FASHU_STYLE).setResource(Resource.NONE);
    }

    public FashuDingshenfaSkill(Builder builder) {
        super(builder);
        deriveAnimation1 = builder.derive1;

    }

    /**
     *  {@link FashuDingshenfaSkill#updateContainer(SkillContainer)}
     */
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        if (executer == null) {
            return;
        }
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();
        if(dataManager.getDataValue(WukongSkillDataKeys.DSF_COOLING_ATTACK.get()) ){
            executer.playSound(WuKongSounds.FASHU_DSS.get(), 0.0F, 0.0F);
            executer.playAnimationSynchronized(deriveAnimation1.get(), 0F);

        }else{
            player.sendSystemMessage(Component.literal("Dingshenfa state updated."));
        }

        super.executeOnServer(container, args);
    }


    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
//        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.ANIMATION_BEGIN_EVENT, EVENT_UUID, (event -> {
//            if (event.getAnimation().equals(WukongAnimations.FASHU_MAGICARTS_DSF_START)) {
//                if (container.getDataManager().getDataValue(WukongSkillDataKeys.DSF_ENEMY_ATTACK.get())) {
//                    container.getDataManager().setData(WukongSkillDataKeys.DSF_ENEMY_ATTACK.get(), false);
//                    if (container.getExecutor() instanceof ServerPlayerPatch) {
//                        ServerPlayerPatch serverPlayerPatch = (ServerPlayerPatch) container.getExecutor();
//                        Dingshenshu_traverse(serverPlayerPatch.getOriginal(),container);;
//                        container.getDataManager().setData(WukongSkillDataKeys.DSF_COOLING_ATTACK.get(), false);
//                    }
//                }
//            }
//        }));

    }

    public static void trackTarget(ServerPlayer player, LivingEntity target) {
        player.getPersistentData().putUUID(TRACKED_TARGET, target.getUUID());
    }

    private LivingEntity getTrackedTarget(ServerPlayer player) {
        if (!player.getPersistentData().hasUUID(TRACKED_TARGET)) {
            return null;
        }
        if (player.serverLevel().getEntity(player.getPersistentData().getUUID(TRACKED_TARGET)) instanceof LivingEntity target) {
            return target;
        }
        return null;
    }

    private void showDingParticles(ServerPlayer player) {
        LivingEntity target = getTrackedTarget(player);
        if (target != null && target.isAlive() && target.getTags().contains("ding")) {
            player.serverLevel().sendParticles(
                    ParticleTypes.WAX_OFF,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(),
                    3,
                    0.15D,
                    0.25D,
                    0.15D,
                    0.02D
            );
        }
    }

    private void liftDing(ServerPlayer player) {
        LivingEntity trackedTarget = getTrackedTarget(player);
        if (trackedTarget != null) {
            releaseTarget(player, trackedTarget);
        } else {
            List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(FALLBACK_SEARCH_RADIUS),
                    entity -> entity.isAlive() && entity.getTags().contains("ding")
            );
            nearbyEntities.forEach(entity -> releaseTarget(player, entity));
        }
        player.getPersistentData().remove(TRACKED_TARGET);
    }

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
    public void onRemoved(SkillContainer container) {
        PlayerPatch<?> executer = container.getExecutor();
        if (!executer.isLogicalClient() && executer.getOriginal() instanceof ServerPlayer player) {
            liftDing(player);
        }
        /*if (executer.getOriginal() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) container.getExecutor().getOriginal();
            SkillDataManager dataManager = container.getDataManager();
            // 閲嶇疆瀹氳韩鐘舵€?
            dataManager.setDataSync(WukongSkillDataKeys.DSF_YINGSHEN_ZT.get(), false, player);
            // 閲嶇疆鍐峰嵈鐘舵€?
            dataManager.setDataSync(WukongSkillDataKeys.DSF_COOLING_ATTACK.get(), true, player);
            dataManager.setDataSync(WukongSkillDataKeys.DSF_COOLING_TIMER.get(), 0, player);
            dataManager.setDataSync(WukongSkillDataKeys.DSF_DERIVE_TIMER.get(), 0, player);
            // 瑙ｉ櫎瀹氳韩
            Dingshenshu_lift(player);
        }*/

        PlayerEventListener listener = container.getExecutor().getEventListener();
        //listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID);
        super.onRemoved(container);
    }



    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();

        if (container.getExecutor().isLogicalClient()) {
            // 瀹㈡埛绔墽琛岀殑閫昏緫
        } else {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();
            if (dataManager.getDataValue(WukongSkillDataKeys.DSF_YINGSHEN_ZT.get())) {
                dataManager.setDataSync(WukongSkillDataKeys.DSF_DERIVE_TIMER.get(), Math.max(dataManager.getDataValue(WukongSkillDataKeys.DSF_DERIVE_TIMER.get()) - 1, 0));
                if (dataManager.getDataValue(WukongSkillDataKeys.DSF_DERIVE_TIMER.get()) == 0) {
                    dataManager.setDataSync(WukongSkillDataKeys.DSF_YINGSHEN_ZT.get(), false);
                    liftDing(serverPlayer);
                }
                if (dataManager.getDataValue(WukongSkillDataKeys.DSF_DERIVE_TIMER.get()) % PARTICLE_INTERVAL == 0) {
                    showDingParticles(serverPlayer);
                }
            }
            if (!dataManager.getDataValue(WukongSkillDataKeys.DSF_COOLING_ATTACK.get())) {
                dataManager.setDataSync(WukongSkillDataKeys.DSF_COOLING_TIMER.get(), Math.max(dataManager.getDataValue(WukongSkillDataKeys.DSF_COOLING_TIMER.get()) - 1, 0));
                if (dataManager.getDataValue(WukongSkillDataKeys.DSF_COOLING_TIMER.get()) == 0) {
                    dataManager.setDataSync(WukongSkillDataKeys.DSF_COOLING_ATTACK.get(), true);
                }
            }
        }
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
        int alpha = 128; // 50% 閫忔槑搴?
        Vec2i pos = ClientConfig.getWeaponInnatePosition(width, height);
        ResourceLocation styleTexture = ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "textures/gui/skills/spell_dsf.png");
        if (container.getDataManager().getDataValue(WukongSkillDataKeys.DSF_COOLING_ATTACK.get())) {
            alpha = 255;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // 浣跨敤榛樿鐨勯€忔槑搴︽贩鍚堟ā寮?
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        guiGraphics.blit(styleTexture, pos.x - 32, pos.y -20, 20, 20, 0.0f, 0f, 1, 1, 1, 1);
        if (!container.getDataManager().getDataValue(WukongSkillDataKeys.DSF_COOLING_ATTACK.get()) ) {
            float second = (container.getDataManager().getDataValue(WukongSkillDataKeys.DSF_COOLING_TIMER.get()) / 20.0F);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 255.0f);
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    String.format("%.1f", second),
                    pos.x - 32 + (20 -  Minecraft.getInstance().font.width(String.format("%.1f", second))) / 2,pos.y - 20 + (20 -  Minecraft.getInstance().font.lineHeight) / 2,
                    16777215
            );
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


    public static class Builder extends SkillBuilder<FashuDingshenfaSkill> {
        protected StaticAnimationProvider[] animationProviders;
        protected StaticAnimationProvider derive1;
        protected StaticAnimationProvider derive2;
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
        public Builder setDeriveAnimations(StaticAnimationProvider derive1) {
            this.derive1 = derive1;
            return this;
        }

    }
}
