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
import com.p1nero.wukong.network.packet.client.PillarFovPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 立棍重击技能: 处理立棍棍式的重击/蓄力/衍生(风云转/江海翻)与棍势管理, 含FOV动画与HUD绘制
public class PillarHeavyAttack extends WeaponInnateSkill implements HeavyAttack {

    // 本技能事件监听器的唯一标识
    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0242ac114513");
    // 立棍增高(升星)动画
    protected final StaticAnimationProvider[] up;
    // 立棍起手动画
    protected final StaticAnimationProvider[] start; // 立起
    // 0~4星立棍重击动画
    protected final StaticAnimationProvider[] heavy;

    // 一段衍生(风云转)动画
    protected StaticAnimationProvider deriveAnimation1;
    // 二段衍生(江海翻)动画
    protected StaticAnimationProvider deriveAnimation2;
    // 风火轮衍生动画
    protected StaticAnimationProvider hotwheel;
    // 衍生收尾动画
    protected StaticAnimationProvider deriveEnd;

    // 创建技能构建器, 设为武器固有技能且无需消耗资源
    public static Builder createChargedAttack() {
        return new Builder().setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.NONE);
    }

    // 构造方法, 保存各类重击/衍生动画提供者
    public PillarHeavyAttack(Builder builder) {
        super(builder);
        this.start = builder.start;
        this.up = builder.up;
        this.heavy = builder.heavy;

        deriveAnimation1 = builder.derive1;
        deriveAnimation2 = builder.derive2;
        hotwheel = builder.hotwheel;
        deriveEnd = builder.deriveEnd;
    }

    // 返回重击动画列表(含二段衍生), 用于判断当前是否处于重击状态
    @Override
    public List<StaticAnimationProvider> getHeavyAttacks() {
        List<StaticAnimationProvider> staticAnimations = new ArrayList<>(List.of(heavy));
        staticAnimations.add(deriveAnimation2);
        return staticAnimations;
    }

    // 在计时周期内使用技能才算使用衍生, 否则视为重击; 长按循环第一段衍生的判断在updateContainer
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executer = container.getServerExecutor();
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executer.getOriginal();
        dataManager.setDataSync(
                WukongSkillDataKeys.STARS_CONSUMED.get(), container.getStack()); // 0星也是星
        boolean stackConsumed = container.getStack() > 0;
        if (container.getStack() == 4) {
            dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
            if (container.getStack() > 0) {
                sendFovAnimation(player, 7F, 31, container.getStack());
            }
            executer.playAnimationSynchronized(start[container.getStack()].get(), 0F);
        } else if (dataManager.getDataValue(WukongSkillDataKeys.PILLAR_FASHU_TIMER.get()) > 0) {
            // 铜头铁臂直接释放窗口: 跳过爬棍蓄力前摇, 直接释放当前星数的砸下重击并清空全部棍势
            dataManager.setDataSync(WukongSkillDataKeys.PILLAR_FASHU_TIMER.get(), 0);
            dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
            executer.playSound(WuKongSounds.XULI_ATTACK_4.get(), 2, 2);
            executer.playAnimationSynchronized(heavy[container.getStack()].get(), 0.0F); // 有几星就几星重击
            resetConsumption(container, executer);
        } else if (dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) > 0
                && stackConsumed) {
            dataManager.setData(WukongSkillDataKeys.PILLAR_FENG_YU_ZHUAN.get(), true);
            executer.playSound(WuKongSounds.stackSounds.get(container.getStack() - 1).get(), 1, 1);
            dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
            executer.playAnimationSynchronized(deriveAnimation1.get(), 0F);
            dataManager.setData(WukongSkillDataKeys.DERIVE_TIMER.get(), 0);
            this.setStackSynchronize(container, container.getStack() - 1);
        } else if (dataManager.getDataValue(WukongSkillDataKeys.PILLAR_JIANGHAIFAN_TIMER.get()) > 0
                && stackConsumed) {
            dataManager.setDataSync(WukongSkillDataKeys.PILLAR_JIANGHAIFAN_TIMER.get(), 0);
            executer.playSound(WuKongSounds.stackSounds.get(container.getStack() - 1).get(), 1, 1);
            this.setStackSynchronize(container, container.getStack() - 1);
            executer.playAnimationSynchronized(deriveAnimation2.get(), 0F);
        } else {
            // 重击开始蓄力
            if (!dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())
                    && checkSpace(player, container.getStack() * 2)) {
                dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
                if (container.getStack() > 0) {
                    sendFovAnimation(player, 7F, 31, container.getStack());
                }
                executer.playAnimationSynchronized(start[container.getStack()].get(), 0F);
            }
        }
        super.executeOnServer(container, args);
    }

    // 向玩家发送立棍蓄力FOV(视野)变化动画数据包
    private static void sendFovAnimation(
            ServerPlayer player, float increaseAmount, int durationTicks, int repeatTimes) {
        PacketRelay.sendToPlayer(
                PacketHandler.INSTANCE,
                new PillarFovPacket(increaseAmount, durationTicks, Math.max(1, repeatTimes)),
                player);
    }

    // 释放后清空棍势(星数)并重置耐力
    private void resetConsumption(SkillContainer container, ServerPlayerPatch executer) {
        if (container.getStack() > 0) {
            int cnt = container.getStack();
        }
        this.setStackSynchronize(container, 0);
        this.setConsumptionSynchronize(container, 1);
    }

    // 注册各类事件监听: 风云转加棍势/移动输入禁跳/减伤霸体/坠机保护/衍生计时等
    @Override
    public void onInitiate(SkillContainer container) {
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK,
                        EVENT_UUID,
                        (event -> {
                            if (event.getPlayerPatch()
                                    .getAnimator()
                                    .getPlayerFor(null)
                                    .getAnimation()
                                    .equals(WukongAnimations.PILLAR_HEAVY_FENGYUNZHUAN.get())) {
                                // 风云转命中加5棍势
                                if (container.getStack() < 4) {
                                    container
                                            .getSkill()
                                            .setConsumptionSynchronize(
                                                    container, container.getResource() + 5.0F);
                                }
                            }
                        }));

        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT,
                        EVENT_UUID,
                        (event -> {
                            // 长按期间禁止跳跃
                            if (event.getPlayerPatch().isEpicFightMode()
                                    && EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown()) {
                                // 弃用API迁移: getMovementInput()改为getInputState(),
                                // 通过InputManager.setInputState应用回原版输入(已核实与直接改Input字段等价)
                                InputManager.setInputState(
                                        event.getInputState().withJumping(false));
                            }
                            // 蓄力期间禁用移动
                            if (event.getPlayerPatch().isEpicFightMode()
                                    && EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown()) {
                                // 弃用API迁移: 同上,
                                // 并以KeyMapping.setDown替代已弃用的ControlEngine.setKeyBind(其内部就是该调用)
                                InputManager.setInputState(
                                        event.getInputState()
                                                .withForwardImpulse(0.0F)
                                                .withLeftImpulse(0.0F)
                                                .withDown(false)
                                                .withUp(false)
                                                .withLeft(false)
                                                .withRight(false)
                                                .withJumping(false)
                                                .withSneaking(false));
                                LocalPlayer clientPlayer = event.getPlayerPatch().getOriginal();
                                clientPlayer.setSprinting(false);
                                clientPlayer.sprintTriggerTime = -1;
                                Minecraft mc = Minecraft.getInstance();
                                mc.options.keySprint.setDown(false);
                            }
                        }));
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
                                                    WukongSkillDataKeys.PROTECT_NEXT_FALL.get())) {
                                event.setCanceled(true);
                                event.setResult(AttackResult.ResultType.MISSED);
                                event.getPlayerPatch().getOriginal().resetFallDistance();
                                container
                                        .getDataManager()
                                        .setData(
                                                WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), false);
                            }
                            // 霸体减伤
                            if (event.getDamageSource()
                                            instanceof EpicFightDamageSource epicFightDamageSource
                                    && epicFightDamageSource.is(EpicFightDamageType.PARTIAL_DAMAGE))
                                return;
                            float damageReduce =
                                    container
                                            .getDataManager()
                                            .getDataValue(WukongSkillDataKeys.DAMAGE_REDUCE.get());
                            if (damageReduce > 0) {
                                if (event.getDamageSource()
                                        instanceof EpicFightDamageSource epicFightDamageSource) {
                                    epicFightDamageSource.setStunType(StunType.NONE);
                                }

                                LivingEntityPatch<?> attackerPatch =
                                        EpicFightCapabilities.getEntityPatch(
                                                event.getDamageSource().getEntity(),
                                                LivingEntityPatch.class);
                                this.processDamage(
                                        event.getPlayerPatch(),
                                        event.getDamageSource(),
                                        AttackResult.ResultType.SUCCESS,
                                        (1 - damageReduce) * event.getDamage(),
                                        attackerPatch);
                                event.setResult(AttackResult.ResultType.MISSED);
                                event.setCanceled(true);
                            }
                            // 防止坠机

                            if (event.getDamageSource().is(DamageTypes.FALL)
                                    && container
                                            .getDataManager()
                                            .getDataValue(
                                                    WukongSkillDataKeys.PROTECT_NEXT_FALL.get())) {
                                LivingEntityPatch<?> attackerPatch =
                                        EpicFightCapabilities.getEntityPatch(
                                                event.getDamageSource().getEntity(),
                                                LivingEntityPatch.class);
                                this.processDamage(
                                        event.getPlayerPatch(),
                                        event.getDamageSource(),
                                        AttackResult.ResultType.SUCCESS,
                                        event.getDamage() * 0.4F,
                                        attackerPatch);
                                event.setResult(AttackResult.ResultType.BLOCKED);
                                event.setCanceled(true);
                                container
                                        .getDataManager()
                                        .setData(
                                                WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), false);
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
                            // 普攻后立即右键可以衍生
                            var autoAnimations =
                                    capabilityItem.getAutoAttackMotion(event.getPlayerPatch());
                            for (int i = 0; i < autoAnimations.size(); i++) {
                                if (autoAnimations.get(i).equals(event.getAnimation()) && i < 4) {
                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.DERIVE_TIMER.get(),
                                                    Config.DERIVE_CHECK_TIME.get().intValue());
                                    return;
                                }
                            }
                        }));

        // 刷新四蓄计时器: 命中敌人即开启/刷新四蓄窗口, 窗口内不掉棍势, 窗口结束满4星降回3星并开始衰减(与劈棍/戳棍/大圣统一)
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
        listener.removeListener(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE, EVENT_UUID);
    }

    // 判断玩家头顶height格高度内是否均为空气(即空间足够立棍)
    public static boolean checkSpace(ServerPlayer serverPlayer, int height) {
        // 获取玩家所在的服务器世界
        ServerLevel serverLevel = serverPlayer.serverLevel();
        // 循环检测玩家头顶`height` 高度内的每个位置
        for (int i = 1; i <= height; i++) {
            // 检测玩家当前所在位置上 `i` 个单位的方块状态
            if (!serverLevel.getBlockState(serverPlayer.getOnPos().above(i)).is(Blocks.AIR)) {
                // 如果不是空气, 返回 false
                return false;
            }
        }

        // 如果检测完所有高度后都为空气, 则返回 true
        return true;
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

    // 每tick更新: 棍势/音效/蓄力释放/各类计时器/四蓄掉棍势等
    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();
        if (container.getExecutor().isLogicalClient()) {
            // KEY_PRESSING用于服务端判断是否继续播动画
            boolean isKeyDown = EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown();
            dataManager.setDataSync(WukongSkillDataKeys.KEY_PRESSING.get(), isKeyDown);
        } else {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();
            // 层数变化检测以播放音效
            if (container.getStack()
                    > dataManager.getDataValue(WukongSkillDataKeys.LAST_STACK.get())) {
                serverPlayerPatch.playSound(
                        WuKongSounds.XULI_LEVEL.get(container.getStack() - 1).get(), 1, 1);
                dataManager.setDataSync(WukongSkillDataKeys.PLAY_SOUND.get(), false);
                if (dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())) {
                    if (!dataManager.getDataValue(WukongSkillDataKeys.PILLAR_FENG_YU_ZHUAN.get())
                            && dataManager.getDataValue(WukongSkillDataKeys.LAST_STACK.get()) < 3) {
                        serverPlayerPatch.playAnimationSynchronized(
                                up[dataManager.getDataValue(WukongSkillDataKeys.LAST_STACK.get())]
                                        .get(),
                                0.1F);
                    }
                }
            }
            dataManager.setData(WukongSkillDataKeys.LAST_STACK.get(), container.getStack());
            // 更新计时器
            dataManager.setDataSync(
                    WukongSkillDataKeys.PILLAR_FASHU_TIMER.get(),
                    Math.max(
                            dataManager.getDataValue(WukongSkillDataKeys.PILLAR_FASHU_TIMER.get())
                                    - 1,
                            0)); // 派生重击
            dataManager.setDataSync(
                    WukongSkillDataKeys.RED_TIMER.get(),
                    Math.max(
                            dataManager.getDataValue(WukongSkillDataKeys.RED_TIMER.get()) - 1,
                            0)); // 使用技能星数显示
            // 铜头铁臂成功格挡后加60棍势(与识破奖励一致), 跨段自动升星并保留多余棍势
            if (dataManager.getDataValue(WukongSkillDataKeys.PILLAR_FASHU_STACK.get())) {
                WukongSkills.gainResource(container, 60.0F);
                dataManager.setDataSync(WukongSkillDataKeys.PILLAR_FASHU_TIMER.get(), 30);
                dataManager.setDataSync(WukongSkillDataKeys.PILLAR_FASHU_STACK.get(), false);
            }

            if (dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) > 0) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.DERIVE_TIMER.get(),
                        dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get())
                                - 1); // 切手技有效时间计算
            }
            if (dataManager.getDataValue(WukongSkillDataKeys.PILLAR_JIANGHAIFAN_TIMER.get()) > 0) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.PILLAR_JIANGHAIFAN_TIMER.get(),
                        dataManager.getDataValue(WukongSkillDataKeys.PILLAR_JIANGHAIFAN_TIMER.get())
                                - 1); // 切手技有效时间计算
            }

            if (dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())) {
                // 防止切物品产生的bug
                if (!WukongWeaponCategories.isWeaponValid(serverPlayerPatch)) {
                    dataManager.setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false);
                    this.setConsumptionSynchronize(container, 1);
                    this.setStackSynchronize(container, 0);
                    return;
                }
                // 蓄力的加成(每tick+1.5棍势)
                if (container.getStack() < 3
                        && dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())) {
                    this.setConsumptionSynchronize(container, container.getResource() + 1.5F);
                }
                if (!dataManager.getDataValue(WukongSkillDataKeys.KEY_PRESSING.get())) {

                    dataManager.setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false);
                    dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true); // MAN
                    serverPlayerPatch.playSound(WuKongSounds.XULI_ATTACK_4.get(), 2, 2);

                    serverPlayerPatch.playAnimationSynchronized(
                            heavy[container.getStack()].get(), 0.0F); // 有几星就几星重击
                    dataManager.setDataSync(
                            WukongSkillDataKeys.STARS_CONSUMED.get(),
                            container.getStack()); // 设置消星数, 方便客户端绘制
                    resetConsumption(container, serverPlayerPatch);
                }
            }

            if (dataManager.getDataValue(WukongSkillDataKeys.PILLAR_FENG_YU_ZHUAN.get())) {
                if (!dataManager.getDataValue(WukongSkillDataKeys.KEY_PRESSING.get())) {
                    dataManager.setDataSync(WukongSkillDataKeys.PILLAR_FENG_YU_ZHUAN.get(), false);
                    serverPlayerPatch.playAnimationSynchronized(deriveEnd.get(), 0.0F);
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
            // 四蓄的掉棍势时间判断
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
    public static class Builder extends SkillBuilder<PillarHeavyAttack> {
        protected StaticAnimationProvider[] start; // 立棍起手动画
        protected StaticAnimationProvider[] up; // 立棍增高(升星)动画
        protected StaticAnimationProvider[] heavy; // 立棍重击动画
        protected StaticAnimationProvider derive1; // 一段衍生(风云转)动画
        protected StaticAnimationProvider derive2; // 二段衍生(江海翻)动画
        protected StaticAnimationProvider deriveEnd; // 衍生收尾动画
        protected StaticAnimationProvider hotwheel; // 风火轮衍生动画

        StaticAnimationProvider pre; // 蓄力前摇动画

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

        // 设置消耗资源
        public Builder setResource(Resource resource) {
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

        // 设置立棍起手动画
        public Builder setStartAnimations(StaticAnimationProvider... animationProviders) {
            this.start = animationProviders;
            return this;
        }

        // 设置立棍增高(0~4星)动画
        public Builder setUpAnimations(StaticAnimationProvider... animationProviders) {
            this.up = animationProviders;
            return this;
        }

        // 设置0~4星立棍重击动画
        public Builder setHeavyAttacks(StaticAnimationProvider... animationProviders) {
            this.heavy = animationProviders;
            return this;
        }

        // 设置风云转/江海翻等衍生动画
        public Builder setDeriveAnimations(
                StaticAnimationProvider derivePre,
                StaticAnimationProvider deriveEnd,
                StaticAnimationProvider derive2) {
            this.derive1 = derivePre;
            this.deriveEnd = deriveEnd;
            this.derive2 = derive2;
            return this;
        }
    }
}
