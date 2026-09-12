package com.p1nero.wukong.epicfight.skill.custom.wukong;

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
import com.p1nero.wukong.epicfight.skill.WukongSkills;
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

// 戳棍重击技能: 处理戳棍棍式的重击/蓄力/衍生(退寸/进尺/搅棍)与棍势管理, 含击退与HUD绘制
public class ThrustHeavyAttack extends WeaponInnateSkill implements HeavyAttack {

    // 本技能事件监听器的唯一标识
    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a02b-0242ac114515");
    @NotNull protected final StaticAnimationProvider[] animations; // 0~4星戳棍重击动画
    @NotNull protected StaticAnimationProvider xuli_start; // 蓄力起手(蓄力)动画
    protected StaticAnimationProvider stepinch; // 退寸技动画
    protected StaticAnimationProvider footage; // 进尺(收棍)动画
    protected StaticAnimationProvider fengchuanhua; // 凤穿花动画
    protected StaticAnimationProvider chargePre; // 蓄力前摇动画
    protected StaticAnimationProvider juesick_start; // 搅棍起手动画
    protected StaticAnimationProvider juesick_loop; // 搅棍循环动画
    protected StaticAnimationProvider juesick_end; // 搅棍收尾动画
    protected StaticAnimationProvider jumpAttackHeavy; // 跳跃重击动画

    // 返回重击动画列表(含进尺/凤穿花), 用于判断当前是否处于重击状态
    @Override
    public List<StaticAnimationProvider> getHeavyAttacks() {
        List<StaticAnimationProvider> staticAnimations =
                new java.util.ArrayList<>(List.of(animations));
        staticAnimations.add(footage);
        staticAnimations.add(fengchuanhua);
        return staticAnimations;
    }

    // 创建技能构建器, 设为武器固有技能且无需消耗资源
    public static Builder createChargedAttack() {
        return new Builder().setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.NONE);
    }

    // 构造方法, 保存各类重击/衍生动画提供者
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

    // 在计时周期内使用技能才算使用衍生, 否则视为重击; 长按循环第一段衍生的判断在updateContainer
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();

        dataManager.setDataSync(
                WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack()); // 0星也是星

        // 4段棍势(含闪避保留棍势后)按重击直接释放凤穿花, 无需蓄力前摇, 与立棍4段直接释放的行为一致
        if (container.getStack() >= 4) {
            executer.playAnimationSynchronized(fengchuanhua.get(), 0F);
            resetConsumption(container, executer);
            super.executeOnServer(container, args);
            return;
        }

        if (dataManager.getDataValue(WukongSkillDataKeys.THRUST_FASHU_TIMER.get()) > 0) {
            // 铜头铁臂直接释放窗口: 跳过蓄力前摇, 直接释放当前星数的重击并清空全部棍势(4星走上方凤穿花分支)
            dataManager.setDataSync(WukongSkillDataKeys.THRUST_FASHU_TIMER.get(), 0);
            dataManager.setData(WukongSkillDataKeys.THRUST_PROTECT_NEXT_FALL.get(), true);
            executer.playSound(WuKongSounds.XULI_ATTACK_4.get(), 2, 2);
            executer.playAnimationSynchronized(
                    animations[container.getStack()].get(), 0.0F); // 有几星就几星重击
            resetConsumption(container, executer);
        } else if (dataManager.getDataValue(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get())
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

    // 注册各类事件监听: 命中加棍势/击退/退寸受击/四蓄窗口等
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
                                // 搅棍命中加10棍势
                                if (container.getStack() < 4) {
                                    container
                                            .getSkill()
                                            .setConsumptionSynchronize(
                                                    container, container.getResource() + 10.0F);
                                }
                            }
                            if (event.getAttackDamage() > 0.0) {
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
                                WukongSkills.gainResource(container, 30.0F); // 退寸获得30棍势
                                PacketRelay.sendToAll(
                                        PacketHandler.INSTANCE,
                                        new AddEntityAfterImageParticle(
                                                event.getPlayerPatch().getOriginal().getId()));
                                event.getPlayerPatch()
                                        .playSound(WuKongSounds.PERFECT_DODGE.get(), 0.5F, 0, 0);
                                event.setCanceled(true);
                            }
                        }));

        // 刷新四蓄计时器: 命中敌人即开启/刷新四蓄窗口, 窗口内不掉棍势, 窗口结束满4星降回3星并开始衰减(与劈棍/立棍/大圣统一)
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE,
                        EVENT_UUID,
                        (event -> {
                            container
                                    .getDataManager()
                                    .setDataSync(
                                            WukongSkillDataKeys.CHARGED4_TIMER.get(),
                                            Config.CHARGED4_WINDOW_TICKS.get().intValue());
                        }));

        super.onInitiate(container);
    }

    // 移除本技能注册的所有事件监听
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        PlayerEventListener listener = container.getExecutor().getEventListener();
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE, EVENT_UUID);
    }

    // 对攻击目标施加按距离归一化的击退(并点燃), 实现戳棍的击退效果
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

    // 每tick更新: 棍势/音效/蓄力释放/退寸/搅棍/四蓄掉棍势等
    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();
        if (container.getExecutor().isLogicalClient()) {
            boolean isKeyDown = EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown();
            dataManager.setDataSync(WukongSkillDataKeys.Thrust_KEY_PRESSING.get(), isKeyDown);

        } else {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            // 铜头铁臂成功格挡后加60棍势(与识破奖励一致), 跨段自动升星并保留多余棍势
            if (dataManager.getDataValue(WukongSkillDataKeys.THRUST_FASHU_STACK.get())) {
                WukongSkills.gainResource(container, 60.0F);
                dataManager.setDataSync(WukongSkillDataKeys.THRUST_FASHU_TIMER.get(), 30);
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
                // 蓄力的加成(每tick+1.5棍势)
                if (container.getStack() < 3) {
                    this.setConsumptionSynchronize(container, container.getResource() + 1.5F);
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
                // 重置可寸时机
                dataManager.setDataSync(WukongSkillDataKeys.Thrust_RETREAT_TIMER.get(), 30);
                // 松手了则播end
                if (!dataManager.getDataValue(WukongSkillDataKeys.IS_ATTACK_KEY_DOWN.get())) {
                    serverPlayerPatch.playAnimationSynchronized(juesick_end.get(), 0.0F);
                    dataManager.setDataSync(WukongSkillDataKeys.IS_REPEATING_DERIVE.get(), false);
                }
            }

            // 破条则加stack, 多余棍势保留给下一段(1星30%/2星50%/3星70%, 4星攒满120封顶)
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

            // 四蓄的掉棍势时间判断(与劈棍/立棍/大圣共用 CHARGED4_TIMER)
            int current = dataManager.getDataValue(WukongSkillDataKeys.CHARGED4_TIMER.get());
            if (current > 0) {
                dataManager.setDataSync(WukongSkillDataKeys.CHARGED4_TIMER.get(), current - 1);
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

    // 破条时增加1星, 多余棍势按当前段阈值(30%/50%/70%)扣减后保留给下一段
    public void breakProgress(ServerPlayerPatch serverPlayerPatch, SkillContainer container) {
        float threshold =
                (float)
                        (container.getMaxResource()
                                * (container.getStack() == 0
                                        ? 0.3
                                        : container.getStack() == 1 ? 0.5 : 0.7));
        this.setConsumptionSynchronize(container, container.getResource() - threshold);
        this.setStackSynchronize(container, container.getStack() + 1);
    }

    // 复制自yesman.epicfight.events.EntityEvents#attackEvent, 将伤害以部分伤害标签反弹给攻击者
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

    // 清空耐力并播红光和音效
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

    // 仅在持有有效棍武器时绘制技能图标
    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }

    // 根据棍式和星级绘制自定义战斗HUD, 完全重写Epic Fight默认技能图标绘制
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
                drawTexture(guiGraphics, goldenLightTexture, lightPos.x, lightPos.y);
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
                    drawTexture(guiGraphics, redLightTexture, lightPos.x, lightPos.y);
                }
            }
        }

        if (stack > 0) {
            for (int i = 0; i < Math.min(stack, 3); i++) {
                Vec2i lightPos = lightList.get(i);
                drawTexture(guiGraphics, whiteLightTexture, lightPos.x, lightPos.y);
            }
            drawTexture(guiGraphics, stackTexture, pos.x - 12, pos.y - 12);
        }
    }

    // 按48x48尺寸绘制指定贴图
    public void drawTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        guiGraphics.blit(texture, x, y, 48, 48, 0.0F, 0.0F, 256, 256, 256, 256);
    }

    // 技能构建器, 收集各类重击/衍生动画提供者
    public static class Builder extends SkillBuilder<ThrustHeavyAttack> {
        protected StaticAnimationProvider[] animationProviders; // 戳棍重击动画
        protected StaticAnimationProvider stepinch; // 退寸技动画
        protected StaticAnimationProvider footage; // 进尺(收棍)动画
        protected StaticAnimationProvider fengchuanhua; // 凤穿花动画
        protected StaticAnimationProvider jumpAttackHeavy; // 跳跃重击动画
        protected StaticAnimationProvider juesick_start; // 搅棍起手动画
        protected StaticAnimationProvider juesick_loop; // 搅棍循环动画
        protected StaticAnimationProvider juesick_end; // 搅棍收尾动画
        protected StaticAnimationProvider start; // 蓄力起手动画

        StaticAnimationProvider pre; // 蓄力前摇动画

        public Builder() {}

        // 设置技能分类
        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        // 设置激活类型
        public Builder setActivateType(Skill.ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        // 设置消耗资源
        public Builder setResource(Skill.Resource resource) {
            this.resource = resource;
            return this;
        }

        // 设置创造模式标签页
        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }

        // 设置蓄力前摇动画
        public Builder setChargePreAnimation(StaticAnimationProvider pre) {
            this.pre = pre;
            return this;
        }

        // 设置退寸/进尺/凤穿花/搅棍等衍生动画(可长按衍生的首个即起手, 具体在动画处判断)
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

        // 设置0~4星戳棍重击动画
        public Builder setHeavyAttacks(StaticAnimationProvider... animationProviders) {
            this.animationProviders = animationProviders;
            return this;
        }

        // 设置蓄力起手动画
        public Builder setStartAttacks(StaticAnimationProvider start) {
            this.start = start;
            return this;
        }

        // 设置跳跃重击动画
        public Builder setJumpAttackHeavy(StaticAnimationProvider jumpAttackHeavy) {
            this.jumpAttackHeavy = jumpAttackHeavy;
            return this;
        }
    }
}
