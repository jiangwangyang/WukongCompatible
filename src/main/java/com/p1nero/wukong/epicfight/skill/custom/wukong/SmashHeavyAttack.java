package com.p1nero.wukong.epicfight.skill.custom.wukong;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.animation.custom.WukongDodgeAnimation;
import com.p1nero.wukong.epicfight.compat.EpicFightDamageType;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.WukongSkills;
import com.p1nero.wukong.epicfight.skill.custom.avatar.HeavyAttack;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.item.WukongItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.ValueModifier;
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
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.List;
import java.util.UUID;

// 劈棍重击技能: 处理劈棍棍式的重击/蓄力/衍生(破棍式/跳重击)与棍势管理, 含伤害倍率与HUD绘制
public class SmashHeavyAttack extends WeaponInnateSkill implements HeavyAttack {

    // 本技能事件监听器的唯一标识
    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0242ac114512");
    @NotNull protected final StaticAnimationProvider[] animations; // 0~4星劈棍重击动画
    protected StaticAnimationProvider deriveAnimation1; // 一段衍生(破棍式)动画
    protected StaticAnimationProvider deriveAnimation2; // 二段衍生动画
    protected StaticAnimationProvider deriveAnimation3; // 大圣套装专属三段衍生动画
    @NotNull protected StaticAnimationProvider jumpAttackHeavy; // 跳跃重击动画
    @NotNull protected StaticAnimationProvider chargePre; // 蓄力前摇动画

    // 返回重击动画列表(含二段衍生), 用于判断当前是否处于重击状态
    @Override
    public List<StaticAnimationProvider> getHeavyAttacks() {
        List<StaticAnimationProvider> staticAnimations =
                new java.util.ArrayList<>(List.of(animations));
        staticAnimations.add(deriveAnimation2);
        return staticAnimations;
    }

    // 创建技能构建器, 设为武器固有技能且无需消耗资源
    public static Builder createChargedAttack() {
        return new Builder().setCategory(SkillCategories.WEAPON_INNATE).setResource(Resource.NONE);
    }

    // 构造方法, 保存各类重击/衍生动画提供者
    public SmashHeavyAttack(Builder builder) {
        super(builder);
        chargePre = builder.pre;

        this.animations = builder.animationProviders;
        deriveAnimation1 = builder.derive1;
        deriveAnimation2 = builder.derive2;
        deriveAnimation3 = builder.derive3;
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
        if (dataManager.getDataValue(WukongSkillDataKeys.CAN_JUMP_HEAVY.get())
                && !player.onGround()) {
            dataManager.setData(
                    WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true); // 放里面, 防止瞎按技能键就防坠机的bug
            // 跳跃攻击, 也消所有棍势
            dataManager.setDataSync(WukongSkillDataKeys.CAN_JUMP_HEAVY.get(), false);
            if (container.getStack() > 0) { // 0星是null会中空
                // executer.playSound(WuKongSounds.stackSounds.get(container.getStack() -
                // 1).get(), 1, 1);
            }
            executer.playAnimationSynchronized(jumpAttackHeavy.get(), 0.15F);
            resetConsumption(container, executer, false);
        } else if (player.onGround()) {
            // 如果用了星则要强化衍生
            boolean stackConsumed = container.getStack() > 0;
            if (dataManager.getDataValue(WukongSkillDataKeys.SMASH_FASHU_TIMER.get()) > 0) {
                // 铜头铁臂直接释放窗口: 跳过蓄力前摇, 直接释放当前星数的重击并清空全部棍势
                dataManager.setDataSync(WukongSkillDataKeys.SMASH_FASHU_TIMER.get(), 0);
                dataManager.setData(
                        WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
                executer.playSound(WuKongSounds.XULI_ATTACK_4.get(), 2, 2);
                executer.playAnimationSynchronized(
                        animations[container.getStack()].get(), 0.0F); // 有几星就几星重击
                resetConsumption(container, executer, true);
            } else if (dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) > 0
                    && !container.isFull()) { // 有星才能用破棍式, 且满星直接放大(也防bug)
                if (dataManager.getDataValue(WukongSkillDataKeys.CAN_FIRST_DERIVE.get())) {
                    dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
                    executer.playSound(
                            WuKongSounds.stackSounds.get(container.getStack() - 1).get(), 1, 1);
                    this.setStackSynchronize(container, container.getStack() - 1);
                    executer.playAnimationSynchronized(deriveAnimation1.get(), 0.2F);
                } else if (dataManager.getDataValue(WukongSkillDataKeys.CAN_SECOND_DERIVE.get())) {
                    dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true);
                    executer.playSound(
                            WuKongSounds.stackSounds.get(container.getStack() - 1).get(), 1, 1);
                    this.setStackSynchronize(container, container.getStack() - 1);
                    executer.playAnimationSynchronized(deriveAnimation2.get(), 0.2F);
                }
            } else if (container.isFull()
                    && deriveAnimation3 != null
                    && isWearingGreatSageSet(player)) {
                executer.playAnimationSynchronized(deriveAnimation3.get(), 0.2F);
            } else {
                // 重击, 消耗所有星, 开始蓄力, 松手在客户端判断
                if (!dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get())) {
                    executer.playAnimationSynchronized(chargePre.get(), 0.2F);
                }
            }
        }

        super.executeOnServer(container, args);
    }

    // 判断玩家是否穿戴全套大圣套装(用于解锁三段衍生)
    private static boolean isWearingGreatSageSet(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(WukongItems.DASHENG_H.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(WukongItems.DASHENG_C.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(WukongItems.DASHENG_L.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(WukongItems.DASHENG_F.get());
    }

    // 清空耐力并播红光和音效(playSound为false时通过蓄力释放则不播放音效)
    private void resetConsumption(
            SkillContainer container, ServerPlayerPatch executer, boolean playSound) {
        if (playSound && container.getStack() > 0) {
            int soundIndex = Math.min(container.getStack(), WuKongSounds.stackSounds.size()) - 1;
            executer.playSound(WuKongSounds.stackSounds.get(soundIndex).get(), 1, 1);
        } else {
            container.getDataManager().setDataSync(WukongSkillDataKeys.PLAY_SOUND.get(), true);
        }
        container
                .getDataManager()
                .setDataSync(
                        WukongSkillDataKeys.RED_TIMER.get(),
                        Config.DERIVE_CHECK_TIME.get().intValue()); // 通知客户端该亮红灯了
        this.setStackSynchronize(container, 0);
        this.setConsumptionSynchronize(container, 1);
    }

    // 注册各类事件监听: 禁跳/识破加棍势/减伤霸体/坠机保护/衍生计时/伤害倍率等
    @Override
    public void onInitiate(SkillContainer container) {

        // 长按期间禁止跳跃
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT,
                        EVENT_UUID,
                        (event -> {
                            if (event.getPlayerPatch().isEpicFightMode()
                                    && EpicFightKeyMappings.WEAPON_INNATE_SKILL.isDown()) {
                                // 弃用API迁移: getMovementInput()改为getInputState(),
                                // 通过InputManager.setInputState应用回原版输入(已核实与直接改Input字段等价)
                                InputManager.setInputState(
                                        event.getInputState().withJumping(false));
                            }
                        }));

        // 成功识破加棍势, 并重置普攻计数器, 下次从三段普攻开始
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK,
                        EVENT_UUID,
                        (event -> {
                            if (event.getDamageSource()
                                            instanceof EpicFightDamageSource epicFightDamageSource
                                    && epicFightDamageSource.is(EpicFightDamageType.PARTIAL_DAMAGE))
                                return;
                            if (container
                                    .getDataManager()
                                    .getDataValue(WukongSkillDataKeys.IS_IN_SPECIAL_ATTACK.get())) {
                                if (!container
                                        .getDataManager()
                                        .getDataValue(
                                                WukongSkillDataKeys.IS_SPECIAL_SUCCESS.get())) {
                                    WukongSkills.gainResource(container, 60.0F); // 识破获得60棍势
                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.IS_SPECIAL_SUCCESS.get(),
                                                    true);
                                }
                                BasicAttack.setComboCounterWithEvent(
                                        ComboCounterHandleEvent.Causal.ANOTHER_ACTION_ANIMATION,
                                        event.getPlayerPatch(),
                                        event.getPlayerPatch().getSkill(SkillSlots.BASIC_ATTACK),
                                        deriveAnimation1.get(),
                                        2);
                                event.setCanceled(true);
                                event.setCanceled(true);
                            }

                            float damageReduce =
                                    container
                                            .getDataManager()
                                            .getDataValue(WukongSkillDataKeys.DAMAGE_REDUCE.get());
                            // 霸体
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

                            // 防止坠机 FIXME
                            if (event.getDamageSource().is(DamageTypes.FALL)
                                    && container
                                            .getDataManager()
                                            .getDataValue(
                                                    WukongSkillDataKeys.PROTECT_NEXT_FALL.get())) {
                                System.out.println("man!");
                                event.setCanceled(true);
                                event.setCanceled(true);
                                event.setResult(AttackResult.ResultType.MISSED);
                                event.getPlayerPatch().getOriginal().resetFallDistance();
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

                            // 蓄力的时候做动作是非法的, 应该清空棍势
                            if (container
                                            .getDataManager()
                                            .getDataValue(WukongSkillDataKeys.IS_CHARGING.get())
                                    && !event.getAnimation().equals(chargePre.get())
                                    && !(event.getAnimation().get()
                                            instanceof WukongDodgeAnimation)) {
                                this.setConsumptionSynchronize(container, 1);
                                this.setStackSynchronize(container, 0);
                                container
                                        .getDataManager()
                                        .setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false);
                            }

                            // 普攻后立即右键可以衍生
                            var autoAnimations =
                                    capabilityItem.getAutoAttackMotion(event.getPlayerPatch());
                            for (int i = 0; i < autoAnimations.size(); i++) {
                                if (autoAnimations.get(i).equals(event.getAnimation()) && i < 4) {
                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.CAN_FIRST_DERIVE.get(),
                                                    true);
                                    container
                                            .getDataManager()
                                            .setDataSync(
                                                    WukongSkillDataKeys.DERIVE_TIMER.get(),
                                                    Config.DERIVE_CHECK_TIME.get().intValue());
                                    return;
                                }
                            }
                        }));

        // 命中敌人刷新四蓄计时器, 识破打中则可接二段
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE,
                        EVENT_UUID,
                        (event -> {
                            ServerPlayer player = event.getPlayerPatch().getOriginal();
                            // 命中敌人即开启/刷新四蓄窗口, 窗口内不掉棍势, 窗口结束满4星降回3星并开始衰减
                            container
                                    .getDataManager()
                                    .setDataSync(
                                            WukongSkillDataKeys.CHARGED4_TIMER.get(),
                                            Config.CHARGED4_WINDOW_TICKS.get().intValue());
                            if (event.getDamageSource()
                                    .getAnimation()
                                    .equals(deriveAnimation1.get())) {
                                container
                                        .getDataManager()
                                        .setDataSync(
                                                WukongSkillDataKeys.CAN_SECOND_DERIVE.get(), true);
                                container
                                        .getDataManager()
                                        .setDataSync(
                                                WukongSkillDataKeys.DERIVE_TIMER.get(),
                                                Config.DERIVE_CHECK_TIME.get().intValue());
                            }
                        }));

        // 根据星数改跳跃重击和破斩棍式伤害
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK,
                        EVENT_UUID,
                        (event -> {
                            int starCnt =
                                    container
                                            .getDataManager()
                                            .getDataValue(WukongSkillDataKeys.STARS_CONSUMED.get());
                            if (event.getDamageSource()
                                    .getAnimation()
                                    .equals(jumpAttackHeavy.get())) {
                                float mul =
                                        switch (starCnt) {
                                            case 1 -> 3;
                                            case 2 -> 4.5F;
                                            case 3 -> 6.2F;
                                            case 4 -> 8.75F;
                                            default -> 1.45F;
                                        };
                                event.getDamageSource()
                                        .attachDamageModifier(ValueModifier.multiplier(mul));
                            } else if (event.getDamageSource()
                                    .getAnimation()
                                    .equals(deriveAnimation1.get())) {
                                float mul = starCnt == 0 ? 1.0F : 1.96F;
                                event.getDamageSource()
                                        .attachDamageModifier(ValueModifier.multiplier(mul));
                            } else if (event.getDamageSource()
                                    .getAnimation()
                                    .equals(deriveAnimation2.get())) {
                                float mul =
                                        switch (starCnt) {
                                            case 1 -> 4.7F;
                                            case 2 -> 4.9F;
                                            case 3, 4 -> 5.1F;
                                            default -> 4.48F;
                                        };
                                event.getDamageSource()
                                        .attachDamageModifier(ValueModifier.multiplier(mul));
                            }
                            // 对地的敌人不施加硬直
                            event.getTarget()
                                    .getCapability(EpicFightCapabilities.CAPABILITY_ENTITY)
                                    .ifPresent(
                                            entityPatch -> {
                                                if (entityPatch
                                                        instanceof
                                                        LivingEntityPatch<?> livingEntityPatch) {
                                                    if (livingEntityPatch
                                                            .getEntityState()
                                                            .knockDown()) {
                                                        event.getDamageSource()
                                                                .setStunType(StunType.NONE);
                                                    }
                                                }
                                            });
                        }));

        super.onInitiate(container);
    }

    // 移除本技能注册的所有事件监听
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        PlayerEventListener listener = container.getExecutor().getEventListener();
        listener.removeListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        listener.removeListener(PlayerEventListener.EventType.FALL_EVENT, EVENT_UUID);
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

    // 每tick更新: 棍势/音效/跳重击判定/蓄力释放/四蓄掉棍势等
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

            // 铜头铁臂成功格挡后加60棍势(与识破奖励一致), 跨段自动升星并保留多余棍势, 并解锁30tick直接释放窗口
            if (dataManager.getDataValue(WukongSkillDataKeys.SMASH_FASHU_STACK.get())) {
                WukongSkills.gainResource(container, 60.0F);
                dataManager.setDataSync(WukongSkillDataKeys.SMASH_FASHU_TIMER.get(), 30);
                dataManager.setDataSync(WukongSkillDataKeys.SMASH_FASHU_STACK.get(), false);
            }
            dataManager.setDataSync(
                    WukongSkillDataKeys.SMASH_FASHU_TIMER.get(),
                    Math.max(
                            dataManager.getDataValue(WukongSkillDataKeys.SMASH_FASHU_TIMER.get())
                                    - 1,
                            0)); // 铜头铁臂直接释放窗口计时

            // 层数变化检测以播放音效
            if (container.getStack()
                    > dataManager.getDataValue(WukongSkillDataKeys.LAST_STACK.get())) {
                serverPlayerPatch.playSound(
                        WuKongSounds.XULI_LEVEL.get(container.getStack() - 1).get(), 1, 1);
                dataManager.setDataSync(WukongSkillDataKeys.PLAY_SOUND.get(), false);
            }
            dataManager.setData(WukongSkillDataKeys.LAST_STACK.get(), container.getStack());

            // 跳重击的判断
            if (!serverPlayer.onGround()) {
                dataManager.setDataSync(WukongSkillDataKeys.CAN_JUMP_HEAVY.get(), true);
            } else if (dataManager.getDataValue(WukongSkillDataKeys.CAN_JUMP_HEAVY.get())) {
                dataManager.setDataSync(WukongSkillDataKeys.CAN_JUMP_HEAVY.get(), false);
            }

            // 更新计时器
            dataManager.setDataSync(
                    WukongSkillDataKeys.RED_TIMER.get(),
                    Math.max(
                            dataManager.getDataValue(WukongSkillDataKeys.RED_TIMER.get()) - 1,
                            0)); // 使用技能星数显示
            dataManager.setDataSync(
                    WukongSkillDataKeys.DERIVE_TIMER.get(),
                    Math.max(
                            dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) - 1,
                            0)); // 切手技有效时间计算
            if (dataManager.getDataValue(WukongSkillDataKeys.DERIVE_TIMER.get()) <= 0) {
                dataManager.setDataSync(WukongSkillDataKeys.CAN_FIRST_DERIVE.get(), false);
                dataManager.setDataSync(WukongSkillDataKeys.CAN_SECOND_DERIVE.get(), false);
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
                if (container.getStack() < 3) {
                    this.setConsumptionSynchronize(container, container.getResource() + 1.5F);
                }
                // 松手则清空棍势打重击
                if (!dataManager.getDataValue(WukongSkillDataKeys.KEY_PRESSING.get())) {
                    dataManager.setDataSync(WukongSkillDataKeys.IS_CHARGING.get(), false);
                    dataManager.setData(WukongSkillDataKeys.PROTECT_NEXT_FALL.get(), true); // MAN
                    serverPlayerPatch.playSound(WuKongSounds.XULI_ATTACK_4.get(), 2, 2);
                    serverPlayerPatch.playAnimationSynchronized(
                            animations[container.getStack()].get(), 0.0F); // 有几星就几星重击
                    dataManager.setDataSync(
                            WukongSkillDataKeys.STARS_CONSUMED.get(),
                            container.getStack()); // 设置消星数, 方便客户端绘制
                    resetConsumption(container, serverPlayerPatch, true);
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

    // 返回自身, 将技能属性绑定到动画
    @Override
    public WeaponInnateSkill registerPropertiesToAnimation() {
        return this;
    }

    // 技能构建器, 收集各类重击/衍生动画提供者
    public static class Builder extends SkillBuilder<SmashHeavyAttack> {
        protected StaticAnimationProvider[] animationProviders; // 劈棍重击动画
        protected StaticAnimationProvider derive1; // 一段衍生(破棍式)动画
        protected StaticAnimationProvider derive2; // 二段衍生动画
        protected StaticAnimationProvider derive3; // 大圣套装专属三段衍生动画
        protected StaticAnimationProvider jumpAttackHeavy; // 跳跃重击动画
        StaticAnimationProvider chargingAnimation; // 蓄力动画(占位, 当前未使用)
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

        // 设置蓄力动画
        public Builder setChargingAnimation(StaticAnimationProvider chargingAnimation) {
            this.chargingAnimation = chargingAnimation;
            return this;
        }

        // 设置蓄力前摇动画
        public Builder setChargePreAnimation(StaticAnimationProvider pre) {
            this.pre = pre;
            return this;
        }

        // 设置0~4星劈棍重击动画
        public Builder setHeavyAttacks(StaticAnimationProvider... animationProviders) {
            this.animationProviders = animationProviders;
            return this;
        }

        // 设置破棍式等衍生动画(可长按衍生的derive1即pre动画, 具体在动画处判断)
        public Builder setDeriveAnimations(
                StaticAnimationProvider derive1,
                StaticAnimationProvider derive2,
                StaticAnimationProvider derive3) {
            this.derive1 = derive1;
            this.derive2 = derive2;
            this.derive3 = derive3;
            return this;
        }

        // 设置跳跃重击动画
        public Builder setJumpAttackHeavy(StaticAnimationProvider jumpAttackHeavy) {
            this.jumpAttackHeavy = jumpAttackHeavy;
            return this;
        }
    }
}
