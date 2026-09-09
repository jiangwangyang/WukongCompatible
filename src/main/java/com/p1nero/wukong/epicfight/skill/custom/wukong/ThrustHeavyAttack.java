package com.p1nero.wukong.epicfight.skill.custom.wukong;

import static yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.STAMINA;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.compat.EpicFightDamageType;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.avatar.HeavyAttack;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.network.PacketHandler;
import com.p1nero.wukong.network.PacketRelay;
import com.p1nero.wukong.network.packet.client.AddEntityAfterImageParticle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.skill.*;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.List;
import java.util.UUID;

/** 戳棍重击 */
public class ThrustHeavyAttack extends WeaponInnateSkill implements HeavyAttack {

    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a02b-0242ac114515");
    @NotNull protected final StaticAnimationProvider[] animations; // 0~4共有五种重击
    @NotNull protected StaticAnimationProvider xuli_start;
    protected StaticAnimationProvider stepinch;
    protected StaticAnimationProvider footage;
    protected StaticAnimationProvider fengchuanhua;
    protected StaticAnimationProvider chargePre;
    protected StaticAnimationProvider juesick_start;
    protected StaticAnimationProvider juesick_loop;
    protected StaticAnimationProvider juesick_end;
    protected StaticAnimationProvider jumpAttackHeavy;

    @Override
    public List<StaticAnimationProvider> getHeavyAttacks() {
        List<StaticAnimationProvider> staticAnimations =
                new java.util.ArrayList<>(List.of(animations));
        staticAnimations.add(footage);
        staticAnimations.add(fengchuanhua);
        return staticAnimations;
    }

    public static Builder createChargedAttack() {
        return new Builder().setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.NONE);
    }

    public ThrustHeavyAttack(Builder builder) {
        super(builder);
        chargePre = builder.pre;
        xuli_start = builder.start;
        this.animations = builder.animationProviders;

        stepinch = builder.stepinch;
        fengchuanhua = builder.fengchuanhua;
        footage = builder.footage;
        juesick_start = builder.juesick_start;
        juesick_loop = builder.juesick_loop;
        juesick_end = builder.juesick_end;

        jumpAttackHeavy = builder.jumpAttackHeavy;
    }

    /**
     * 在计时周期内使用技能才算使用衍生，否则视为重击 长按循环第一段衍生的判断在{@link
     * ThrustHeavyAttack#updateContainer(SkillContainer)}
     */
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();

        dataManager.setDataSync(
                WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack()); // 0星也是星

        if (dataManager.getDataValue(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get())
                > 0) { // 普攻击解锁退寸技
            dataManager.setData(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get(), 0);
            executer.playAnimationSynchronized(stepinch.get(), 0F);
        } else if (dataManager.getDataValue(WukongSkillDataKeys.CAN_SECOND_TIMER.get()) > 0) {
            dataManager.setDataSync(WukongSkillDataKeys.THRUST_METERS_BACK.get(), true);
        } else {
            executer.playAnimationSynchronized(xuli_start.get(), 0F);
        }
        super.executeOnServer(container, args);
    }

    @Override
    public void onInitiate(SkillContainer container) {
        SkillDataManager dataManager = container.getDataManager();
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK,
                        EVENT_UUID,
                        (event -> {
                            ServerPlayerPatch serverPlayerPatch = event.getPlayerPatch();
                            ServerPlayer player = serverPlayerPatch.getOriginal();
                            if (event.getPlayerPatch()
                                    .getAnimator()
                                    .getPlayerFor(null)
                                    .getAnimation()
                                    .equals(WukongAnimations.THRUST_JUESICK_LOOP.get())) {
                                if (container.getStack() < 4) {
                                    container
                                            .getSkill()
                                            .setConsumptionSynchronize(
                                                    container,
                                                    container.getResource()
                                                            + Config.CHARGING_SPEED
                                                                            .get()
                                                                            .floatValue()
                                                                    * 3);
                                }
                            }
                            if (event.getAttackDamage() > 0.0) {
                                modifyStamina(event.getPlayerPatch().getOriginal(), 2.0F);
                                if (event.getPlayerPatch()
                                        .getAnimator()
                                        .getPlayerFor(null)
                                        .getAnimation()
                                        .equals(WukongAnimations.PILLAR_HEAVY_FENGYUNZHUAN.get())) {
                                    createRepelForAttackTarget(
                                            player, event.getForgeEvent().getEntity(), 2);
                                } else if (event.getPlayerPatch()
                                        .getAnimator()
                                        .getPlayerFor(null)
                                        .getAnimation()
                                        .equals(WukongAnimations.THRUST_FOOTAGE.get())) {
                                    createRepelForAttackTarget(
                                            player, event.getForgeEvent().getEntity(), 1);
                                } else if (event.getPlayerPatch()
                                        .getAnimator()
                                        .getPlayerFor(null)
                                        .getAnimation()
                                        .equals(WukongAnimations.THRUST_CHARGED3.get())) {
                                    createRepelForAttackTarget(
                                            player, event.getForgeEvent().getEntity(), 1.5);
                                }
                            }
                        }));

        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.ACTION_EVENT_SERVER,
                        EVENT_UUID,
                        (event -> {
                            ServerPlayerPatch serverPlayerPatch = event.getPlayerPatch();
                            ServerPlayer player = serverPlayerPatch.getOriginal();
                            CapabilityItem capabilityItem =
                                    EpicFightCapabilities.getItemStackCapability(
                                            player.getMainHandItem());
                            if (!WukongWeaponCategories.isWeaponValid(event.getPlayerPatch())) {
                                return;
                            }
                            var autoAnimations =
                                    capabilityItem.getAutoAttackMotion(event.getPlayerPatch());
                            for (int i = 0; i < autoAnimations.size(); i++) {
                                if (autoAnimations.get(i).equals(event.getAnimation()) && i < 6) {

                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.Thrust_RETREAT_TIMER.get(),
                                                    30);
                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.Thrust_CAN_SECOND_DERIVE
                                                            .get(),
                                                    false);
                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.Thrust_STEOP_BACK.get(),
                                                    false);
                                    dataManager.setDataSync(
                                            WukongSkillDataKeys.THRUST_METERS_BACK.get(), false);
                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.Thrust_DERIVE_TIMER_TWO
                                                            .get(),
                                                    0);
                                    return;
                                }
                            }
                        }));

        // 监听玩家退寸受到伤害
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK,
                        EVENT_UUID,
                        (event -> {
                            if (event.getDamageSource().is(DamageTypes.FALL)
                                    && container
                                            .getDataManager()
                                            .getDataValue(
                                                    WukongSkillDataKeys.THRUST_PROTECT_NEXT_FALL
                                                            .get())) {
                                event.setCanceled(true);
                                event.setResult(AttackResult.ResultType.MISSED);
                                event.getPlayerPatch().getOriginal().resetFallDistance();
                                container
                                        .getDataManager()
                                        .setData(
                                                WukongSkillDataKeys.THRUST_PROTECT_NEXT_FALL.get(),
                                                false);
                            }
                            if (container
                                    .getDataManager()
                                    .getDataValue(WukongSkillDataKeys.Thrust_STEOP_BACK.get())) {
                                container
                                        .getSkill()
                                        .setConsumptionSynchronize(
                                                container,
                                                container.getResource()
                                                        + Config.CHARGING_SPEED.get().floatValue()
                                                                * 90); // 获得大量棍势
                                PacketRelay.sendToAll(
                                        PacketHandler.INSTANCE,
                                        new AddEntityAfterImageParticle(
                                                event.getPlayerPatch().getOriginal().getId()));
                                event.getPlayerPatch()
                                        .playSound(WuKongSounds.PERFECT_DODGE.get(), 0.5F, 0, 0);
                                modifyStamina(event.getPlayerPatch().getOriginal(), 5.0F);
                                event.setCanceled(true);
                            }
                        }));

        super.onInitiate(container);
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        PlayerEventListener listener = container.getExecutor().getEventListener();
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
    }

    public void createRepelForAttackTarget(
            ServerPlayer player, Entity target, double knockbackStrength) {
        Vec3 playerPos = player.position();
        if (target instanceof LivingEntity) {
            Vec3 targetPos = target.position();
            double deltaX = targetPos.x - playerPos.x;
            double deltaZ = targetPos.z - playerPos.z;
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (distance > 0.1) {
                deltaX /= distance;
                deltaZ /= distance;
                target.push(deltaX * knockbackStrength, 0.0, deltaZ * knockbackStrength);
                target.setSecondsOnFire(10);
            }
        }
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
            boolean isKeyDown = EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown();
            dataManager.setDataSync(WukongSkillDataKeys.Thrust_KEY_PRESSING.get(), isKeyDown);

        } else {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();
            if (dataManager.getDataValue(WukongSkillDataKeys.THRUST_FASHU_STACK.get())) {
                if (container.getStack() < 3) {
                    this.setStackSynchronize(container, Math.min(container.getStack() + 2, 4));
                    serverPlayerPatch.playSound(
                            WuKongSounds.XULI_LEVEL.get(container.getStack() - 1).get(), 1, 1);
                    dataManager.setData(
                            WukongSkillDataKeys.Thrust_LAST_STACK.get(), container.getStack());
                }
                dataManager.setDataSync(WukongSkillDataKeys.THRUST_FASHU_TIMER.get(), 20);
                dataManager.setDataSync(WukongSkillDataKeys.THRUST_FASHU_STACK.get(), false);
            }

            dataManager.setDataSync(
                    WukongSkillDataKeys.THRUST_FASHU_TIMER.get(),
                    Math.max(
                            dataManager.getDataValue(WukongSkillDataKeys.THRUST_FASHU_TIMER.get())
                                    - 1,
                            0)); // 派生重击
            if (dataManager.getDataValue(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get()) != 0) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.Thrust_RETREAT_TIMER.get(),
                        dataManager.getDataValue(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get())
                                - 1); // 退寸
            }
            if (dataManager.getDataValue(WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get()) != 0) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get(),
                        dataManager.getDataValue(WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get())
                                - 1);
            }
            if (dataManager.getDataValue(WukongSkillDataKeys.CAN_SECOND_TIMER.get()) != 0) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.CAN_SECOND_TIMER.get(),
                        dataManager.getDataValue(WukongSkillDataKeys.CAN_SECOND_TIMER.get()) - 1);
            }

            if (container.getStack()
                    > dataManager.getDataValue(WukongSkillDataKeys.Thrust_LAST_STACK.get())) {
                serverPlayerPatch.playSound(
                        WuKongSounds.XULI_LEVEL.get(container.getStack() - 1).get(), 1, 1);
                dataManager.setDataSync(WukongSkillDataKeys.Thrust_PLAY_SOUND.get(), false);
                if (container.getStack() != 3
                        && dataManager.getDataValue(WukongSkillDataKeys.Thrust_KEY_PRESSING.get()))
                    serverPlayerPatch.playSound(WuKongSounds.XULI_LEVEL_RISE03.get(), 2.0F, 2.0F);
            }

            dataManager.setData(WukongSkillDataKeys.Thrust_LAST_STACK.get(), container.getStack());
            dataManager.setDataSync(
                    WukongSkillDataKeys.RED_TIMER.get(),
                    Math.max(
                            dataManager.getDataValue(WukongSkillDataKeys.RED_TIMER.get()) - 1,
                            0)); // 使用技能星数显示
            if (dataManager.getDataValue(WukongSkillDataKeys.Thrust_IS_CHARGING.get())) {
                if (!WukongWeaponCategories.isWeaponValid(serverPlayerPatch)) {
                    dataManager.setDataSync(WukongSkillDataKeys.Thrust_IS_CHARGING.get(), false);
                    this.setConsumptionSynchronize(container, 1);
                    this.setStackSynchronize(container, 0);
                    return;
                }
                if (container.getStack() < 3) {
                    this.setConsumptionSynchronize(
                            container,
                            container.getResource() + Config.CHARGING_SPEED.get().floatValue());
                }
                if (!dataManager.getDataValue(WukongSkillDataKeys.Thrust_KEY_PRESSING.get())) {
                    dataManager.setDataSync(WukongSkillDataKeys.Thrust_IS_CHARGING.get(), false);
                    serverPlayerPatch.playSound(WuKongSounds.XULI_ATTACK_4.get(), 2, 2);
                    serverPlayerPatch.playAnimationSynchronized(
                            animations[container.getStack()].get(), 0.0F);
                    dataManager.setDataSync(
                            WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack());
                    resetConsumption(container, serverPlayerPatch);
                }
            }

            if (dataManager.getDataValue(WukongSkillDataKeys.CAN_SECOND_TIMER.get()) > 0) {
                if (dataManager.getDataValue(WukongSkillDataKeys.IS_ATTACK_KEY_DOWN.get())) {
                    // WukongMoveset.LOGGER.info("搅棍");
                    dataManager.setDataSync(WukongSkillDataKeys.CAN_SECOND_TIMER.get(), 0);
                    // 开始搅
                    if (dataManager.getDataValue(WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get())
                                    > 0
                            && !dataManager.getDataValue(
                                    WukongSkillDataKeys.IS_REPEATING_DERIVE.get())) {
                        if (dataManager.getDataValue(
                                WukongSkillDataKeys.IS_ATTACK_KEY_DOWN.get())) {
                            serverPlayerPatch.playAnimationSynchronized(juesick_start.get(), 0.15F);
                            dataManager.setDataSync(
                                    WukongSkillDataKeys.IS_REPEATING_DERIVE.get(), true);
                            dataManager.setDataSync(
                                    WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get(), 0);
                        }
                    }
                } else if (dataManager.getDataValue(WukongSkillDataKeys.THRUST_METERS_BACK.get())) {
                    // WukongMoveset.LOGGER.info("进尺");
                    dataManager.setDataSync(WukongSkillDataKeys.CAN_SECOND_TIMER.get(), 0);
                    if (container.getStack() > 0) {
                        serverPlayerPatch.playAnimationSynchronized(footage.get(), 0.0F);
                        this.setStackSynchronize(container, container.getStack() - 1);
                    } else {
                        serverPlayerPatch.playAnimationSynchronized(animations[0].get(), 0.0F);
                    }
                }
            }

            if (dataManager.getDataValue(WukongSkillDataKeys.IS_REPEATING_DERIVE.get())) {
                if (!serverPlayer.isCreative()) {
                    if (!serverPlayerPatch.hasStamina(0.1F)) {
                        serverPlayerPatch.playAnimationSynchronized(juesick_end.get(), 0.0F);
                        dataManager.setDataSync(
                                WukongSkillDataKeys.IS_REPEATING_DERIVE.get(), false);
                    }
                }
                // 重置可寸时机
                dataManager.setDataSync(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get(), 30);
                // 松手了则播end
                if (!dataManager.getDataValue(WukongSkillDataKeys.IS_ATTACK_KEY_DOWN.get())) {
                    serverPlayerPatch.playAnimationSynchronized(juesick_end.get(), 0.0F);
                    dataManager.setDataSync(WukongSkillDataKeys.IS_REPEATING_DERIVE.get(), false);
                }
            }

            if (container.getStack() < 1
                    && container.getResource() > container.getMaxResource() * 0.3) {
                breakProgress(serverPlayerPatch, container);
            } else if (container.getStack() < 2
                    && container.getResource() > container.getMaxResource() * 0.5) {
                breakProgress(serverPlayerPatch, container);
            } else if (container.getStack() < 3
                    && container.getResource() > container.getMaxResource() * 0.7) {
                breakProgress(serverPlayerPatch, container);
            }

            int current = dataManager.getDataValue(WukongSkillDataKeys.Thrust_CHARGED4_TIMER.get());
            if (current > 0) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.Thrust_CHARGED4_TIMER.get(), current - 1);
            }
            float consumption = Config.CHARGING_SPEED.get().floatValue() / 5;
            if (current == 1 && container.isFull()) {
                this.setStackSynchronize(container, 3);
                this.setConsumptionSynchronize(container, container.getMaxResource() - consumption);
            }
            if (current == 0
                    && container.getStack() >= 3
                    && container.getResource() > consumption + 0.1) {
                this.setConsumptionSynchronize(container, container.getResource() - consumption);
            }
        }
    }

    public void breakProgress(ServerPlayerPatch serverPlayerPatch, SkillContainer container) {
        this.setConsumptionSynchronize(container, 0.1F);
        this.setStackSynchronize(container, container.getStack() + 1);
    }

    /** copy from {@link yesman.epicfight.events.EntityEvents#attackEvent(LivingAttackEvent)} */
    public void processDamage(
            PlayerPatch<?> playerPatch,
            DamageSource damageSource,
            AttackResult.ResultType attackResult,
            float amount,
            @Nullable LivingEntityPatch<?> attackerPatch) {
        AttackResult result =
                playerPatch != null
                        ? AttackResult.of(attackResult, amount)
                        : AttackResult.success(amount);
        if (attackerPatch != null) {
            attackerPatch.setLastAttackResult(result);
        }
        EpicFightDamageSource deflictedDamage =
                (damageSource instanceof EpicFightDamageSource epicFightDamageSource)
                        ? epicFightDamageSource
                        : EpicFightDamageSources.fromVanillaDamageSource(damageSource);
        deflictedDamage.addRuntimeTag(EpicFightDamageType.PARTIAL_DAMAGE);
        if (playerPatch != null) {
            playerPatch.getOriginal().hurt(deflictedDamage, result.damage);
        }
    }

    /** 清空耐力并播红光和音效 */
    private void resetConsumption(SkillContainer container, ServerPlayerPatch executer) {
        if (container.getStack() > 0) {
            int soundIndex = Math.min(container.getStack(), WuKongSounds.stackSounds.size()) - 1;
            executer.playSound(WuKongSounds.stackSounds.get(soundIndex).get(), 1, 1);
        } else {
            container
                    .getDataManager()
                    .setDataSync(WukongSkillDataKeys.Thrust_PLAY_SOUND.get(), true);
        }
        container
                .getDataManager()
                .setDataSync(
                        WukongSkillDataKeys.RED_TIMER.get(),
                        Config.DERIVE_CHECK_TIME.get().intValue()); // 通知客户端该亮红灯了
        this.setStackSynchronize(container, 0);
        this.setConsumptionSynchronize(container, 1);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }

    /** 根据棍式和星级画图 本方法完全重写 Epic Fight 默认的技能图标绘制, 战斗模式 HUD 仅显示此自定义画面 */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawOnGui(
            BattleModeGui gui,
            SkillContainer container,
            GuiGraphics guiGraphics,
            float x,
            float y,
            float partialTick) {
        // 显式开启混合并重置着色器颜色, 避免依赖上游 GL 状态导致光晕画成不透明色块
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int stack = Mth.clamp(container.getStack(), 0, 4);
        int style =
                container
                                .getExecutor()
                                .getHoldingItemCapability(InteractionHand.MAIN_HAND)
                                .getStyle(container.getExecutor())
                                .universalOrdinal()
                        - WukongStyles.SMASH.universalOrdinal();
        float cooldownRatio =
                !container.isFull() && !container.isActivated()
                        ? container.getResource(1.0F)
                        : 1.0F;
        int progress = Mth.clamp((int) Math.ceil(cooldownRatio * 40), 0, 40);
        Window sr = Minecraft.getInstance().getWindow();
        int width = sr.getGuiScaledWidth();
        int height = sr.getGuiScaledHeight();
        Vec2i pos = ClientConfig.getWeaponInnatePosition(width, height);
        ResourceLocation progressTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID,
                        "textures/gui/staff_stack/progress/" + progress + ".png");
        ResourceLocation styleTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID,
                        "textures/gui/staff_stack/stance/" + style + "_0.png");
        ResourceLocation stackBgTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/gui/staff_stack/stack/ui" + stack + ".png");
        ResourceLocation stackTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID,
                        "textures/gui/staff_stack/stack/stack" + stack + ".png");
        ResourceLocation goldenLightTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/gold.png");
        ResourceLocation whiteLightTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/white.png");
        ResourceLocation redLightTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/red.png");
        guiGraphics.blit(
                progressTexture, pos.x - 12, pos.y - 12, 48, 48, 0.0F, 0.0F, 256, 256, 256, 256);
        drawTexture(guiGraphics, styleTexture, pos.x - 12, pos.y - 12);
        drawTexture(guiGraphics, stackBgTexture, pos.x - 12, pos.y - 12);
        Vec2i light1 = new Vec2i(pos.x - 14, pos.y + 3);
        Vec2i light2 = new Vec2i(pos.x - 5, pos.y + 1);
        Vec2i light3 = new Vec2i(pos.x + 4, pos.y - 5);
        List<Vec2i> lightList = List.of(light1, light2, light3);

        if (container.isFull()) {
            for (Vec2i lightPos : lightList) {
                drawLightTexture(guiGraphics, goldenLightTexture, lightPos.x, lightPos.y);
            }
        }
        if (container.getDataManager().getDataValue(WukongSkillDataKeys.RED_TIMER.get()) > 0) {
            int star =
                    Math.min(
                            container
                                    .getDataManager()
                                    .getDataValue(WukongSkillDataKeys.STARS_CONSUMED.get()),
                            3);
            if (star > 0) {
                for (int i = 0; i < star; i++) {
                    Vec2i lightPos = lightList.get(i);
                    drawLightTexture(guiGraphics, redLightTexture, lightPos.x, lightPos.y);
                }
            }
        }

        if (stack > 0) {
            for (int i = 0; i < Math.min(stack, 3); i++) {
                Vec2i lightPos = lightList.get(i);
                drawLightTexture(guiGraphics, whiteLightTexture, lightPos.x, lightPos.y);
            }
            drawTexture(guiGraphics, stackTexture, pos.x - 12, pos.y - 12);
        }
    }

    public void drawTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        guiGraphics.blit(texture, x, y, 48, 48, 0.0F, 0.0F, 256, 256, 256, 256);
    }

    /** 光晕以星点为中心缩小绘制(星点位于 48x48 贴图中心, 即入参偏移 +24), 避免光斑过大溢出圆环 */
    public void drawLightTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        int size = 20;
        int offset = (48 - size) / 2;
        guiGraphics.blit(
                texture, x + offset, y + offset, size, size, 0.0F, 0.0F, 256, 256, 256, 256);
    }

    public WeaponInnateSkill registerPropertiesToAnimation() {
        return this;
    }

    public static class Builder extends SkillBuilder<ThrustHeavyAttack> {
        protected StaticAnimationProvider[] animationProviders;
        protected StaticAnimationProvider stepinch;
        protected StaticAnimationProvider footage;
        protected StaticAnimationProvider fengchuanhua;
        protected StaticAnimationProvider jumpAttackHeavy;
        protected StaticAnimationProvider juesick_start;
        protected StaticAnimationProvider juesick_loop;
        protected StaticAnimationProvider juesick_end;
        StaticAnimationProvider chargingAnimation;
        protected StaticAnimationProvider start;

        StaticAnimationProvider pre;

        public Builder() {}

        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        public Builder setActivateType(Skill.ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        public Builder setResource(Skill.Resource resource) {
            this.resource = resource;
            return this;
        }

        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }

        public Builder setChargingAnimation(StaticAnimationProvider chargingAnimation) {
            this.chargingAnimation = chargingAnimation;
            return this;
        }

        public Builder setChargePreAnimation(StaticAnimationProvider pre) {
            this.pre = pre;
            return this;
        }

        /** 如果是可长按的衍生则derive1就是pre动画，具体逻辑在动画那里判断 */
        public Builder setDeriveAnimations(
                StaticAnimationProvider stepinch,
                StaticAnimationProvider footage,
                StaticAnimationProvider fengchuanhua,
                StaticAnimationProvider juesick_start,
                StaticAnimationProvider juesick_loop,
                StaticAnimationProvider juesick_end) {
            this.stepinch = stepinch;
            this.fengchuanhua = fengchuanhua;
            this.footage = footage;
            this.juesick_start = juesick_start;
            this.juesick_loop = juesick_loop;
            this.juesick_end = juesick_end;
            return this;
        }

        /** 0~4星重击 */
        public Builder setHeavyAttacks(StaticAnimationProvider... animationProviders) {
            this.animationProviders = animationProviders;
            return this;
        }

        public Builder setStartAttacks(StaticAnimationProvider start) {
            this.start = start;
            return this;
        }

        public Builder setJumpAttackHeavy(StaticAnimationProvider jumpAttackHeavy) {
            this.jumpAttackHeavy = jumpAttackHeavy;
            return this;
        }
    }
}
