package com.p1nero.wukong.epicfight.animation;

import com.p1nero.wukong.Config;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.entity.FakeWukongEntity;
import com.p1nero.wukong.epicfight.animation.custom.*;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.BattleUnit;
import com.p1nero.wukong.epicfight.weapon.WukongColliders;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.item.WukongItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.MoveCoordFunctions;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.LevelUtil;
import yesman.epicfight.api.utils.TimePairList;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.skill.BasicAttack;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
// 悟空动画注册主类: 持有各形态动画访问器, 提供普通/戳棍/立棍/劈棍等动画的构建逻辑, 并附带物品缩放事件与 tick 计时等工具方法
public class WukongAnimations {

    // 若武器天赋技能数据已注册则写入数据(不同步)
    private static <T> void setWeaponInnateDataIfRegistered(
            ServerPlayerPatch playerPatch, SkillDataKey<T> key, T value) {
        SkillDataManager dataManager =
                playerPatch.getSkill(SkillSlots.WEAPON_INNATE).getDataManager();
        if (dataManager.hasData(key)) {
            dataManager.setData(key, value);
        }
    }

    // 若武器天赋技能数据已注册则写入数据并同步到客户端
    private static <T> void setWeaponInnateDataSyncIfRegistered(
            ServerPlayerPatch playerPatch, SkillDataKey<T> key, T value) {
        SkillDataManager dataManager =
                playerPatch.getSkill(SkillSlots.WEAPON_INNATE).getDataManager();
        if (dataManager.hasData(key)) {
            dataManager.setDataSync(key, value);
        }
    }

    public static AnimationManager.AnimationAccessor IDLE;
    public static AnimationManager.AnimationAccessor WALK;
    public static AnimationManager.AnimationAccessor RUN_F;
    public static AnimationManager.AnimationAccessor RUN;
    public static AnimationManager.AnimationAccessor DASH;
    public static AnimationManager.AnimationAccessor JUMP;
    public static AnimationManager.AnimationAccessor FALL;

    public static AnimationManager.AnimationAccessor JUMP_ATTACK_LIGHT;
    public static AnimationManager.AnimationAccessor JUMP_ATTACK_LIGHT_HIT;
    public static AnimationManager.AnimationAccessor JUMP_ATTACK_HEAVY;
    public static AnimationManager.AnimationAccessor DODGE_F1;
    public static AnimationManager.AnimationAccessor DODGE_F2;
    public static AnimationManager.AnimationAccessor DODGE_F3;
    public static AnimationManager.AnimationAccessor DODGE_FP;
    public static AnimationManager.AnimationAccessor DODGE_B1;
    public static AnimationManager.AnimationAccessor DODGE_B2;
    public static AnimationManager.AnimationAccessor DODGE_B3;
    public static AnimationManager.AnimationAccessor DODGE_BP;
    public static AnimationManager.AnimationAccessor DODGE_L1;
    public static AnimationManager.AnimationAccessor DODGE_L2;
    public static AnimationManager.AnimationAccessor DODGE_L3;
    public static AnimationManager.AnimationAccessor DODGE_LP;
    public static AnimationManager.AnimationAccessor DODGE_R1;
    public static AnimationManager.AnimationAccessor DODGE_R2;
    public static AnimationManager.AnimationAccessor DODGE_R3;
    public static AnimationManager.AnimationAccessor DODGE_RP;
    // 棍花
    public static AnimationManager.AnimationAccessor STAFF_SPIN_ONE_HAND_LOOP;
    public static AnimationManager.AnimationAccessor STAFF_SPIN_TWO_HAND_LOOP;

    // 轻击1~5
    public static AnimationManager.AnimationAccessor STAFF_AUTO1_DASH;
    public static AnimationManager.AnimationAccessor STAFF_AUTO1;
    public static AnimationManager.AnimationAccessor STAFF_AUTO2;
    public static AnimationManager.AnimationAccessor STAFF_AUTO3;
    public static AnimationManager.AnimationAccessor STAFF_AUTO4;
    public static AnimationManager.AnimationAccessor STAFF_AUTO5;

    // 劈棍
    // 衍生1 2
    public static AnimationManager.AnimationAccessor SMASH_DERIVE1;
    public static AnimationManager.AnimationAccessor SMASH_DERIVE2;
    public static AnimationManager.AnimationAccessor SMASH_CHARGING_PRE;
    public static AnimationManager.AnimationAccessor SMASH_CHARGING_LOOP;
    public static AnimationManager.AnimationAccessor SMASH_CHARGING_LOOP_STAND;
    // 不同星级的重击
    public static AnimationManager.AnimationAccessor SMASH_CHARGED0;
    public static AnimationManager.AnimationAccessor SMASH_CHARGED1;
    public static AnimationManager.AnimationAccessor SMASH_CHARGED2;
    public static AnimationManager.AnimationAccessor SMASH_CHARGED3;
    public static AnimationManager.AnimationAccessor SMASH_CHARGED4;

    // 戳棍
    // 衍生1 2

    public static AnimationManager.AnimationAccessor THRUST_JUESICK_FENGCHUANHUA;
    public static AnimationManager.AnimationAccessor THRUST_JUESICK_END;
    public static AnimationManager.AnimationAccessor THRUST_JUESICK_START;
    public static AnimationManager.AnimationAccessor THRUST_JUESICK_LOOP;
    public static AnimationManager.AnimationAccessor THRUST_FOOTAGE;
    public static AnimationManager.AnimationAccessor THRUST_RETREAT;
    public static AnimationManager.AnimationAccessor THRUST_XULI_START;
    public static AnimationManager.AnimationAccessor THRUST_XULI_LOOP;

    // 不同星级的重击
    public static AnimationManager.AnimationAccessor THRUST_CHARGED0;
    public static AnimationManager.AnimationAccessor THRUST_CHARGED1;
    public static AnimationManager.AnimationAccessor THRUST_CHARGED2;
    public static AnimationManager.AnimationAccessor THRUST_CHARGED3;

    // 立棍
    // 不同星级的重击
    public static AnimationManager.AnimationAccessor PILLAR_START0;
    public static AnimationManager.AnimationAccessor PILLAR_START1;
    public static AnimationManager.AnimationAccessor PILLAR_START2;
    public static AnimationManager.AnimationAccessor PILLAR_START3;
    public static AnimationManager.AnimationAccessor PILLAR_START4;
    public static AnimationManager.AnimationAccessor PILLAR_LOOP0;
    public static AnimationManager.AnimationAccessor PILLAR_CHARGED_LOOP1;
    public static AnimationManager.AnimationAccessor PILLAR_CHARGED_LOOP2;
    public static AnimationManager.AnimationAccessor PILLAR_CHARGED_LOOP3;
    public static AnimationManager.AnimationAccessor PILLAR_CHARGED_LOOP4;
    public static AnimationManager.AnimationAccessor PILLAR_CHARGED_LOOP0TOP1;
    public static AnimationManager.AnimationAccessor PILLAR_CHARGED_LOOP1TOP2;
    public static AnimationManager.AnimationAccessor PILLAR_CHARGED_LOOP2TOP3;
    public static AnimationManager.AnimationAccessor PILLAR_HEAVY0;
    public static AnimationManager.AnimationAccessor PILLAR_HEAVY1;
    public static AnimationManager.AnimationAccessor PILLAR_HEAVY2;
    public static AnimationManager.AnimationAccessor PILLAR_HEAVY3;
    public static AnimationManager.AnimationAccessor PILLAR_HEAVY4;
    public static AnimationManager.AnimationAccessor PILLAR_UP4;
    public static AnimationManager.AnimationAccessor PILLAR_UP;

    public static AnimationManager.AnimationAccessor PILLAR_HEAVY3_SAGE;
    public static AnimationManager.AnimationAccessor PILLAR_HEAVY_RIVERSEAFLIP;
    public static AnimationManager.AnimationAccessor PILLAR_HEAVY_FENGYUNZHUAN;
    public static AnimationManager.AnimationAccessor PILLAR_HEAVY_FENGYUNZHUANEND;
    // 法术
    public static AnimationManager.AnimationAccessor FASHU_MAGICARTS_ASF_START;
    public static AnimationManager.AnimationAccessor FASHU_MAGICARTS_DSF_START;
    // 身法
    public static AnimationManager.AnimationAccessor SHENFA_MAGICARTS_JQSQ_START;
    public static AnimationManager.AnimationAccessor SHENFA_MAGICARTS_JQSQ_END;
    public static AnimationManager.AnimationAccessor SHENFA_MAGICARTS_JQSQ_DISPLACEMENT_END;
    public static AnimationManager.AnimationAccessor SHENFA_MAGICARTS_TTTB_START;
    public static AnimationManager.AnimationAccessor SHENFA_MAGICARTS_TTTB_FAIL;

    public static AnimationManager.AnimationAccessor HAOMAO_MAGICARTS_FS; // 分身

    // 监听动画注册事件, 用本模组命名空间下的构建器构建普通形态与大圣形态的全部动画
    @SubscribeEvent
    public static void registerAnimations(AnimationManager.AnimationRegistryEvent event) {
        event.newBuilder(
                WukongMoveset.MOD_ID,
                (builder) -> {
                    WukongAnimations.build(builder);
                    WukongGreatSageAnimations.build(builder);
                });
    }

    // 将原始访问器以目标动画类型收窄, 仅为规避泛型检查的桥接方法
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <A extends StaticAnimation> AnimationManager.AnimationAccessor<A> typed(
            AnimationManager.AnimationAccessor accessor) {
        return accessor;
    }

    // 将原始访问器以主帧动画类型收窄, 仅为规避泛型检查的桥接方法
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <A extends MainFrameAnimation> AnimationManager.AnimationAccessor<A> typedMain(
            AnimationManager.AnimationAccessor accessor) {
        return accessor;
    }

    // 构建普通形态的全部动画: 法术/身法/立棍/戳棍/移动/闪避/轻击/跳跃攻击/棍花/劈棍等, 并注册访问器
    private static void build(AnimationManager.AnimationBuilder builder) {

        HumanoidArmature biped = Armatures.BIPED.get();
        // 专治各种因为移动导致的动画取消
        AnimationEvent.InPeriodEvent allStopMovement =
                AnimationEvent.InPeriodEvent.create(
                        0.00F,
                        Float.MAX_VALUE,
                        ((livingEntityPatch, staticAnimation, objects) -> {
                            if (livingEntityPatch instanceof LocalPlayerPatch localPlayerPatch) {
                                Input input = localPlayerPatch.getOriginal().input;
                                input.forwardImpulse = 0.0F;
                                input.leftImpulse = 0.0F;
                                input.down = false;
                                input.up = false;
                                input.left = false;
                                input.right = false;
                                input.jumping = false;
                                input.shiftKeyDown = false;
                                LocalPlayer clientPlayer = localPlayerPatch.getOriginal();
                                clientPlayer.setSprinting(false);
                            }
                        }),
                        AnimationEvent.Side.CLIENT);

        HAOMAO_MAGICARTS_FS =
                builder.nextAccessor(
                        "biped/fashu/haomao_fenshen",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.1F,
                                                        (livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.HAOMAO_SWSF.get(),
                                                                    2,
                                                                    2); // 播放音效
                                                        },
                                                        AnimationEvent.Side.SERVER),
                                                AnimationEvent.InTimeEvent.create(
                                                        3.2F,
                                                        (livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            Vec3 startPos =
                                                                    livingEntityPatch.getTarget()
                                                                                    == null
                                                                            ? livingEntityPatch
                                                                                    .getOriginal()
                                                                                    .position()
                                                                            : livingEntityPatch
                                                                                    .getTarget()
                                                                                    .position();
                                                            Vec3 particleOrigin =
                                                                    startPos.subtract(0, 1, 0);
                                                            if (livingEntityPatch.getOriginal()
                                                                    instanceof
                                                                    ServerPlayer serverPlayer) {
                                                                ServerLevel serverLevel =
                                                                        (ServerLevel)
                                                                                serverPlayer
                                                                                        .level();
                                                                int particleCount = 7;
                                                                float radius = 5F;
                                                                float angleIncrement =
                                                                        (float) Math.PI
                                                                                * 2
                                                                                / particleCount;
                                                                serverPlayer
                                                                        .getCapability(
                                                                                WKCapabilityProvider
                                                                                        .WK_PLAYER)
                                                                        .ifPresent(
                                                                                wkPlayer -> {
                                                                                    List<Integer>
                                                                                            spawnedIds =
                                                                                                    new ArrayList<>(); // 用于存放新生成的实体ID
                                                                                    for (int i = 0;
                                                                                            i
                                                                                                    < particleCount;
                                                                                            i++) {
                                                                                        float
                                                                                                angle =
                                                                                                        i
                                                                                                                * angleIncrement;
                                                                                        float
                                                                                                xOffset =
                                                                                                        radius
                                                                                                                * (float)
                                                                                                                        Math
                                                                                                                                .cos(
                                                                                                                                        angle);
                                                                                        float
                                                                                                zOffset =
                                                                                                        radius
                                                                                                                * (float)
                                                                                                                        Math
                                                                                                                                .sin(
                                                                                                                                        angle);
                                                                                        Vec3
                                                                                                particlePos =
                                                                                                        serverPlayer
                                                                                                                .position()
                                                                                                                .add(
                                                                                                                        xOffset,
                                                                                                                        0,
                                                                                                                        zOffset);
                                                                                        serverLevel
                                                                                                .sendParticles(
                                                                                                        ParticleTypes
                                                                                                                .POOF,
                                                                                                        particlePos
                                                                                                                .x,
                                                                                                        particlePos
                                                                                                                        .y
                                                                                                                + 2,
                                                                                                        particlePos
                                                                                                                .z,
                                                                                                        20,
                                                                                                        0,
                                                                                                        0,
                                                                                                        0,
                                                                                                        0.1);
                                                                                        FakeWukongEntity
                                                                                                fakeWukongEntity =
                                                                                                        new FakeWukongEntity(
                                                                                                                serverPlayer);
                                                                                        fakeWukongEntity
                                                                                                .setPos(
                                                                                                        particlePos
                                                                                                                .add(
                                                                                                                        0,
                                                                                                                        1,
                                                                                                                        0));
                                                                                        serverLevel
                                                                                                .addFreshEntity(
                                                                                                        fakeWukongEntity);
                                                                                        int
                                                                                                entityId =
                                                                                                        fakeWukongEntity
                                                                                                                .getId();
                                                                                        wkPlayer
                                                                                                .addFakeWukongId(
                                                                                                        entityId);
                                                                                        spawnedIds
                                                                                                .add(
                                                                                                        entityId);
                                                                                    }
                                                                                });
                                                            }
                                                        },
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.5F)); // 设置播放速度

        FASHU_MAGICARTS_DSF_START =
                builder.nextAccessor(
                        "biped/fashu/fashu_magicarts_dsf_start",
                        accessor ->
                                (new SpecialActionAnimation(
                                                0F, 0.14F, typed(accessor), Armatures.BIPED))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2.0F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        (livingEntityPatch,
                                                                staticAnimation,
                                                                objects) ->
                                                                BattleUnit.ding(livingEntityPatch),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.XULI_DING_SOU
                                                                            .get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));

        FASHU_MAGICARTS_ASF_START =
                builder.nextAccessor(
                        "biped/fashu/fashu_magicarts_asf_start",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.5F)));
        SHENFA_MAGICARTS_TTTB_START =
                builder.nextAccessor(
                        "biped/fashu/shenfa_magicarts_tttb_start",
                        accessor ->
                                new AttackAnimation(
                                                0F,
                                                0F,
                                                0F,
                                                0F,
                                                0.5F,
                                                WukongColliders.JUMP_ATTACK_LIGHT,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.5F)));
        SHENFA_MAGICARTS_TTTB_FAIL =
                builder.nextAccessor(
                        "biped/fashu/shenfa_magicarts_tttb_fail",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.5F)));

        SHENFA_MAGICARTS_JQSQ_START =
                builder.nextAccessor(
                        "biped/fashu/magicarts_cfda_start_s1",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2.5F)));
        SHENFA_MAGICARTS_JQSQ_END =
                builder.nextAccessor(
                        "biped/fashu/magicarts_cfda_end",
                        accessor ->
                                new AttackAnimation(
                                                0.15F,
                                                0.6F,
                                                0.15F,
                                                0.8F,
                                                1.6F,
                                                WukongColliders.JUMP_ATTACK_LIGHT,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(3.0F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(4.0F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .MAX_STRIKES_MODIFIER,
                                                ValueModifier.setter(1))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_ON_LINK,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_UPDATE_TIME,
                                                TimePairList.create(0.0F, 0.6F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_BEGIN,
                                                WukongMoveCoordFunctions.TRACE_TARGET_DASH)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_TICK,
                                                WukongMoveCoordFunctions.TRACE_TARGET_DASH)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty.COORD_GET,
                                                MoveCoordFunctions.WORLD_COORD)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.5F))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.SHENFA_JXSQ_END
                                                                            .get(),
                                                                    0,
                                                                    0);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        SHENFA_MAGICARTS_JQSQ_DISPLACEMENT_END =
                builder.nextAccessor(
                        "biped/fashu/magicarts_cfda_displacement_end",
                        accessor ->
                                new AttackAnimation(
                                                0.15F,
                                                0.5F,
                                                0.15F,
                                                0.8333F,
                                                1.5F,
                                                WukongColliders.JUMP_ATTACK_LIGHT,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(3.0F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(4.0F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .MAX_STRIKES_MODIFIER,
                                                ValueModifier.setter(1))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_ON_LINK,
                                                false)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.5F))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.SHENFA_JXSQ_END
                                                                            .get(),
                                                                    0,
                                                                    0);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));

        PILLAR_START0 =
                builder.nextAccessor(
                        "biped/pillar/pillar_start0",
                        accessor ->
                                new ActionAnimation(0.5F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.7F, 1.7f))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                livingEntityPatch.reserveAnimation(
                                                                        PILLAR_LOOP0);
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setData(
                                                                                WukongSkillDataKeys
                                                                                        .IS_CHARGING
                                                                                        .get(),
                                                                                true);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(0.6F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.of(
                                                                0.83F, 1F, 1F, 1F, 0F, 1.6F, 0F),
                                                        ScaleTime.of(
                                                                1.7f, 1F, 1F, 1F, 0F, 1.6F, 0F))));
        PILLAR_START1 =
                builder.nextAccessor(
                        "biped/pillar/pillar_start1",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.7F, 1.7f))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                livingEntityPatch.reserveAnimation(
                                                                        PILLAR_LOOP0);
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setData(
                                                                                WukongSkillDataKeys
                                                                                        .IS_CHARGING
                                                                                        .get(),
                                                                                true);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(0.6F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.of(
                                                                0.83F, 1F, 1.4F, 1F, 0F, 1.8F, 0F),
                                                        ScaleTime.of(
                                                                1.7f, 1F, 1.4F, 1F, 0F, 1.8F,
                                                                0F))));

        PILLAR_START2 =
                builder.nextAccessor(
                        "biped/pillar/pillar_start2",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.7F, 1.7f))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                livingEntityPatch.reserveAnimation(
                                                                        PILLAR_LOOP0);
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setData(
                                                                                WukongSkillDataKeys
                                                                                        .IS_CHARGING
                                                                                        .get(),
                                                                                true);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(0.6F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.of(
                                                                1.33F, 1F, 1.7F, 1F, 0F, 1.8F, 0F),
                                                        ScaleTime.of(
                                                                1.7f, 1F, 1.7F, 1F, 0F, 1.8F,
                                                                0F))));

        PILLAR_START3 =
                builder.nextAccessor(
                        "biped/pillar/pillar_start3",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.7F, 1.7f))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                livingEntityPatch.reserveAnimation(
                                                                        PILLAR_LOOP0);
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setData(
                                                                                WukongSkillDataKeys
                                                                                        .IS_CHARGING
                                                                                        .get(),
                                                                                true);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(0.6F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.of(
                                                                1.33F, 1F, 2.2F, 1F, 0F, 1.9F, 0F),
                                                        ScaleTime.of(
                                                                1.7f, 1F, 2.2F, 1F, 0F, 1.9F,
                                                                0F))));

        PILLAR_LOOP0 =
                builder.nextAccessor(
                        "biped/pillar/pillar_loop0",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 4F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.reserveAnimation(
                                                                    PILLAR_LOOP0);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        final AnimationEvent.InTimeEvent[][] pillarUpEvents = new AnimationEvent.InTimeEvent[1][];
        PILLAR_UP =
                builder.nextAccessor(
                        "biped/pillar/pillar_up_1",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 1F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.5F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.reserveAnimation(
                                                                    PILLAR_LOOP0);
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(pillarUpEvents[0]));
        List<AnimationEvent.InTimeEvent> pillar_up =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.3F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.of(0F, 1F, 1F, 1F, 0F, 1.6F, 0F),
                                ScaleTime.of(0.33F, 1F, 1F, 1F, 0F, 1.8F, 0F),
                                ScaleTime.of(0.83F, 1F, 1.4F, 1F, 0F, 1.8F, 0F),
                                ScaleTime.of(1f, 1F, 1.4F, 1F, 0F, 1.8F, 0F)));
        pillarUpEvents[0] = pillar_up.toArray(new AnimationEvent.InTimeEvent[0]);

        PILLAR_HEAVY0 =
                builder.nextAccessor(
                        "biped/pillar/charged_heavy0",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                0.833F,
                                                1F,
                                                3.166F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(1.75F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 0.33f))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2.2F))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.HIT_GROUND.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        PILLAR_HEAVY1 =
                builder.nextAccessor(
                        "biped/pillar/charged_heavy1",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                1.1F,
                                                1.3F,
                                                3.166F,
                                                WukongColliders.PILLAR_HEAVY1,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(3.75F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 0.33f))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2.5F))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.HIT_GROUND.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {}),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(
                                                                0F, 1F, 1.2F, 1F, 0F, 0.25F, 0F),
                                                        ScaleTime.of(
                                                                2.9F, 1F, 1.2F, 1F, 0F, 0.25F, 0F),
                                                        ScaleTime.of(
                                                                2.933F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.reset(2.933F))));
        PILLAR_HEAVY2 =
                builder.nextAccessor(
                        "biped/pillar/charged_heavy2",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                1F,
                                                1.5F,
                                                3.166F,
                                                WukongColliders.PILLAR_HEAVY2,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(5.75F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 0.5f))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        3F))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.HIT_GROUND.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {}),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(
                                                                0F, 1F, 1.5F, 1F, 0F, 0.45F, 0F),
                                                        ScaleTime.of(
                                                                2.9F, 1F, 1.5F, 1F, 0F, 0.45F, 0F),
                                                        ScaleTime.of(3F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.reset(3F))));
        final AnimationEvent.InTimeEvent[][] pillarHeavy3Events =
                new AnimationEvent.InTimeEvent[1][];
        PILLAR_HEAVY3 =
                builder.nextAccessor(
                        "biped/pillar/charged_heavy3",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                1F,
                                                1.5F,
                                                3.6F,
                                                WukongColliders.PILLAR_HEAVY3,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(7.75F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 0.66f))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.HIT_GROUND.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (self,
                                                        entitypatch,
                                                        speed,
                                                        prevElapsedTime,
                                                        elapsedTime) -> {
                                                    double originalYVelocity =
                                                            ((LivingEntity)
                                                                            entitypatch
                                                                                    .getOriginal())
                                                                    .getDeltaMovement()
                                                                    .y;
                                                    if (elapsedTime >= 0.73f
                                                            && elapsedTime <= 1.1f) {
                                                        double extraGravity = 1.7F;
                                                        entitypatch
                                                                .getOriginal()
                                                                .setDeltaMovement(
                                                                        entitypatch
                                                                                .getOriginal()
                                                                                .getDeltaMovement()
                                                                                .x,
                                                                        originalYVelocity
                                                                                - extraGravity,
                                                                        entitypatch
                                                                                .getOriginal()
                                                                                .getDeltaMovement()
                                                                                .z);
                                                    } else {
                                                        entitypatch
                                                                .getOriginal()
                                                                .setDeltaMovement(
                                                                        entitypatch
                                                                                .getOriginal()
                                                                                .getDeltaMovement()
                                                                                .x,
                                                                        originalYVelocity,
                                                                        entitypatch
                                                                                .getOriginal()
                                                                                .getDeltaMovement()
                                                                                .z);
                                                    }
                                                    return 3F;
                                                })
                                        .addEvents(pillarHeavy3Events[0]));
        List<AnimationEvent.InTimeEvent> PILLAR_Heavy3 =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.083F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.of(0F, 1F, 1.5F, 1F, 0F, 1.5F, 0F),
                                ScaleTime.of(2.9F, 1F, 1.5F, 1F, 0F, 1F, 0F),
                                ScaleTime.of(3F, 1F, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.reset(3F)));
        PILLAR_Heavy3.add(
                AnimationEvent.InTimeEvent.create(
                                1.2333F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0F, 0F, -6F),
                                yesman.epicfight.gameasset.Armatures.BIPED.get().rootJoint,
                                3D,
                                0.01F));
        pillarHeavy3Events[0] = PILLAR_Heavy3.toArray(new AnimationEvent.InTimeEvent[0]);

        PILLAR_START4 =
                builder.nextAccessor(
                        "biped/pillar/charged_start4",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                1.666F,
                                                1.5F,
                                                3.6F,
                                                WukongColliders.PILLAR_HEAVY4,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.7F, 1.66F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2.0F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.reserveAnimation(
                                                                    PILLAR_CHARGED_LOOP4);
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setDataSync(
                                                                                WukongSkillDataKeys
                                                                                        .IS_CHARGING
                                                                                        .get(),
                                                                                true);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(0F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.of(
                                                                0.7F, 1F, 1F, 1F, 0F, 0.45F, 0F),
                                                        ScaleTime.of(1f, 1, 1.5F, 1F, 0F, 1.5F, 0F),
                                                        ScaleTime.of(
                                                                1.3f, 1, 1.8F, 1F, 0F, 1.5F, 0F),
                                                        ScaleTime.of(
                                                                1.66F, 1F, 1.8F, 1F, 0F, 1.5F, 0F),
                                                        ScaleTime.of(
                                                                2F, 1F, 1.8F, 1F, 0F, 1.5F, 0F))));

        PILLAR_CHARGED_LOOP4 =
                builder.nextAccessor(
                        "biped/pillar/charged_loop4",
                        accessor ->
                                new ActionAnimation(0F, 1F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 5.766F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.reserveAnimation(
                                                                    PILLAR_CHARGED_LOOP4);
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.3F))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(
                                                                0F, 1F, 1.8F, 1F, 0F, 1.5F, 0F),
                                                        ScaleTime.of(
                                                                0F, 1F, 1.8F, 1F, 0F, 1.5F, 0F),
                                                        ScaleTime.of(
                                                                4.766F, 1F, 1.8F, 1F, 0F, 1.5F,
                                                                0F))));
        final AnimationEvent.InTimeEvent[][] pillarHeavy4Events =
                new AnimationEvent.InTimeEvent[1][];
        PILLAR_HEAVY4 =
                builder.nextAccessor(
                        "biped/pillar/charged_heavy4",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                0.9F,
                                                2F,
                                                3.6F,
                                                WukongColliders.PILLAR_HEAVY4,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(11f))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(2.5F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 0.73f))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (self,
                                                        entitypatch,
                                                        speed,
                                                        prevElapsedTime,
                                                        elapsedTime) -> {
                                                    double originalYVelocity =
                                                            ((LivingEntity)
                                                                            entitypatch
                                                                                    .getOriginal())
                                                                    .getDeltaMovement()
                                                                    .y;
                                                    if (elapsedTime >= 0.73f
                                                            && elapsedTime <= 1.1f) {
                                                        double extraGravity = 1.5F;
                                                        entitypatch
                                                                .getOriginal()
                                                                .setDeltaMovement(
                                                                        entitypatch
                                                                                .getOriginal()
                                                                                .getDeltaMovement()
                                                                                .x,
                                                                        originalYVelocity
                                                                                - extraGravity,
                                                                        entitypatch
                                                                                .getOriginal()
                                                                                .getDeltaMovement()
                                                                                .z);
                                                    } else {
                                                        entitypatch
                                                                .getOriginal()
                                                                .setDeltaMovement(
                                                                        entitypatch
                                                                                .getOriginal()
                                                                                .getDeltaMovement()
                                                                                .x,
                                                                        originalYVelocity,
                                                                        entitypatch
                                                                                .getOriginal()
                                                                                .getDeltaMovement()
                                                                                .z);
                                                    }
                                                    return 2.5F;
                                                })
                                        .addEvents(pillarHeavy4Events[0]));
        List<AnimationEvent.InTimeEvent> pillar_heavy4 =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.083F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.of(0F, 1F, 1.8F, 1F, 0F, 1.5F, 0F),
                                ScaleTime.of(2.9F, 1F, 1.8F, 1F, 0F, 1.5F, 0F),
                                ScaleTime.of(3F, 1F, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.reset(3F)));
        pillar_heavy4.add(
                AnimationEvent.InTimeEvent.create(
                                1.2333F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0F, 0F, -8F),
                                yesman.epicfight.gameasset.Armatures.BIPED.get().rootJoint,
                                5D,
                                0.01F));
        pillarHeavy4Events[0] = pillar_heavy4.toArray(new AnimationEvent.InTimeEvent[0]);

        final AnimationEvent.InTimeEvent[][] riverseaflipEvents =
                new AnimationEvent.InTimeEvent[1][];
        PILLAR_HEAVY_RIVERSEAFLIP =
                builder.nextAccessor(
                        "biped/pillar/stick_heavy_riverseaflip",
                        accessor ->
                                new BasicMultipleAttackAnimation(
                                                0F,
                                                typed(accessor),
                                                Armatures.BIPED,
                                                new AttackAnimation.Phase(
                                                                0.0F,
                                                                0.43333F,
                                                                0.6666F,
                                                                3.03333F,
                                                                0.7666F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.9F)),
                                                new AttackAnimation.Phase(
                                                                0.7666F,
                                                                2.23333f,
                                                                2.43333F,
                                                                3.03333F,
                                                                4.7F,
                                                                biped.toolR,
                                                                WukongColliders
                                                                        .PILLAR_HEAVY_RIVERSEAFLIP)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(4.48f)))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (self,
                                                        entitypatch,
                                                        speed,
                                                        prevElapsedTime,
                                                        elapsedTime) -> {
                                                    if (elapsedTime > 0.4333f
                                                            && elapsedTime < 0.6f) {
                                                        return 2.8F;
                                                    } else if (elapsedTime > 2.1f
                                                            && elapsedTime < 2.4333f) {
                                                        return 2.8F;
                                                    }
                                                    return 2.3F;
                                                })
                                        .addEvents(riverseaflipEvents[0]));
        List<AnimationEvent.InTimeEvent> riverseaflip =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.1F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.of(0F, 1, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(2.23333f, 1, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.reset(3F)));
        riverseaflipEvents[0] = riverseaflip.toArray(new AnimationEvent.InTimeEvent[0]);

        PILLAR_HEAVY_FENGYUNZHUAN =
                builder.nextAccessor(
                        "biped/pillar/stick_heavy_fengyunzhuan",
                        accessor ->
                                new WukongBasicMultipleAttackAnimation(
                                                0F,
                                                typed(accessor),
                                                Armatures.BIPED,
                                                new AttackAnimation.Phase(
                                                                0.0F,
                                                                0.2F,
                                                                0.4F,
                                                                1.1F,
                                                                0.4F,
                                                                biped.toolR,
                                                                WukongColliders.PILLAR_FENGYUNZHUAN)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(1.12F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .IMPACT_MODIFIER,
                                                                ValueModifier.multiplier(4f))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .SWING_SOUND,
                                                                WuKongSounds.PERFECT_FYZ.get()),
                                                new AttackAnimation.Phase(
                                                                0.4F,
                                                                0.5f,
                                                                0.6333F,
                                                                1.1F,
                                                                0.6333F,
                                                                biped.toolR,
                                                                WukongColliders.PILLAR_FENGYUNZHUAN)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(1.12F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .IMPACT_MODIFIER,
                                                                ValueModifier.multiplier(4f))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .SWING_SOUND,
                                                                WuKongSounds.PERFECT_FYZ.get()),
                                                new AttackAnimation.Phase(
                                                                0.6333F,
                                                                0.766f,
                                                                0.8666F,
                                                                1.1F,
                                                                0.8666F,
                                                                biped.toolR,
                                                                WukongColliders.PILLAR_FENGYUNZHUAN)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(1.12F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .IMPACT_MODIFIER,
                                                                ValueModifier.multiplier(4f))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .SWING_SOUND,
                                                                WuKongSounds.PERFECT_FYZ.get()),
                                                new AttackAnimation.Phase(
                                                                0.8666F,
                                                                0.96666f,
                                                                1.1F,
                                                                1.1F,
                                                                1.1F,
                                                                biped.toolR,
                                                                WukongColliders.PILLAR_FENGYUNZHUAN)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(1.12F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .IMPACT_MODIFIER,
                                                                ValueModifier.multiplier(4f))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .SWING_SOUND,
                                                                WuKongSounds.PERFECT_FYZ.get()))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true) // 禁用垂直移动
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_ON_LINK,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false) // 禁用取消移动
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                true)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                setWeaponInnateDataSyncIfRegistered(
                                                                        serverPlayerPatch,
                                                                        WukongSkillDataKeys
                                                                                .PROTECT_NEXT_FALL
                                                                                .get(),
                                                                        true);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch.getOriginal()
                                                                    instanceof LivingEntity) {
                                                                LivingEntity livingEntity =
                                                                        (LivingEntity)
                                                                                livingEntityPatch
                                                                                        .getOriginal(); // 转换类型
                                                                livingEntity.setPos(
                                                                        livingEntity.getX(),
                                                                        livingEntity.getY(),
                                                                        livingEntity
                                                                                .getZ()); // 保持位置
                                                            }
                                                            livingEntityPatch.reserveAnimation(
                                                                    PILLAR_HEAVY_FENGYUNZHUAN);
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.8F))); // 修改播放速度

        PILLAR_HEAVY_FENGYUNZHUANEND =
                builder.nextAccessor(
                        "biped/pillar/stick_heavy_fengyunzhuanend",
                        accessor ->
                                new BasicAttackAnimation(
                                                0F,
                                                0,
                                                0,
                                                1.93333F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 0.5333F))
                                        .addEvents(
                                                AnimationEvent.InPeriodEvent.create(
                                                        0F,
                                                        1.93333F,
                                                        (livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setDataSync(
                                                                                WukongSkillDataKeys
                                                                                        .PILLAR_JIANGHAIFAN_TIMER
                                                                                        .get(),
                                                                                Config
                                                                                        .DERIVE_CHECK_TIME
                                                                                        .get()
                                                                                        .intValue());
                                                                setWeaponInnateDataSyncIfRegistered(
                                                                        serverPlayerPatch,
                                                                        WukongSkillDataKeys
                                                                                .PROTECT_NEXT_FALL
                                                                                .get(),
                                                                        false);
                                                            }
                                                        },
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2.3F)));

        THRUST_XULI_LOOP =
                builder.nextAccessor(
                        "biped/thrust/thrust_xuli_loop",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2.0F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.reserveAnimation(
                                                                    THRUST_XULI_LOOP);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        THRUST_XULI_START =
                builder.nextAccessor(
                        "biped/thrust/thrust_xuli_start",
                        accessor ->
                                new ActionAnimation(0F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2.5F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.reserveAnimation(
                                                                    THRUST_XULI_LOOP);
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setDataSync(
                                                                                WukongSkillDataKeys
                                                                                        .Thrust_IS_CHARGING
                                                                                        .get(),
                                                                                true);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.2F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                if (serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .getDataValue(
                                                                                WukongSkillDataKeys
                                                                                        .Thrust_KEY_PRESSING
                                                                                        .get()))
                                                                    livingEntityPatch.playSound(
                                                                            WuKongSounds
                                                                                    .XULI_LEVEL_RISE03
                                                                                    .get(),
                                                                            0,
                                                                            0);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        THRUST_CHARGED0 =
                builder.nextAccessor(
                        "biped/thrust/thrust_heavy0",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                0.5F,
                                                0.733F,
                                                2.766F,
                                                WukongColliders.STACK_0_1,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.STUN_TYPE,
                                                StunType.LONG)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(2F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_BEGIN,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_TICK,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .FIXED_HEAD_ROTATION,
                                                true)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            WukongMoveCoordFunctions.reseTSjzt();
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.8F))
                                        .addEvents(allStopMovement)
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.HIT_GROUND.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        THRUST_CHARGED1 =
                builder.nextAccessor(
                        "biped/thrust/thrust_heavy1",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                0.5F,
                                                0.733F,
                                                2.766F,
                                                WukongColliders.STACK_0_1,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(4F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_BEGIN,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_TICK,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .FIXED_HEAD_ROTATION,
                                                true)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            WukongMoveCoordFunctions.reseTSjzt();
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.8F))
                                        .addEvents(allStopMovement)
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.HIT_GROUND.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        final AnimationEvent.InTimeEvent[][] charged2Events = new AnimationEvent.InTimeEvent[1][];
        THRUST_CHARGED2 =
                builder.nextAccessor(
                        "biped/thrust/thrust_heavy2",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                0.6F,
                                                0.733F,
                                                2.766F,
                                                WukongColliders.THRUST_CHARGED2,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(6.25F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_BEGIN,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_TICK,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .FIXED_HEAD_ROTATION,
                                                true)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            WukongMoveCoordFunctions.reseTSjzt();
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.8F))
                                        .addEvents(allStopMovement)
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.HIT_GROUND.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(charged2Events[0]));
        List<AnimationEvent.InTimeEvent> charged2 =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.1F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.of(0F, 1, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(0.2F, 1, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(0.66F, 1, 2F, 1F, 0F, -1F, 0F),
                                ScaleTime.of(1F, 1, 2F, 1F, 0F, -1F, 0F),
                                ScaleTime.of(1.333F, 1, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.reset(1.333F)));
        charged2Events[0] = charged2.toArray(new AnimationEvent.InTimeEvent[0]);

        final AnimationEvent.InTimeEvent[][] charged3Events = new AnimationEvent.InTimeEvent[1][];
        THRUST_CHARGED3 =
                builder.nextAccessor(
                        "biped/thrust/thrust_heavy3",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                0.56F,
                                                0.833F,
                                                3.33F,
                                                WukongColliders.THRUST_CHARGED3,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(8F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(50f))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_BEGIN,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_TICK,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .FIXED_HEAD_ROTATION,
                                                true)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            WukongMoveCoordFunctions.reseTSjzt();
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2.4F))
                                        .addEvents(charged3Events[0]));
        List<AnimationEvent.InTimeEvent> charged3 =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.1F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.of(0F, 1, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(0.266F, 1, 1.5F, 1F, 0F, 1F, 0F),
                                ScaleTime.of(0.333F, 1, 2F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(0.5F, 1, 3F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(1F, 1, 3F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(2F, 1, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.reset(2F)));
        charged3Events[0] = charged3.toArray(new AnimationEvent.InTimeEvent[0]);

        final AnimationEvent.InTimeEvent[][] fengchuanhuaEvents =
                new AnimationEvent.InTimeEvent[1][];
        THRUST_JUESICK_FENGCHUANHUA =
                builder.nextAccessor(
                        "biped/thrust/thrust_heavy4_fengchuanhua",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                0.56F,
                                                0.833F,
                                                3.33F,
                                                WukongColliders.THRUST_FENGCHUANHUA,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(15.6F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .MAX_STRIKES_MODIFIER,
                                                ValueModifier.setter(50F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(50f))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.2F, 0.766F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_BEGIN,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_TICK,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .FIXED_HEAD_ROTATION,
                                                true)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            WukongMoveCoordFunctions.reseTSjzt();
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .POSE_MODIFIER,
                                                ((dynamicAnimation,
                                                        pose,
                                                        livingEntityPatch,
                                                        v,
                                                        v1) -> {}))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        2F))
                                        .addEvents(fengchuanhuaEvents[0]));
        List<AnimationEvent.InTimeEvent> fengchuanhua =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.1F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.of(0F, 1, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(0.266F, 1, 1.5F, 1F, 0F, 1F, 0F),
                                ScaleTime.of(0.333F, 1, 2F, 1F, 0F, 1F, 0F),
                                ScaleTime.of(0.5F, 1, 4F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(1F, 1, 4F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(2F, 1, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.reset(2F)));
        fengchuanhua.add(
                AnimationEvent.InTimeEvent.create(
                                0.3F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0F, 0F, -5F),
                                yesman.epicfight.gameasset.Armatures.BIPED.get().rootJoint,
                                1D,
                                0.01F));
        fengchuanhua.add(
                AnimationEvent.InTimeEvent.create(
                                0.4F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0F, 0F, -8F),
                                yesman.epicfight.gameasset.Armatures.BIPED.get().rootJoint,
                                2D,
                                0.01F));
        fengchuanhua.add(
                AnimationEvent.InTimeEvent.create(
                                0.5F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0F, 0F, -13F),
                                yesman.epicfight.gameasset.Armatures.BIPED.get().rootJoint,
                                3D,
                                0.01F));
        fengchuanhuaEvents[0] = fengchuanhua.toArray(new AnimationEvent.InTimeEvent[0]);

        // 退步
        THRUST_RETREAT =
                builder.nextAccessor(
                        "biped/thrust/thrust_retreat",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.1F,
                                                0F,
                                                0F,
                                                0.5F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 0.5F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.5F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setDataSync(
                                                                                WukongSkillDataKeys
                                                                                        .Thrust_CAN_FIRST_DERIVE
                                                                                        .get(),
                                                                                false);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .TICK_EVENTS,
                                                AnimationEvent.InTimeEvent.create(
                                                        0.366F,
                                                        (livingEntityPatch,
                                                                staticAnimation,
                                                                objects) ->
                                                                BattleUnit.CUNTUI_JIESUO(
                                                                        livingEntityPatch),
                                                        AnimationEvent.Side.SERVER),
                                                AnimationEvent.InTimeEvent.create(
                                                        1.166F,
                                                        (livingEntityPatch,
                                                                staticAnimation,
                                                                objects) ->
                                                                BattleUnit.CUNTUI_SHANGSUO(
                                                                        livingEntityPatch),
                                                        AnimationEvent.Side.SERVER)));

        // 进入
        THRUST_FOOTAGE =
                builder.nextAccessor(
                        "biped/thrust/thrust_footage",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                1.866F,
                                                2.033F,
                                                3.566f,
                                                WukongColliders.THRUST_FOOTAGE,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(3.92F)) // 3.92
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(50f))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_ON_LINK,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.133F, 1.33F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_BEGIN,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_TICK,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            WukongMoveCoordFunctions.reseTSjzt();
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .POSE_MODIFIER,
                                                ((dynamicAnimation,
                                                        pose,
                                                        livingEntityPatch,
                                                        v,
                                                        v1) -> {}))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        3F))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(0F, 1, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.of(
                                                                1.866F, 1, 1.8F, 1F, 0F, 0.3F, 0F),
                                                        ScaleTime.of(2.033F, 1, 3F, 1F, 0F, 1F, 0F),
                                                        ScaleTime.of(
                                                                2.033F, 1, 2.5F, 1F, 0F, 1F, 0F),
                                                        ScaleTime.of(3.5F, 1, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.reset(3.5F))));

        THRUST_JUESICK_LOOP =
                builder.nextAccessor(
                        "biped/thrust/thrust_juesick_loop",
                        accessor ->
                                new BasicAttackAnimation(
                                                0.15F,
                                                typed(accessor),
                                                Armatures.BIPED,
                                                new AttackAnimation.Phase(
                                                                0.0F,
                                                                0F,
                                                                0.333F,
                                                                1.333F,
                                                                0.333F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.672f)),
                                                new AttackAnimation.Phase(
                                                                0.333F,
                                                                0.333F,
                                                                0.666F,
                                                                1.333F,
                                                                0.666F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.672f)),
                                                new AttackAnimation.Phase(
                                                                0.666F,
                                                                0.666F,
                                                                1F,
                                                                1.333F,
                                                                1F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.672f)),
                                                new AttackAnimation.Phase(
                                                                1F,
                                                                1F,
                                                                1.333F,
                                                                1.333F,
                                                                1.333F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.672f)))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(0.9F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_ON_LINK,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.8F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.reserveAnimation(
                                                                    THRUST_JUESICK_LOOP);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));

        THRUST_JUESICK_START =
                builder.nextAccessor(
                        "biped/thrust/thrust_juesick_start",
                        accessor ->
                                new AttackAnimation(
                                                0F,
                                                typed(accessor),
                                                Armatures.BIPED,
                                                new AttackAnimation.Phase(
                                                                0.0F,
                                                                0.866F,
                                                                1F,
                                                                1.6666F,
                                                                1.6666F,
                                                                biped.toolR,
                                                                WukongColliders
                                                                        .THRUST_JUESICK_START)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(1.68f)))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_ON_LINK,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        3F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.reserveAnimation(
                                                                    THRUST_JUESICK_LOOP);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));

        THRUST_JUESICK_END =
                builder.nextAccessor(
                        "biped/thrust/thrust_juesick_end",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0F,
                                                0F,
                                                0F,
                                                0.9f,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.8F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setDataSync(
                                                                                WukongSkillDataKeys
                                                                                        .Thrust_JUESICK_BACK
                                                                                        .get(),
                                                                                false);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER)));

        final AnimationEvent.InTimeEvent[][] pillarHeavy3SageEvents =
                new AnimationEvent.InTimeEvent[1][];
        PILLAR_HEAVY3_SAGE =
                builder.nextAccessor(
                        "biped/pillar/stick_heavy_sage",
                        accessor ->
                                new BasicMultipleAttackAnimation(
                                                0F,
                                                typed(accessor),
                                                Armatures.BIPED,
                                                new AttackAnimation.Phase(
                                                                0.0F,
                                                                1.3666F,
                                                                1.5666F,
                                                                5.93333F,
                                                                1.6666F,
                                                                biped.toolR,
                                                                WukongColliders.PILLAR_HEAVY3_SAGE)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(2f)),
                                                new AttackAnimation.Phase(
                                                                1.6666F,
                                                                2.6666F,
                                                                2.7666F,
                                                                5.93333F,
                                                                2.8333F,
                                                                biped.toolR,
                                                                WukongColliders.PILLAR_HEAVY3_SAGE)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(2f)),
                                                new AttackAnimation.Phase(
                                                                2.8333F,
                                                                3f,
                                                                3.16666F,
                                                                5.93333F,
                                                                3.26666F,
                                                                biped.toolR,
                                                                WukongColliders.PILLAR_HEAVY3_SAGE)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(2f)),
                                                new AttackAnimation.Phase(
                                                                3.26666F,
                                                                3.933f,
                                                                4F,
                                                                5.93333F,
                                                                5.93333F,
                                                                biped.toolR,
                                                                WukongColliders.PILLAR_HEAVY3_SAGE)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(13f)))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0F, 4.4666F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (self,
                                                        entitypatch,
                                                        speed,
                                                        prevElapsedTime,
                                                        elapsedTime) -> {
                                                    if (elapsedTime > 1.6333F
                                                            && elapsedTime < 2.2f) {
                                                        return 3F * speed;
                                                    }
                                                    return 1.2F * speed;
                                                })
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.2F))
                                        .addEvents(pillarHeavy3SageEvents[0]));
        List<AnimationEvent.InTimeEvent> scList5 =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.1F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.of(0F, 1, 1F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(1.3333F, 1, 1.2F, 1F, 0F, -0.2F, 0F),
                                ScaleTime.of(2.13333F, 1, 2.0F, 1F, 0F, -1.2F, 0F),
                                ScaleTime.reset(5.93333F)));
        scList5.add(
                AnimationEvent.InTimeEvent.create(
                                1.5666F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0F, 0F, 0F),
                                yesman.epicfight.gameasset.Armatures.BIPED.get().rootJoint,
                                3D,
                                0.01F));
        scList5.add(
                AnimationEvent.InTimeEvent.create(
                                2.8333F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0F, -5F, -5F),
                                yesman.epicfight.gameasset.Armatures.BIPED.get().rootJoint,
                                2D,
                                0.01F));
        scList5.add(
                AnimationEvent.InTimeEvent.create(
                                3.066F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0F, -5F, -5F),
                                yesman.epicfight.gameasset.Armatures.BIPED.get().rootJoint,
                                4D,
                                0.01F));
        scList5.add(
                AnimationEvent.InTimeEvent.create(
                                3.966666F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0F, -5F, -5F),
                                yesman.epicfight.gameasset.Armatures.BIPED.get().rootJoint,
                                7D,
                                0.01F));
        pillarHeavy3SageEvents[0] = scList5.toArray(new AnimationEvent.InTimeEvent[0]);

        IDLE =
                builder.nextAccessor(
                        "biped/idle",
                        accessor -> new StaticAnimation(true, typed(accessor), Armatures.BIPED));
        WALK =
                builder.nextAccessor(
                        "biped/walk",
                        accessor ->
                                new StaticAnimation(true, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.2F)));
        RUN_F =
                builder.nextAccessor(
                        "biped/run_f",
                        accessor -> new StaticAnimation(true, typed(accessor), Armatures.BIPED));
        RUN =
                builder.nextAccessor(
                        "biped/run",
                        accessor ->
                                new SelectiveAnimation(
                                        (entityPatch) -> {
                                            Vec3 view =
                                                    entityPatch.getOriginal().getViewVector(1.0F);
                                            Vec3 move =
                                                    entityPatch.getOriginal().getDeltaMovement();
                                            double dot = view.dot(move);
                                            return dot < 0.0 ? 1 : 0;
                                        },
                                        typed(accessor),
                                        RUN_F,
                                        WALK));
        DASH =
                builder.nextAccessor(
                        "biped/dash",
                        accessor -> new StaticAnimation(true, typed(accessor), Armatures.BIPED));
        JUMP =
                builder.nextAccessor(
                        "biped/jump",
                        accessor ->
                                new StaticAnimation(0.15F, false, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.2F)));
        FALL =
                builder.nextAccessor(
                        "biped/fall",
                        accessor ->
                                new StaticAnimation(0.15F, true, typed(accessor), Armatures.BIPED));
        DODGE_F1 =
                builder.nextAccessor(
                        "biped/dodge/dodge_f1",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F, 0.4F, typed(accessor), 0.6F, 0.8F, Armatures.BIPED));
        DODGE_B1 =
                builder.nextAccessor(
                        "biped/dodge/dodge_b1",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F, 0.4F, typed(accessor), 0.6F, 0.8F, Armatures.BIPED));
        DODGE_R1 =
                builder.nextAccessor(
                        "biped/dodge/dodge_r1",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F, 0.4F, typed(accessor), 0.6F, 0.8F, Armatures.BIPED));
        DODGE_L1 =
                builder.nextAccessor(
                        "biped/dodge/dodge_l1",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F, 0.4F, typed(accessor), 0.6F, 0.8F, Armatures.BIPED));
        DODGE_F2 =
                builder.nextAccessor(
                        "biped/dodge/dodge_f2",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F, 0.4F, typed(accessor), 0.6F, 0.8F, Armatures.BIPED));
        DODGE_B2 =
                builder.nextAccessor(
                        "biped/dodge/dodge_b2",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F, 0.4F, typed(accessor), 0.6F, 0.8F, Armatures.BIPED));
        DODGE_R2 =
                builder.nextAccessor(
                        "biped/dodge/dodge_r2",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F, 0.4F, typed(accessor), 0.6F, 0.8F, Armatures.BIPED));
        DODGE_L2 =
                builder.nextAccessor(
                        "biped/dodge/dodge_l2",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F, 0.4F, typed(accessor), 0.6F, 0.8F, Armatures.BIPED));
        DODGE_F3 =
                builder.nextAccessor(
                        "biped/dodge/dodge_f3",
                        accessor ->
                                new WukongDodgeAnimation(
                                                0.1F,
                                                0.6F,
                                                typed(accessor),
                                                0.6F,
                                                1.35F,
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true));
        DODGE_B3 =
                builder.nextAccessor(
                        "biped/dodge/dodge_b3",
                        accessor ->
                                new WukongDodgeAnimation(
                                                0.1F,
                                                0.6F,
                                                typed(accessor),
                                                0.6F,
                                                1.35F,
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true));
        DODGE_R3 =
                builder.nextAccessor(
                        "biped/dodge/dodge_r3",
                        accessor ->
                                new WukongDodgeAnimation(
                                                0.1F,
                                                0.6F,
                                                typed(accessor),
                                                0.6F,
                                                1.35F,
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true));
        DODGE_L3 =
                builder.nextAccessor(
                        "biped/dodge/dodge_l3",
                        accessor ->
                                new WukongDodgeAnimation(
                                                0.1F,
                                                0.6F,
                                                typed(accessor),
                                                0.6F,
                                                1.35F,
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true));
        DODGE_FP =
                builder.nextAccessor(
                        "biped/dodge/dodge_fp",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F,
                                        0.63F,
                                        typed(accessor),
                                        0.6F,
                                        1.35F,
                                        Armatures.BIPED,
                                        true));
        DODGE_BP =
                builder.nextAccessor(
                        "biped/dodge/dodge_bp",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F,
                                        0.63F,
                                        typed(accessor),
                                        0.6F,
                                        1.35F,
                                        Armatures.BIPED,
                                        true));
        DODGE_RP =
                builder.nextAccessor(
                        "biped/dodge/dodge_rp",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F,
                                        0.63F,
                                        typed(accessor),
                                        0.6F,
                                        1.35F,
                                        Armatures.BIPED,
                                        true));
        DODGE_LP =
                builder.nextAccessor(
                        "biped/dodge/dodge_lp",
                        accessor ->
                                new WukongDodgeAnimation(
                                        0.1F,
                                        0.63F,
                                        typed(accessor),
                                        0.6F,
                                        1.35F,
                                        Armatures.BIPED,
                                        true));

        STAFF_AUTO1_DASH =
                builder.nextAccessor(
                        "biped/auto_1_dash",
                        accessor ->
                                new BasicAttackAnimation(
                                                0.15F,
                                                0.2916F,
                                                0.5000F,
                                                0.51F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(0.9F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.8F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                // 冲刺攻击重置普攻计数器
                                                                BasicAttack
                                                                        .setComboCounterWithEvent(
                                                                                ComboCounterHandleEvent
                                                                                        .Causal
                                                                                        .ANOTHER_ACTION_ANIMATION,
                                                                                serverPlayerPatch,
                                                                                serverPlayerPatch
                                                                                        .getSkill(
                                                                                                SkillSlots
                                                                                                        .BASIC_ATTACK),
                                                                                typedMain(
                                                                                        STAFF_AUTO1_DASH),
                                                                                1);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        STAFF_AUTO1 =
                builder.nextAccessor(
                        "biped/auto_1",
                        accessor ->
                                new BasicAttackAnimation(
                                                0.15F,
                                                0.2916F,
                                                0.5000F,
                                                0.51F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(0.9F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.8F))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.STAFF1.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(0F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.reset(0.2F))));

        STAFF_AUTO2 =
                builder.nextAccessor(
                        "biped/auto_2",
                        accessor ->
                                new BasicAttackAnimation(
                                                0.15F,
                                                0.6667F,
                                                0.875F,
                                                0.875F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(1.25F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.8F))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.STAFF2.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(0F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.reset(0.2F))));

        STAFF_AUTO3 =
                builder.nextAccessor(
                        "biped/auto_3",
                        accessor ->
                                new BasicMultipleAttackAnimation(
                                                0.15F,
                                                typed(accessor),
                                                Armatures.BIPED,
                                                new AttackAnimation.Phase(
                                                                0.0F,
                                                                0.25F,
                                                                0.4583F,
                                                                0.4583F,
                                                                0.4583F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(1.0F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .MAX_STRIKES_MODIFIER,
                                                                ValueModifier.setter(4.0F)),
                                                new AttackAnimation.Phase(
                                                                0.4583F,
                                                                0.4583F,
                                                                0.7083F,
                                                                0.7083F,
                                                                3.3333F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(1.0F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .MAX_STRIKES_MODIFIER,
                                                                ValueModifier.setter(4.0F)))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.2F))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(0F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.reset(0.2F))));
        STAFF_AUTO4 =
                builder.nextAccessor(
                        "biped/auto_4",
                        accessor ->
                                new BasicMultipleAttackAnimation(
                                                0.15F,
                                                typed(accessor),
                                                Armatures.BIPED,
                                                new AttackAnimation.Phase(
                                                                0.0F,
                                                                0.1F,
                                                                0.2F,
                                                                0.2F,
                                                                0.2F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.5F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .SWING_SOUND,
                                                                EpicFightSounds.WHOOSH_ROD.get()),
                                                new AttackAnimation.Phase(
                                                                0.2F,
                                                                0.2F,
                                                                0.4F,
                                                                0.4F,
                                                                0.4F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.5F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .SWING_SOUND,
                                                                EpicFightSounds.WHOOSH_ROD.get()),
                                                new AttackAnimation.Phase(
                                                                0.4F,
                                                                0.4F,
                                                                0.6F,
                                                                0.6F,
                                                                0.6F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.5F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .SWING_SOUND,
                                                                EpicFightSounds.WHOOSH_ROD.get()),
                                                new AttackAnimation.Phase(
                                                                0.6F,
                                                                0.6F,
                                                                0.8F,
                                                                0.8F,
                                                                0.8F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.5F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .SWING_SOUND,
                                                                EpicFightSounds.WHOOSH_ROD.get()),
                                                new AttackAnimation.Phase(
                                                                0.8F,
                                                                1.0416F,
                                                                1.125F,
                                                                1.2583F,
                                                                2.5F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(1.0F))
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .IMPACT_MODIFIER,
                                                                ValueModifier.multiplier(5F)))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.STUN_TYPE,
                                                StunType.KNOCKDOWN)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.2F))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        1.125F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            LivingEntity self =
                                                                    livingEntityPatch.getOriginal();
                                                            if (self.getMainHandItem()
                                                                    .is(
                                                                            WukongItems.KANG_JIN
                                                                                    .get())) {
                                                                if (livingEntityPatch.getTarget()
                                                                                != null
                                                                        && self.level()
                                                                                instanceof
                                                                                ServerLevel
                                                                                        serverLevel) {
                                                                    EntityType.LIGHTNING_BOLT.spawn(
                                                                            serverLevel,
                                                                            livingEntityPatch
                                                                                    .getTarget()
                                                                                    .getOnPos(),
                                                                            MobSpawnType.TRIGGERED);
                                                                }
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.STAFF4.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(0F, 1F, 1F, 1F, 0F, 0F, 0F),
                                                        ScaleTime.reset(0.2F))));
        final AnimationEvent.InTimeEvent[][] attack5Events = new AnimationEvent.InTimeEvent[1][];
        STAFF_AUTO5 =
                builder.nextAccessor(
                        "biped/auto_5",
                        accessor ->
                                new BasicAttackAnimation(
                                                0.01F,
                                                0.9166F,
                                                1.15F,
                                                1.9833F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(3.0F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.STUN_TYPE,
                                                StunType.LONG)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.SWING_SOUND,
                                                EpicFightSounds.WHOOSH_BIG.get())
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(2.0F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.01F, 1.9833F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_BEGIN,
                                                MoveCoordFunctions.TRACE_TARGET_LOCATION_ROTATION)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_TICK,
                                                MoveCoordFunctions.TRACE_TARGET_LOCATION_ROTATION)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.2F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) ->
                                                                livingEntityPatch.playSound(
                                                                        EpicFightSounds.ENTITY_MOVE
                                                                                .get(),
                                                                        1,
                                                                        1)),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationEvent.InPeriodEvent.create(
                                                        0.01F,
                                                        1.9833F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                            instanceof
                                                                            ServerPlayerPatch
                                                                                    playerPatch
                                                                    && WukongWeaponCategories
                                                                            .isWeaponValid(
                                                                                    playerPatch)) {
                                                                SkillDataManager dataManager =
                                                                        playerPatch
                                                                                .getSkill(
                                                                                        SkillSlots
                                                                                                .WEAPON_INNATE)
                                                                                .getDataManager();
                                                                if (dataManager.hasData(
                                                                        WukongSkillDataKeys
                                                                                .DAMAGE_REDUCE
                                                                                .get())) {
                                                                    dataManager.setData(
                                                                            WukongSkillDataKeys
                                                                                    .DAMAGE_REDUCE
                                                                                    .get(),
                                                                            0.5F);
                                                                }
                                                                if (dataManager.hasData(
                                                                        WukongSkillDataKeys
                                                                                .THRUST_PROTECT_NEXT_FALL
                                                                                .get())) {
                                                                    dataManager.setData(
                                                                            WukongSkillDataKeys
                                                                                    .THRUST_PROTECT_NEXT_FALL
                                                                                    .get(),
                                                                            true);
                                                                }
                                                                if (dataManager.hasData(
                                                                        WukongSkillDataKeys
                                                                                .PROTECT_NEXT_FALL
                                                                                .get())) {
                                                                    dataManager.setData(
                                                                            WukongSkillDataKeys
                                                                                    .PROTECT_NEXT_FALL
                                                                                    .get(),
                                                                            true);
                                                                }
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(attack5Events[0]));

        List<AnimationEvent.InTimeEvent> Attack5 =
                append(
                        AnimationEvent.InTimeEvent.create(
                                1F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.wave5.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.of(1F, 1, 1F, 1F, 0F, 0F, 0F), ScaleTime.reset(1F)));
        Attack5.add(
                AnimationEvent.InTimeEvent.create(
                                1F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0F, 0F, -2.3F),
                                yesman.epicfight.gameasset.Armatures.BIPED.get().rootJoint,
                                1.5D,
                                0.01F));
        attack5Events[0] = Attack5.toArray(new AnimationEvent.InTimeEvent[0]);

        JUMP_ATTACK_LIGHT =
                builder.nextAccessor(
                        "biped/jump_attack/jump_light_pre",
                        accessor ->
                                new WukongJumpAttackAnimation(
                                                0.10F,
                                                0.13F,
                                                0.40F,
                                                0.50F,
                                                WukongColliders.JUMP_ATTACK_LIGHT,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(1.45F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .MAX_STRIKES_MODIFIER,
                                                ValueModifier.setter(1)) // 最多踹一个
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.01F, 0.10F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.8F)));
        JUMP_ATTACK_LIGHT_HIT =
                builder.nextAccessor(
                        "biped/jump_attack/jump_light_hit",
                        accessor ->
                                new ActionAnimation(0.15F, typed(accessor), Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.01F, 0.3F))
                                        .addState(
                                                EntityState.CAN_SKILL_EXECUTION,
                                                true) // 为了可以用重击取消后摇
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.0F)));
        JUMP_ATTACK_HEAVY =
                builder.nextAccessor(
                        "biped/jump_attack/jump_heavy",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.01F,
                                                0.54F,
                                                0.67F,
                                                1.25F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.STUN_TYPE,
                                                StunType.LONG)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.SWING_SOUND,
                                                EpicFightSounds.WHOOSH_BIG.get())
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(2.0F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.01F, 0.67F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.0F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) ->
                                                                livingEntityPatch.playSound(
                                                                        EpicFightSounds.ROLL.get(),
                                                                        1,
                                                                        1)),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationEvent.InPeriodEvent.create(
                                                        0.01F,
                                                        0.5F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                            instanceof
                                                                            ServerPlayerPatch
                                                                                    playerPatch
                                                                    && WukongWeaponCategories
                                                                            .isWeaponValid(
                                                                                    playerPatch)) {
                                                                setWeaponInnateDataIfRegistered(
                                                                        playerPatch,
                                                                        WukongSkillDataKeys
                                                                                .DAMAGE_REDUCE
                                                                                .get(),
                                                                        0.5F);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        ;

        STAFF_SPIN_ONE_HAND_LOOP =
                builder.nextAccessor(
                        "biped/staff_spin/staff_spin_one_hand",
                        accessor ->
                                new StaffSpinAttackAnimation(
                                        1.25F, typed(accessor), biped, 0.05F, false));
        STAFF_SPIN_TWO_HAND_LOOP =
                builder.nextAccessor(
                        "biped/staff_spin/staff_spin_two_hand",
                        accessor ->
                                new StaffSpinAttackAnimation(
                                        0.83F, typed(accessor), biped, 0.08F, true));

        // 劈start
        // 前摇完自动接下一个动作
        SMASH_CHARGING_PRE =
                builder.nextAccessor(
                        "biped/smash/smash_charge_pre",
                        accessor ->
                                new ActionAnimation(0.15F, typed(accessor), Armatures.BIPED)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.reserveAnimation(
                                                                    SMASH_CHARGING_LOOP_STAND);
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setDataSync(
                                                                                WukongSkillDataKeys
                                                                                        .IS_CHARGING
                                                                                        .get(),
                                                                                true);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.1F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    EpicFightSounds.WHOOSH_ROD
                                                                            .get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER),
                                                AnimationEvent.InTimeEvent.create(
                                                        0.2F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                if (serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .getDataValue(
                                                                                WukongSkillDataKeys
                                                                                        .KEY_PRESSING
                                                                                        .get()))
                                                                    livingEntityPatch.playSound(
                                                                            WuKongSounds
                                                                                    .XULI_LEVEL_RISE03
                                                                                    .get(),
                                                                            0,
                                                                            0);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER),
                                                AnimationEvent.InTimeEvent.create(
                                                        0.3F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    EpicFightSounds.WHOOSH_ROD
                                                                            .get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        SMASH_CHARGING_LOOP_STAND =
                builder.nextAccessor(
                        "biped/smash/smash_charging",
                        accessor ->
                                new StaticAnimation(0.15F, true, typed(accessor), Armatures.BIPED));

        SMASH_CHARGED0 =
                builder.nextAccessor(
                        "biped/smash/smash_heavy0",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.15F,
                                                0.75F,
                                                0.92F,
                                                1.67F,
                                                WukongColliders.STACK_0_1,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.STUN_TYPE,
                                                StunType.LONG)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(2.0F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(2.6F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.01F, 0.75F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.0F))
                                        .addEvents(allStopMovement)
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.HIT_GROUND.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        SMASH_CHARGED1 =
                builder.nextAccessor(
                        "biped/smash/smash_heavy1",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.15F,
                                                0.75F,
                                                0.92F,
                                                1.67F,
                                                WukongColliders.STACK_0_1,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(2.5F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(5.6F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.01F, 0.75F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.0F))
                                        .addEvents(allStopMovement)
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.083F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            livingEntityPatch.playSound(
                                                                    WuKongSounds.HIT_GROUND.get(),
                                                                    1,
                                                                    1);
                                                        }),
                                                        AnimationEvent.Side.SERVER)));
        final AnimationEvent.InTimeEvent[][] smashCharged2Events =
                new AnimationEvent.InTimeEvent[1][];
        SMASH_CHARGED2 =
                builder.nextAccessor(
                        "biped/smash/smash_heavy2",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.15F,
                                                1.30F,
                                                1.55F,
                                                2.5F,
                                                WukongColliders.STACK_2,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .MAX_STRIKES_MODIFIER,
                                                ValueModifier.setter(4.0F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.SWING_SOUND,
                                                EpicFightSounds.WHOOSH_BIG.get())
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(8.8F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.01F, 1.30F))
                                        .newTimePair(0, 2.5F)
                                        .addState(EntityState.TURNING_LOCKED, true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.0F))
                                        .addEvents(
                                                AnimationEvent.InPeriodEvent.create(
                                                        0.01F,
                                                        2.5F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                            instanceof
                                                                            ServerPlayerPatch
                                                                                    playerPatch
                                                                    && WukongWeaponCategories
                                                                            .isWeaponValid(
                                                                                    playerPatch)) {
                                                                setWeaponInnateDataIfRegistered(
                                                                        playerPatch,
                                                                        WukongSkillDataKeys
                                                                                .DAMAGE_REDUCE
                                                                                .get(),
                                                                        0.5F);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(smashCharged2Events[0]));
        ;
        List<AnimationEvent.InTimeEvent> sc2List =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.292F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.reset(1.30F),
                                ScaleTime.of(1.45F, 1, 1.8F, 1, 0F, 0F, 0F),
                                ScaleTime.of(2.13F, 1, 1.8F, 1, 0F, 0F, 0F),
                                ScaleTime.reset(2.29F)));
        sc2List.add(
                AnimationEvent.InTimeEvent.create(
                        0.833F,
                        ((livingEntityPatch, staticAnimation, objects) ->
                                livingEntityPatch.playSound(WuKongSounds.HIT_GROUND.get(), 1, 1)),
                        AnimationEvent.Side.SERVER));
        smashCharged2Events[0] = sc2List.toArray(new AnimationEvent.InTimeEvent[0]);

        final AnimationEvent.InTimeEvent[][] smashCharged3Events =
                new AnimationEvent.InTimeEvent[1][];
        SMASH_CHARGED3 =
                builder.nextAccessor(
                        "biped/smash/smash_heavy3",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.15F,
                                                1.792F,
                                                1.958F,
                                                2.667F,
                                                WukongColliders.STACK_3,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .MAX_STRIKES_MODIFIER,
                                                ValueModifier.setter(6.0F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.STUN_TYPE,
                                                StunType.NEUTRALIZE)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.SWING_SOUND,
                                                EpicFightSounds.WHOOSH_BIG.get())
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.HIT_SOUND,
                                                EpicFightSounds.BLUNT_HIT_HARD.get())
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(3.5F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(11))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.01F, 1.30F))
                                        .newTimePair(0, 2.667F)
                                        .addState(EntityState.TURNING_LOCKED, true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.0F))
                                        .addEvents(
                                                AnimationEvent.InPeriodEvent.create(
                                                        0.01F,
                                                        2.6667F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                            instanceof
                                                                            ServerPlayerPatch
                                                                                    playerPatch
                                                                    && WukongWeaponCategories
                                                                            .isWeaponValid(
                                                                                    playerPatch)) {
                                                                setWeaponInnateDataIfRegistered(
                                                                        playerPatch,
                                                                        WukongSkillDataKeys
                                                                                .DAMAGE_REDUCE
                                                                                .get(),
                                                                        0.7F);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(smashCharged3Events[0]));
        List<AnimationEvent.InTimeEvent> sc3List =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.292F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.reset(1.667F),
                                ScaleTime.of(1.792F, 1, 2.4F, 1, 0F, 0F, 0F),
                                ScaleTime.of(1.958F, 1, 2.4F, 1, 0F, 0F, 0F),
                                ScaleTime.of(2.667F, 1, 2F, 1, 0F, 0F, 0F),
                                ScaleTime.reset(2.8F)));
        sc3List.add(
                AnimationEvent.InTimeEvent.create(
                        0.833F,
                        ((livingEntityPatch, staticAnimation, objects) ->
                                livingEntityPatch.playSound(WuKongSounds.HIT_GROUND.get(), 1, 1)),
                        AnimationEvent.Side.SERVER));
        sc3List.add(
                AnimationEvent.InTimeEvent.create(
                        1.125F,
                        ((livingEntityPatch, staticAnimation, objects) ->
                                livingEntityPatch.playSound(WuKongSounds.HIT_GROUND.get(), 1, 1)),
                        AnimationEvent.Side.SERVER));
        smashCharged3Events[0] = sc3List.toArray(new AnimationEvent.InTimeEvent[0]);

        final AnimationEvent.InTimeEvent[][] smashCharged4Events =
                new AnimationEvent.InTimeEvent[1][];
        SMASH_CHARGED4 =
                builder.nextAccessor(
                        "biped/smash/smash_heavy4",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.15F,
                                                2.63F,
                                                2.8F,
                                                3.3F,
                                                WukongColliders.STACK_4,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .MAX_STRIKES_MODIFIER,
                                                ValueModifier.multiplier(2.0F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.STUN_TYPE,
                                                StunType.NEUTRALIZE)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.SWING_SOUND,
                                                EpicFightSounds.WHOOSH_BIG.get())
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.HIT_SOUND,
                                                EpicFightSounds.BLUNT_HIT_HARD.get())
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(4.0F))
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(15.5F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.01F, 2.75F))
                                        .newTimePair(0, 3.3F)
                                        .addState(EntityState.TURNING_LOCKED, true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.3F))
                                        .addEvents(
                                                AnimationEvent.InPeriodEvent.create(
                                                        0.01F,
                                                        3.3F,
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                            instanceof
                                                                            ServerPlayerPatch
                                                                                    playerPatch
                                                                    && WukongWeaponCategories
                                                                            .isWeaponValid(
                                                                                    playerPatch)) {
                                                                setWeaponInnateDataIfRegistered(
                                                                        playerPatch,
                                                                        WukongSkillDataKeys
                                                                                .DAMAGE_REDUCE
                                                                                .get(),
                                                                        0.9F);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(smashCharged4Events[0]));
        ;
        List<AnimationEvent.InTimeEvent> sc4List =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.208F,
                                ((livingEntityPatch, anim, obj) ->
                                        livingEntityPatch.playSound(
                                                WuKongSounds.HIT_GROUND.get(), 1, 1)),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                ScaleTime.of(2.4167F, 1, 1, 1, 0F, 0F, 0F),
                                ScaleTime.of(2.5833F, 1F, 1.15F, 1F, 0F, 0F, 0F),
                                ScaleTime.of(2.7083F, 1.5F, 3.15F, 1.15F, 0F, 0F, 0F),
                                ScaleTime.of(3.3333F, 1.5F, 3.15F, 1.5F, 0F, 0F, 0F),
                                ScaleTime.of(3.5833F, 1, 1, 1, 0F, 0F, 0F)));
        sc4List.add(
                AnimationEvent.InTimeEvent.create(
                        2.8F,
                        ((livingEntityPatch, staticAnimation, objects) -> {
                            LivingEntity entity = livingEntityPatch.getOriginal();
                            Vec3 viewVec = entity.getViewVector(1.0F);
                            Vec3 hVec = viewVec.add(0, -viewVec.y, 0);
                            Vec3 target =
                                    entity.position().add(hVec.normalize().scale(4)).add(0, -2, 0);
                            LevelUtil.circleSlamFracture(entity, entity.level(), target, 3.0);
                        }),
                        AnimationEvent.Side.SERVER));
        smashCharged4Events[0] = sc4List.toArray(new AnimationEvent.InTimeEvent[0]);

        SMASH_DERIVE1 =
                builder.nextAccessor(
                        "biped/smash/smash_special1",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.15F,
                                                0.63F,
                                                0.75F,
                                                1.20F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.STUN_TYPE,
                                                StunType.HOLD)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(4.0F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.0F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                SkillDataManager dataManager =
                                                                        serverPlayerPatch
                                                                                .getSkill(
                                                                                        SkillSlots
                                                                                                .WEAPON_INNATE)
                                                                                .getDataManager();
                                                                dataManager.setDataSync(
                                                                        WukongSkillDataKeys
                                                                                .CAN_FIRST_DERIVE
                                                                                .get(),
                                                                        false);
                                                                dataManager.setDataSync(
                                                                        WukongSkillDataKeys
                                                                                .IS_IN_SPECIAL_ATTACK
                                                                                .get(),
                                                                        true);
                                                                dataManager.setDataSync(
                                                                        WukongSkillDataKeys
                                                                                .IS_SPECIAL_SUCCESS
                                                                                .get(),
                                                                        false);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setDataSync(
                                                                                WukongSkillDataKeys
                                                                                        .IS_IN_SPECIAL_ATTACK
                                                                                        .get(),
                                                                                false);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        ScaleTime.of(
                                                                0.625F, 1, 1.8F, 1, 0F, 0F, 0F),
                                                        ScaleTime.of(
                                                                1.125F, 1, 1.8F, 1, 0F, 0F, 0F),
                                                        ScaleTime.reset(1.25F))));

        SMASH_DERIVE2 =
                builder.nextAccessor(
                        "biped/smash/smash_special2",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.15F,
                                                1.04F,
                                                1.71F,
                                                2.30F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty.STUN_TYPE,
                                                StunType.LONG)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .IMPACT_MODIFIER,
                                                ValueModifier.multiplier(3.0F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.01F, 1.04F))
                                        .newTimePair(0.01F, 1.71F)
                                        .addState(
                                                EntityState.ATTACK_RESULT,
                                                (damageSource) -> AttackResult.ResultType.MISSED)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                ((dynamicAnimation, livingEntityPatch, v, v1, v2) ->
                                                        1.0F))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        ((livingEntityPatch,
                                                                staticAnimation,
                                                                objects) -> {
                                                            if (livingEntityPatch
                                                                    instanceof
                                                                    ServerPlayerPatch
                                                                            serverPlayerPatch) {
                                                                serverPlayerPatch
                                                                        .getSkill(
                                                                                SkillSlots
                                                                                        .WEAPON_INNATE)
                                                                        .getDataManager()
                                                                        .setDataSync(
                                                                                WukongSkillDataKeys
                                                                                        .CAN_SECOND_DERIVE
                                                                                        .get(),
                                                                                false);
                                                            }
                                                        }),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                append(
                                                                AnimationEvent.InTimeEvent.create(
                                                                        0.042F,
                                                                        ((livingEntityPatch,
                                                                                anim,
                                                                                obj) ->
                                                                                livingEntityPatch
                                                                                        .playSound(
                                                                                                WuKongSounds
                                                                                                        .HIT_GROUND
                                                                                                        .get(),
                                                                                                1,
                                                                                                1)),
                                                                        AnimationEvent.Side.SERVER),
                                                                getScaleEvents(
                                                                        ScaleTime.of(
                                                                                0.042F, 1, 1.583F,
                                                                                1, 0F, 0F, 0F),
                                                                        ScaleTime.of(
                                                                                0.083F, 1, 1.758F,
                                                                                1, 0F, 0F, 0F),
                                                                        ScaleTime.of(
                                                                                0.167F, 1, 1.952F,
                                                                                1, 0F, 0F, 0F),
                                                                        ScaleTime.of(
                                                                                0.208F, 1, 2, 1, 0F,
                                                                                0F, 0F),
                                                                        ScaleTime.of(
                                                                                1.458F, 1, 2, 1, 0F,
                                                                                0F, 0F),
                                                                        ScaleTime.reset(1.460F)))
                                                        .toArray(
                                                                new AnimationEvent.InTimeEvent
                                                                        [0])));
        // 劈end

    }

    // 对目标施加来自玩家的水平方向击退, 强度由 knockbackStrength 控制
    public void applyKnockback(ServerPlayer player, Entity target, double knockbackStrength) {
        if (target instanceof LivingEntity) {
            double directionX = target.getX() - player.getX();
            double directionZ = target.getZ() - player.getZ();
            double distance = Math.sqrt(directionX * directionX + directionZ * directionZ);

            if (distance > 0.1) {
                directionX /= distance; // 归一化方向
                directionZ /= distance;
                target.push(directionX * knockbackStrength, 0.0, directionZ * knockbackStrength);
            }
        }
    }

    // 通过后台调度线程平滑调整客户端视野(FOV), 每 10ms 一帧, 在 durationTicks 内从当前 FOV 插值到目标 FOV, 重复 repeatTimes 次, 上限
    // 97
    public static void CameraOperationFov(
            float increaseAmount, int durationTicks, int repeatTimes) {
        Minecraft MC = Minecraft.getInstance();
        float startFov = MC.options.fov().get();
        float targetFov = Math.min(startFov + increaseAmount, 97F);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger tickCount = new AtomicInteger(0);
        AtomicInteger repeatCount = new AtomicInteger(0);
        Runnable task =
                new Runnable() {
                    private float currentStartFov = startFov;

                    @Override
                    public void run() {
                        int currentTick = tickCount.incrementAndGet();
                        if (currentTick > durationTicks) {
                            tickCount.set(0);
                            repeatCount.incrementAndGet();
                            if (repeatCount.get() >= repeatTimes) {
                                scheduler.shutdown();
                                return;
                            }
                            currentStartFov = MC.options.fov().get();
                        }
                        float progress = (float) currentTick / durationTicks;
                        float newFov = currentStartFov + (targetFov - currentStartFov) * progress;
                        MC.options.fov().set((int) newFov);
                    }
                };

        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.MILLISECONDS);
    }

    // 若主手是悟空棍, 在物品 nbt 中写入特效剩余时间(单位 tick)
    public static void addItemEffectTimer(ServerPlayer serverPlayer, int leftTime) {
        serverPlayer
                .getMainHandItem()
                .getCapability(EpicFightCapabilities.CAPABILITY_ITEM)
                .ifPresent(
                        (capabilityItem -> {
                            if (capabilityItem
                                    .getWeaponCategory()
                                    .equals(WukongWeaponCategories.WK_STAFF)) {
                                serverPlayer
                                        .getMainHandItem()
                                        .getOrCreateTag()
                                        .putInt(WukongMoveset.ITEM_HAS_EFFECT_TIMER_KEY, leftTime);
                            }
                        }));
    }

    // 把新事件 e 追加到旧事件数组末尾并返回新列表
    public static List<AnimationEvent.InTimeEvent> append(
            AnimationEvent.InTimeEvent e, AnimationEvent.InTimeEvent... oldArr) {
        List<AnimationEvent.InTimeEvent> list = new ArrayList<>(List.of(oldArr));
        list.add(e);
        return list;
    }

    // 生成逐 tick 的物品缩放/位移动画事件, 写入物品 nbt 供
    // ItemRendererMixin(com.p1nero.wukong.mixin.ItemRendererMixin) 读取渲染; 首尾 tick 复位缩放, 中间按插值结果设置
    public static AnimationEvent.InTimeEvent[] getScaleEvents(ScaleTime... ticks) {
        int lastTick = ticks[ticks.length - 1].tick;
        AnimationEvent.InTimeEvent[] timeStampedEvents = new AnimationEvent.InTimeEvent[lastTick];
        ticks = interpolate(ticks, lastTick);

        // 首个事件: 不缩放, 不位移
        timeStampedEvents[0] =
                AnimationEvent.InTimeEvent.create(
                        0.01F,
                        ((livingEntityPatch, staticAnimation, objects) -> {
                            if (!WukongWeaponCategories.isWeaponValid(livingEntityPatch)) {
                                return;
                            }
                            CompoundTag tag =
                                    livingEntityPatch
                                            .getOriginal()
                                            .getMainHandItem()
                                            .getOrCreateTag();
                            tag.putBoolean("WK_shouldScaleItem", false);
                            tag.putBoolean("WK_shouldTranslateItem", false);
                        }),
                        AnimationEvent.Side.CLIENT);

        // 最后一个事件: 不缩放, 不位移
        timeStampedEvents[lastTick - 1] =
                AnimationEvent.InTimeEvent.create(
                        0.05F * lastTick,
                        ((livingEntityPatch, staticAnimation, objects) -> {
                            if (!WukongWeaponCategories.isWeaponValid(livingEntityPatch)) {
                                return;
                            }
                            CompoundTag tag =
                                    livingEntityPatch
                                            .getOriginal()
                                            .getMainHandItem()
                                            .getOrCreateTag();
                            tag.putBoolean("WK_shouldScaleItem", false);
                            tag.putBoolean("WK_shouldTranslateItem", false);
                        }),
                        AnimationEvent.Side.CLIENT);

        // 中间事件: 使用插值后的缩放/位移数据
        for (int i = 1; i < lastTick - 1; i++) {
            float x = ticks[i].x;
            float y = ticks[i].y;
            float z = ticks[i].z;
            float tx = ticks[i].tx;
            float ty = ticks[i].ty;
            float tz = ticks[i].tz;

            timeStampedEvents[i] =
                    AnimationEvent.InTimeEvent.create(
                            0.05F * i,
                            ((livingEntityPatch, staticAnimation, objects) -> {
                                if (!WukongWeaponCategories.isWeaponValid(livingEntityPatch)) {
                                    return;
                                }
                                CompoundTag tag =
                                        livingEntityPatch
                                                .getOriginal()
                                                .getMainHandItem()
                                                .getOrCreateTag();
                                tag.putBoolean("WK_shouldScaleItem", true);
                                tag.putBoolean("WK_shouldTranslateItem", true);
                                tag.putFloat("WK_XScale", x);
                                tag.putFloat("WK_YScale", y);
                                tag.putFloat("WK_ZScale", z);
                                tag.putFloat("WK_XTranslation", tx);
                                tag.putFloat("WK_YTranslation", ty);
                                tag.putFloat("WK_ZTranslation", tz);
                            }),
                            AnimationEvent.Side.CLIENT);
        }

        return timeStampedEvents;
    }

    // 插值处理: 对 0~lastTick 之间每个 tick 求缩放/位移数据
    // scaleTimes 为已知的锚点时间点(按 tick), lastTick 为最后一个 tick
    // 返回插值补全后的数据数组
    public static ScaleTime[] interpolate(ScaleTime[] scaleTimes, int lastTick) {
        ScaleTime[] results = new ScaleTime[lastTick + 1];

        // 先填充已知锚点值
        for (ScaleTime scaleTime : scaleTimes) {
            if (scaleTime.tick <= lastTick) {
                results[scaleTime.tick] = scaleTime;
            }
        }

        // 对缺失的 tick 做线性插值
        for (int i = 0; i <= lastTick; i++) {
            if (results[i] == null) {
                // 向前向后寻找最近的两个锚点
                ScaleTime before = null;
                ScaleTime after = null;

                for (int j = i - 1; j >= 0; j--) {
                    if (results[j] != null) {
                        before = results[j];
                        break;
                    }
                }

                for (int j = i + 1; j <= lastTick; j++) {
                    if (results[j] != null) {
                        after = results[j];
                        break;
                    }
                }

                if (before != null && after != null) {
                    // 对缩放与位移分别做线性插值
                    float t = (float) (i - before.tick) / (after.tick - before.tick);
                    float x = before.x + t * (after.x - before.x);
                    float y = before.y + t * (after.y - before.y);
                    float z = before.z + t * (after.z - before.z);
                    float tx = before.tx + t * (after.tx - before.tx);
                    float ty = before.ty + t * (after.ty - before.ty);
                    float tz = before.tz + t * (after.tz - before.tz);
                    results[i] = new ScaleTime(i, x, y, z, tx, ty, tz);
                }
            }
        }

        // 剩余空洞用最近的已知值前向填充
        for (int i = 0; i <= lastTick; i++) {
            if (results[i] == null) {
                if (i > 0) {
                    results[i] = results[i - 1]; // 复制上一个已知值
                } else {
                    results[i] = new ScaleTime(i, 1, 1, 1, 0, 0, 0);
                }
            }
        }

        return results;
    }

    // 单个 tick 的缩放/位移数据: x/y/z 为缩放比例, tx/ty/tz 为位移, tick 为触发时刻(按 tick)
    public record ScaleTime(int tick, float x, float y, float z, float tx, float ty, float tz) {
        // 以秒为单位的时间构造缩放数据, 内部换算为 tick
        public static ScaleTime of(
                float time, float x, float y, float z, float tx, float ty, float tz) {
            return new ScaleTime(((int) (time * 20)), x, y, z, tx, ty, tz);
        }

        // 构造一个复位(无缩放/无位移)的缩放数据, 时间换算为 tick
        public static ScaleTime reset(float time) {
            return new ScaleTime(((int) (time * 20)), 1, 1, 1, 0, 0, 0);
        }
    }

    // 玩家每 tick 递减主手棍的物品特效计时
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer serverPlayer) {
            serverPlayer
                    .getMainHandItem()
                    .getCapability(EpicFightCapabilities.CAPABILITY_ITEM)
                    .ifPresent(
                            (capabilityItem -> {
                                if (capabilityItem
                                        .getWeaponCategory()
                                        .equals(WukongWeaponCategories.WK_STAFF)) {
                                    CompoundTag mainHandItem =
                                            serverPlayer.getMainHandItem().getOrCreateTag();
                                    mainHandItem.putInt(
                                            WukongMoveset.ITEM_HAS_EFFECT_TIMER_KEY,
                                            Math.max(
                                                    0,
                                                    mainHandItem.getInt(
                                                                    WukongMoveset
                                                                            .ITEM_HAS_EFFECT_TIMER_KEY)
                                                            - 1));
                                }
                            }));
        }
    }
}
