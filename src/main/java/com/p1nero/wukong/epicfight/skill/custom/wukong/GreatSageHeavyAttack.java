package com.p1nero.wukong.epicfight.skill.custom.wukong;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.animation.custom.WukongDodgeAnimation;
import com.p1nero.wukong.epicfight.compat.EpicFightDamageType;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.avatar.HeavyAttack;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.skill.BasicAttack;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GreatSageHeavyAttack extends WeaponInnateSkill implements HeavyAttack {
    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a02b-0242ac114585");
    private static final int MAX_CHARGED4_TICKS = 300;

    @NotNull private final StaticAnimationProvider[] derivedAttacks1;
    @NotNull private final StaticAnimationProvider[] derivedAttacks2;
    @NotNull private final StaticAnimationProvider[] chargedAttacks;
    @NotNull private final StaticAnimationProvider[] pillarHeavyAttacks;
    @NotNull private final StaticAnimationProvider[] pillarStartAttacks;
    @NotNull private final StaticAnimationProvider pillarUp;
    @NotNull private final StaticAnimationProvider chargePre;

    public static Builder createChargedAttack() {
        return new Builder().setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.NONE);
    }

    public GreatSageHeavyAttack(Builder builder) {
        super(builder);
        this.derivedAttacks1 =
                requireProviders(builder.derivedAttacks1, 3, "first derived attacks");
        this.derivedAttacks2 =
                requireProviders(builder.derivedAttacks2, 4, "second derived attacks");
        this.chargedAttacks = requireProviders(builder.chargedAttacks, 5, "charged attacks");
        this.pillarHeavyAttacks =
                requireProviders(builder.pillarHeavyAttacks, 5, "pillar heavy attacks");
        this.pillarStartAttacks =
                requireProviders(builder.pillarStartAttacks, 5, "pillar start attacks");
        this.pillarUp = requireProvider(builder.pillarUp, "pillar up attack");
        this.chargePre = requireProvider(builder.chargePre, "charge pre animation");
    }

    private static StaticAnimationProvider[] requireProviders(
            StaticAnimationProvider[] providers, int minimumLength, String name) {
        if (providers == null || providers.length < minimumLength) {
            throw new IllegalArgumentException("Great Sage requires " + minimumLength + " " + name);
        }
        return providers;
    }

    private static StaticAnimationProvider requireProvider(
            StaticAnimationProvider provider, String name) {
        if (provider == null) {
            throw new IllegalArgumentException("Great Sage requires " + name);
        }
        return provider;
    }

    @Override
    public List<StaticAnimationProvider> getHeavyAttacks() {
        List<StaticAnimationProvider> animations = new ArrayList<>();
        Collections.addAll(animations, derivedAttacks1);
        Collections.addAll(animations, derivedAttacks2);
        Collections.addAll(animations, chargedAttacks);
        Collections.addAll(animations, pillarHeavyAttacks);
        Collections.addAll(animations, pillarStartAttacks);
        animations.add(pillarUp);
        animations.add(chargePre);
        return animations;
    }

    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        super.executeOnServer(container, args);
        ServerPlayerPatch executor = container.getServerExecutor();
        if (executor == null || !WukongWeaponCategories.isWeaponValid(executor)) {
            return;
        }

        SkillDataManager data = container.getDataManager();
        ServerPlayer player = executor.getOriginal();
        int stack = Math.max(0, Math.min(container.getStack(), 4));
        int combo =
                Math.max(
                        0,
                        Math.min(data.getDataValue(WukongSkillDataKeys.GREATSAGE_NUMBER.get()), 3));

        if (stack >= 4) {
            data.setDataSync(WukongSkillDataKeys.STARS_CONSUMED.get(), 4);
            executor.playAnimationSynchronized(chargedAttacks[4].get(), 0.0F);
            resetConsumption(container, executor);
            return;
        }

        if (data.getDataValue(WukongSkillDataKeys.CAN_SECOND_TIMER.get()) > 0 && stack > 0) {
            data.setDataSync(WukongSkillDataKeys.CAN_SECOND_TIMER.get(), 0);
            data.setDataSync(WukongSkillDataKeys.STARS_CONSUMED.get(), 1);
            data.setDataSync(
                    WukongSkillDataKeys.RED_TIMER.get(), Config.DERIVE_CHECK_TIME.get().intValue());
            executor.playAnimationSynchronized(derivedAttacks2[combo].get(), 0.0F);
            setStackSynchronize(container, stack - 1);
            return;
        }

        if (data.getDataValue(WukongSkillDataKeys.CAN_FIRST_TIMER.get()) > 0) {
            data.setDataSync(WukongSkillDataKeys.CAN_FIRST_TIMER.get(), 0);
            if (combo == 3) {
                if (!data.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())) {
                    data.setDataSync(WukongSkillDataKeys.GREATSAGE_PILLAR.get(), true);
                    executor.playAnimationSynchronized(pillarStartAttacks[stack].get(), 0.1F);
                }
            } else {
                data.setDataSync(WukongSkillDataKeys.GREATSAGE_PILLAR.get(), false);
                data.setDataSync(WukongSkillDataKeys.CAN_SECOND_TIMER.get(), 20);
                executor.playAnimationSynchronized(derivedAttacks1[combo].get(), 0.1F);
            }
            return;
        }

        if (!data.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())) {
            data.setDataSync(WukongSkillDataKeys.GREATSAGE_PILLAR.get(), false);
            executor.playAnimationSynchronized(chargePre.get(), 0.2F);
        }
    }

    @Override
    public void onInitiate(SkillContainer container) {
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK,
                        EVENT_UUID,
                        event -> {
                            if (event.getDamageSource()
                                            instanceof EpicFightDamageSource epicFightDamageSource
                                    && epicFightDamageSource.is(
                                            EpicFightDamageType.PARTIAL_DAMAGE)) {
                                return;
                            }

                            SkillDataManager data = container.getDataManager();
                            ServerPlayerPatch playerPatch = event.getPlayerPatch();
                            if (data.getDataValue(WukongSkillDataKeys.IS_IN_SPECIAL_ATTACK.get())) {
                                if (!data.getDataValue(
                                        WukongSkillDataKeys.IS_SPECIAL_SUCCESS.get())) {
                                    container
                                            .getSkill()
                                            .setConsumptionSynchronize(
                                                    container,
                                                    container.getResource()
                                                            + Config.CHARGING_SPEED
                                                                            .get()
                                                                            .floatValue()
                                                                    * 60.0F);
                                    data.setDataSync(
                                            WukongSkillDataKeys.IS_SPECIAL_SUCCESS.get(), true);
                                }
                                BasicAttack.setComboCounterWithEvent(
                                        ComboCounterHandleEvent.Causal.ANOTHER_ACTION_ANIMATION,
                                        playerPatch,
                                        playerPatch.getSkill(SkillSlots.BASIC_ATTACK),
                                        derivedAttacks1[0].get(),
                                        0);
                                event.setResult(AttackResult.ResultType.MISSED);
                                event.setCanceled(true);
                                return;
                            }

                            float damageReduce =
                                    data.getDataValue(WukongSkillDataKeys.DAMAGE_REDUCE.get());
                            if (damageReduce <= 0.0F && !isHeavyAttackActive(playerPatch)) {
                                return;
                            }

                            if (event.getDamageSource()
                                    instanceof EpicFightDamageSource epicFightDamageSource) {
                                epicFightDamageSource.setStunType(StunType.NONE);
                            }
                            Entity attacker = event.getDamageSource().getEntity();
                            LivingEntityPatch<?> attackerPatch =
                                    attacker == null
                                            ? null
                                            : EpicFightCapabilities.getEntityPatch(
                                                    attacker, LivingEntityPatch.class);
                            float damage =
                                    damageReduce > 0.0F
                                            ? event.getDamage()
                                                    * Math.max(0.0F, 1.0F - damageReduce)
                                            : event.getDamage();
                            processDamage(
                                    playerPatch, event.getDamageSource(), damage, attackerPatch);
                            event.setResult(AttackResult.ResultType.MISSED);
                            event.setCanceled(true);
                        });

        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.ACTION_EVENT_SERVER,
                        EVENT_UUID,
                        event -> {
                            ServerPlayerPatch playerPatch = event.getPlayerPatch();
                            ServerPlayer player = playerPatch.getOriginal();
                            if (!WukongWeaponCategories.isWeaponValid(playerPatch)) {
                                return;
                            }

                            SkillDataManager data = container.getDataManager();
                            // PILLAR_LOOP0等立棍衔接动画由立棍起手在服务端自动播放 不属于玩家主动动作 不应取消蓄力
                            // 否则蓄力状态被清空后玩家会被永久困在立棍循环里
                            if (data.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())
                                    && !event.getAnimation().equals(chargePre.get())
                                    && !isPillarFlowAnimation(event.getAnimation())
                                    && !(event.getAnimation().get()
                                            instanceof WukongDodgeAnimation)) {
                                cancelCharge(container, playerPatch);
                            }

                            CapabilityItem capability =
                                    EpicFightCapabilities.getItemStackCapability(
                                            player.getMainHandItem());
                            var autoAnimations = capability.getAutoAttackMotion(playerPatch);
                            for (int i = 0; i < Math.min(autoAnimations.size(), 4); i++) {
                                if (autoAnimations.get(i).equals(event.getAnimation())) {
                                    data.setDataSync(WukongSkillDataKeys.CAN_FIRST_TIMER.get(), 25);
                                    data.setDataSync(WukongSkillDataKeys.CAN_SECOND_TIMER.get(), 0);
                                    data.setDataSync(WukongSkillDataKeys.GREATSAGE_NUMBER.get(), i);
                                    data.setDataSync(
                                            WukongSkillDataKeys.GREATSAGE_PILLAR.get(), false);
                                    return;
                                }
                            }
                        });

        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE,
                        EVENT_UUID,
                        event -> {
                            if (container.isFull()) {
                                container
                                        .getDataManager()
                                        .setDataSync(
                                                WukongSkillDataKeys.CHARGED4_TIMER.get(),
                                                MAX_CHARGED4_TICKS);
                            }
                        });

        super.onInitiate(container);
    }

    @Override
    public void onRemoved(SkillContainer container) {
        PlayerEventListener listener = container.getExecutor().getEventListener();
        listener.removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE, EVENT_UUID);
        super.onRemoved(container);
    }

    private boolean isHeavyAttackActive(ServerPlayerPatch playerPatch) {
        var activeAnimation = playerPatch.getAnimator().getPlayerFor(null).getAnimation();
        for (StaticAnimationProvider animation : getHeavyAttacks()) {
            if (animation.get().equals(activeAnimation)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPillarFlowAnimation(
            AnimationManager.AnimationAccessor<? extends MainFrameAnimation> animation) {
        for (StaticAnimationProvider pillarStart : pillarStartAttacks) {
            if (pillarStart.get().equals(animation)) {
                return true;
            }
        }
        // PILLAR_LOOP0与PILLAR_CHARGED_LOOP4字段本身就是accessor 不能调用get 否则取到动画实例导致比较永远失败
        return pillarUp.get().equals(animation)
                || WukongAnimations.PILLAR_LOOP0.equals(animation)
                || WukongAnimations.PILLAR_CHARGED_LOOP4.equals(animation);
    }

    private void processDamage(
            PlayerPatch<?> playerPatch,
            DamageSource damageSource,
            float amount,
            @Nullable LivingEntityPatch<?> attackerPatch) {
        AttackResult result = AttackResult.of(AttackResult.ResultType.SUCCESS, amount);
        if (attackerPatch != null) {
            attackerPatch.setLastAttackResult(result);
        }
        EpicFightDamageSource deflectedDamage =
                damageSource instanceof EpicFightDamageSource epicFightDamageSource
                        ? epicFightDamageSource
                        : EpicFightDamageSources.fromVanillaDamageSource(damageSource);
        deflectedDamage.addRuntimeTag(EpicFightDamageType.PARTIAL_DAMAGE);
        playerPatch.getOriginal().hurt(deflectedDamage, result.damage);
    }

    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager data = container.getDataManager();

        if (container.getExecutor().isLogicalClient()) {
            boolean keyDown = EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown();
            data.setDataSync(WukongSkillDataKeys.KEY_PRESSING.get(), keyDown);
            return;
        }

        ServerPlayerPatch playerPatch = (ServerPlayerPatch) container.getExecutor();
        ServerPlayer player = playerPatch.getOriginal();

        if (data.getDataValue(WukongSkillDataKeys.ADD_BEANS.get())) {
            int currentStack = container.getStack();
            if (currentStack < 3) {
                int nextStack = currentStack + 1;
                setStackSynchronize(container, nextStack);
                playerPatch.playSound(WuKongSounds.XULI_LEVEL.get(nextStack - 1).get(), 1.0F, 1.0F);
                data.setData(WukongSkillDataKeys.LAST_STACK.get(), nextStack);
            }
            data.setDataSync(WukongSkillDataKeys.ADD_BEANS.get(), false);
        }

        decrementTimer(data, WukongSkillDataKeys.CAN_FIRST_TIMER.get(), player);
        decrementTimer(data, WukongSkillDataKeys.CAN_SECOND_TIMER.get(), player);
        decrementTimer(data, WukongSkillDataKeys.RED_TIMER.get(), player);

        int stack = container.getStack();
        int lastStack = data.getDataValue(WukongSkillDataKeys.LAST_STACK.get());
        if (stack > lastStack && stack > 0 && stack <= WuKongSounds.XULI_LEVEL.size()) {
            playerPatch.playSound(WuKongSounds.XULI_LEVEL.get(stack - 1).get(), 1.0F, 1.0F);
            if (data.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())
                    && data.getDataValue(WukongSkillDataKeys.GREATSAGE_PILLAR.get())
                    && lastStack < 3) {
                playerPatch.playAnimationSynchronized(pillarUp.get(), 0.1F);
            }
        }
        data.setData(WukongSkillDataKeys.LAST_STACK.get(), stack);

        if (data.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())) {
            if (!WukongWeaponCategories.isWeaponValid(playerPatch)) {
                cancelCharge(container, playerPatch);
                return;
            }

            boolean pillar = data.getDataValue(WukongSkillDataKeys.GREATSAGE_PILLAR.get());
            int maxChargeStack = pillar ? 3 : 4;
            if (container.getStack() < maxChargeStack) {
                setConsumptionSynchronize(
                        container,
                        container.getResource() + Config.CHARGING_SPEED.get().floatValue());
            }

            if (!data.getDataValue(WukongSkillDataKeys.KEY_PRESSING.get())) {
                int releaseStack = Math.max(0, Math.min(container.getStack(), 4));
                data.setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false);
                data.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
                data.setDataSync(WukongSkillDataKeys.STARS_CONSUMED.get(), releaseStack);
                playerPatch.playSound(WuKongSounds.XULI_ATTACK_4.get(), 2.0F, 2.0F);

                if (pillar) {
                    data.setDataSync(WukongSkillDataKeys.GREATSAGE_PILLAR.get(), false);
                    playerPatch.playAnimationSynchronized(
                            pillarHeavyAttacks[
                                    Math.min(releaseStack, pillarHeavyAttacks.length - 1)]
                                    .get(),
                            0.0F);
                } else {
                    playerPatch.playAnimationSynchronized(
                            chargedAttacks[Math.min(releaseStack, chargedAttacks.length - 1)].get(),
                            0.0F);
                }
                resetConsumption(container, playerPatch);
                return;
            }
        }

        if (container.getStack() < 1
                && container.getResource() > container.getMaxResource() * 0.3F) {
            breakProgress(container);
        } else if (container.getStack() < 2
                && container.getResource() > container.getMaxResource() * 0.5F) {
            breakProgress(container);
        } else if (container.getStack() < 3
                && container.getResource() > container.getMaxResource() * 0.7F) {
            breakProgress(container);
        }

        int charged4Timer = data.getDataValue(WukongSkillDataKeys.CHARGED4_TIMER.get());
        if (charged4Timer > 0) {
            data.setDataSync(WukongSkillDataKeys.CHARGED4_TIMER.get(), charged4Timer - 1);
        }

        float decay = Config.CHARGING_SPEED.get().floatValue() / 5.0F;
        if (charged4Timer == 1 && container.isFull()) {
            setStackSynchronize(container, 3);
            setConsumptionSynchronize(container, container.getMaxResource() - decay);
        } else if (charged4Timer == 0
                && container.getStack() >= 3
                && container.getResource() > decay + 0.1F) {
            setConsumptionSynchronize(container, container.getResource() - decay);
        }
    }

    private static void decrementTimer(
            SkillDataManager data,
            yesman.epicfight.skill.SkillDataKey<Integer> key,
            ServerPlayer player) {
        int value = data.getDataValue(key);
        if (value > 0) {
            data.setDataSync(key, value - 1);
        }
    }

    private void breakProgress(SkillContainer container) {
        setConsumptionSynchronize(container, 0.1F);
        setStackSynchronize(container, container.getStack() + 1);
    }

    private void cancelCharge(SkillContainer container, ServerPlayerPatch playerPatch) {
        SkillDataManager data = container.getDataManager();
        data.setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false);
        data.setDataSync(WukongSkillDataKeys.GREATSAGE_PILLAR.get(), false);
        setConsumptionSynchronize(container, 1.0F);
        setStackSynchronize(container, 0);
    }

    private void resetConsumption(SkillContainer container, ServerPlayerPatch playerPatch) {
        int stack = Math.max(0, Math.min(container.getStack(), WuKongSounds.stackSounds.size()));
        if (stack > 0) {
            playerPatch.playSound(WuKongSounds.stackSounds.get(stack - 1).get(), 1.0F, 1.0F);
        }
        container
                .getDataManager()
                .setDataSync(
                        WukongSkillDataKeys.RED_TIMER.get(),
                        Config.DERIVE_CHECK_TIME.get().intValue());
        setStackSynchronize(container, 0);
        setConsumptionSynchronize(container, 1.0F);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawOnGui(
            BattleModeGui gui,
            SkillContainer container,
            GuiGraphics graphics,
            float x,
            float y,
            float partialTick) {
        // 显式开启混合并重置着色器颜色, 避免依赖上游 GL 状态导致光晕画成不透明色块
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int stack = Math.max(0, Math.min(container.getStack(), 4));
        float cooldownRatio =
                !container.isFull() && !container.isActivated()
                        ? container.getResource(1.0F)
                        : 1.0F;
        int progress = Mth.clamp((int) Math.ceil(cooldownRatio * 40.0F), 0, 40);
        Window window = Minecraft.getInstance().getWindow();
        Vec2i pos =
                ClientConfig.getWeaponInnatePosition(
                        window.getGuiScaledWidth(), window.getGuiScaledHeight());

        ResourceLocation progressTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID,
                        "textures/gui/staff_stack/progress/" + progress + ".png");
        ResourceLocation styleTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID,
                        "textures/gui/staff_stack/stance/greatsage_style.png");
        ResourceLocation stackBackground =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/gui/staff_stack/stack/ui" + stack + ".png");
        ResourceLocation stackTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID,
                        "textures/gui/staff_stack/stack/stack" + stack + ".png");
        ResourceLocation gold =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/gold.png");
        ResourceLocation white =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/white.png");
        ResourceLocation red =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/gui/staff_stack/light/red.png");

        graphics.blit(
                progressTexture, pos.x - 12, pos.y - 12, 48, 48, 0.0F, 0.0F, 256, 256, 256, 256);
        drawTexture(graphics, styleTexture, pos.x - 12, pos.y - 12);
        drawTexture(graphics, stackBackground, pos.x - 12, pos.y - 12);

        List<Vec2i> lights =
                List.of(
                        new Vec2i(pos.x - 14, pos.y + 3),
                        new Vec2i(pos.x - 5, pos.y + 1),
                        new Vec2i(pos.x + 4, pos.y - 5));

        if (container.isFull()) {
            lights.forEach(light -> drawTexture(graphics, gold, light.x, light.y));
        }
        if (container.getDataManager().getDataValue(WukongSkillDataKeys.RED_TIMER.get()) > 0) {
            int consumed =
                    Math.min(
                            container
                                    .getDataManager()
                                    .getDataValue(WukongSkillDataKeys.STARS_CONSUMED.get()),
                            lights.size());
            for (int i = 0; i < consumed; i++) {
                drawTexture(graphics, red, lights.get(i).x, lights.get(i).y);
            }
        }
        for (int i = 0; i < Math.min(stack, lights.size()); i++) {
            drawTexture(graphics, white, lights.get(i).x, lights.get(i).y);
        }
        if (stack > 0) {
            drawTexture(graphics, stackTexture, pos.x - 12, pos.y - 12);
        }
    }

    private static void drawTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, 48, 48, 0.0F, 0.0F, 256, 256, 256, 256);
    }

    @Override
    public WeaponInnateSkill registerPropertiesToAnimation() {
        return this;
    }

    public static class Builder extends SkillBuilder<GreatSageHeavyAttack> {
        private StaticAnimationProvider[] derivedAttacks1;
        private StaticAnimationProvider[] derivedAttacks2;
        private StaticAnimationProvider[] chargedAttacks;
        private StaticAnimationProvider[] pillarHeavyAttacks;
        private StaticAnimationProvider[] pillarStartAttacks;
        private StaticAnimationProvider pillarUp;
        private StaticAnimationProvider chargePre;

        @Override
        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        @Override
        public Builder setActivateType(Skill.ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        @Override
        public Builder setResource(Skill.Resource resource) {
            this.resource = resource;
            return this;
        }

        @Override
        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }

        public Builder setDerivedHeavyAttacks1(StaticAnimationProvider... providers) {
            this.derivedAttacks1 = providers;
            return this;
        }

        public Builder setDerivedHeavyAttacks2(StaticAnimationProvider... providers) {
            this.derivedAttacks2 = providers;
            return this;
        }

        public Builder setHeavyAttacks(StaticAnimationProvider... providers) {
            this.chargedAttacks = providers;
            return this;
        }

        public Builder setPillarHeavyAttacks(StaticAnimationProvider... providers) {
            this.pillarHeavyAttacks = providers;
            return this;
        }

        public Builder setPillarStartAttacks(StaticAnimationProvider... providers) {
            this.pillarStartAttacks = providers;
            return this;
        }

        public Builder setUpStartAttack(StaticAnimationProvider provider) {
            this.pillarUp = provider;
            return this;
        }

        public Builder setChargePreAnimation(StaticAnimationProvider provider) {
            this.chargePre = provider;
            return this;
        }
    }
}
