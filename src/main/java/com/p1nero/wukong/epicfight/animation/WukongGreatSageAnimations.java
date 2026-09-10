package com.p1nero.wukong.epicfight.animation;

import static com.p1nero.wukong.epicfight.animation.WukongAnimations.append;
import static com.p1nero.wukong.epicfight.animation.WukongAnimations.getScaleEvents;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.animation.custom.BasicMultipleAttackAnimation;
import com.p1nero.wukong.epicfight.animation.custom.WukongMoveCoordFunctions;
import com.p1nero.wukong.epicfight.animation.custom.WukongScaleStaffAttackAnimation;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.BattleUnit;
import com.p1nero.wukong.epicfight.weapon.WukongColliders;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.item.WukongItems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.fml.common.Mod;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.MoveCoordFunctions;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.BasicAttackAnimation;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.TimePairList;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.skill.BasicAttack;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
// 大圣形态动画注册类: 持有大圣套动画访问器, 提供基础连段与重击风格的构建逻辑
public final class WukongGreatSageAnimations {
    // 大圣套基础连段访问器(冲刺轻击与轻击 1~5)
    public static AnimationManager.AnimationAccessor STAFF_AUTO1_DASH;
    public static AnimationManager.AnimationAccessor STAFF_AUTO1;
    public static AnimationManager.AnimationAccessor STAFF_AUTO2;
    public static AnimationManager.AnimationAccessor STAFF_AUTO3;
    public static AnimationManager.AnimationAccessor STAFF_AUTO4;
    public static AnimationManager.AnimationAccessor STAFF_AUTO5;

    // 大圣套重击风格访问器(断棍/劈棍/风云/扫戳等)与蓄力重击/变身动画
    public static AnimationManager.AnimationAccessor BROKEN_STICK_STYLE;
    public static AnimationManager.AnimationAccessor CHOP_STICK_STYLE;
    public static AnimationManager.AnimationAccessor WIND_CLOUD_STYLE;
    public static AnimationManager.AnimationAccessor SWEEP_JAB_STYLE;
    public static AnimationManager.AnimationAccessor HEAVY_AUTO2_3;
    public static AnimationManager.AnimationAccessor XULI_HEAVY_4;
    public static AnimationManager.AnimationAccessor HENSHIN;

    // 旧命名别名, 分别指向风云扫式与扫戳式
    public static AnimationManager.AnimationAccessor FENG_YUN_SAO_STYLE;
    public static AnimationManager.AnimationAccessor SAO_CHUO_SHI_STYLE;

    // 工具类, 禁止实例化
    private WukongGreatSageAnimations() {}

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

    // 构建大圣形态全部动画: 先注册基础连段与重击风格, 再初始化旧命名别名
    static void build(AnimationManager.AnimationBuilder builder) {
        WukongMoveset.LOGGER.info("Registering complete Great Sage animation set");
        HumanoidArmature biped = Armatures.BIPED.get();

        registerBasicCombo(builder, biped);
        registerHeavyAttacks(builder, biped);

        FENG_YUN_SAO_STYLE = WIND_CLOUD_STYLE;
        SAO_CHUO_SHI_STYLE = SWEEP_JAB_STYLE;
    }

    // 注册大圣形态的基础连段动画: 冲刺轻击(重置连击数)与轻击 1~5, 各段配置伤害/音效/缩放等事件
    private static void registerBasicCombo(
            AnimationManager.AnimationBuilder builder, HumanoidArmature biped) {
        // 冲刺轻击: 起手冲刺攻击, 开始时将普攻连击计数重置为 1
        STAFF_AUTO1_DASH =
                builder.nextAccessor(
                        "biped/greatsage/basic/auto_1_dash",
                        accessor ->
                                new BasicAttackAnimation(
                                                0.15F,
                                                0.2916F,
                                                0.5F,
                                                0.51F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(1.25F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, previous, elapsed) ->
                                                        1.8F)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        (patch, animation, args) -> {
                                                            if (patch
                                                                    instanceof
                                                                    ServerPlayerPatch playerPatch) {
                                                                BasicAttack
                                                                        .setComboCounterWithEvent(
                                                                                ComboCounterHandleEvent
                                                                                        .Causal
                                                                                        .ANOTHER_ACTION_ANIMATION,
                                                                                playerPatch,
                                                                                playerPatch
                                                                                        .getSkill(
                                                                                                SkillSlots
                                                                                                        .BASIC_ATTACK),
                                                                                typedMain(
                                                                                        STAFF_AUTO1_DASH),
                                                                                1);
                                                            }
                                                        },
                                                        AnimationEvent.Side.SERVER)));

        // 轻击 1: 基础攻击, 附带挥棒音效与棍子缩放事件
        STAFF_AUTO1 =
                builder.nextAccessor(
                        "biped/greatsage/basic/auto_1",
                        accessor ->
                                new BasicAttackAnimation(
                                                0.15F,
                                                0.2916F,
                                                0.5F,
                                                0.51F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(1.0F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, previous, elapsed) ->
                                                        1.8F)
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.0F,
                                                        (patch, animation, args) ->
                                                                patch.playSound(
                                                                        WuKongSounds.STAFF1.get(),
                                                                        1.0F,
                                                                        1.0F),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        WukongAnimations.ScaleTime.of(
                                                                0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F,
                                                                0.0F),
                                                        WukongAnimations.ScaleTime.reset(0.2F))));

        // 轻击 2: 基础攻击, 附带挥棒音效与棍子缩放事件
        STAFF_AUTO2 =
                builder.nextAccessor(
                        "biped/greatsage/basic/auto_2",
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
                                                ValueModifier.multiplier(1.0F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .CANCELABLE_MOVE,
                                                false)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, previous, elapsed) ->
                                                        1.8F)
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.0F,
                                                        (patch, animation, args) ->
                                                                patch.playSound(
                                                                        WuKongSounds.STAFF2.get(),
                                                                        1.0F,
                                                                        1.0F),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        WukongAnimations.ScaleTime.of(
                                                                0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F,
                                                                0.0F),
                                                        WukongAnimations.ScaleTime.reset(0.2F))));

        // 轻击 3: 两段相位多段攻击, 第一段高伤且最多命中 4 次
        STAFF_AUTO3 =
                builder.nextAccessor(
                        "biped/greatsage/basic/auto_3",
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
                                                                ValueModifier.multiplier(2.0F))
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
                                                (animation, patch, speed, previous, elapsed) ->
                                                        1.2F)
                                        .addEvents(
                                                getScaleEvents(
                                                        WukongAnimations.ScaleTime.of(
                                                                0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F,
                                                                0.0F),
                                                        WukongAnimations.ScaleTime.reset(0.2F))));

        // 轻击 4: 五段相位连击, 末段高冲击力并击倒; 持镔金棍时在 1.125s 于目标处生成闪电
        STAFF_AUTO4 =
                builder.nextAccessor(
                        "biped/greatsage/basic/auto_4",
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
                                                                ValueModifier.multiplier(3.0F))
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
                                                                ValueModifier.multiplier(5.0F)))
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
                                                (animation, patch, speed, previous, elapsed) ->
                                                        1.2F)
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        1.125F,
                                                        (patch, animation, args) -> {
                                                            if (patch.getOriginal()
                                                                            .getMainHandItem()
                                                                            .is(
                                                                                    WukongItems
                                                                                            .KANG_JIN
                                                                                            .get())
                                                                    && patch.getTarget() != null
                                                                    && patch.getOriginal().level()
                                                                            instanceof
                                                                            ServerLevel
                                                                                    serverLevel) {
                                                                EntityType.LIGHTNING_BOLT.spawn(
                                                                        serverLevel,
                                                                        patch.getTarget()
                                                                                .getOnPos(),
                                                                        MobSpawnType.TRIGGERED);
                                                            }
                                                        },
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationEvent.InTimeEvent.create(
                                                        0.0F,
                                                        (patch, animation, args) ->
                                                                patch.playSound(
                                                                        WuKongSounds.STAFF4.get(),
                                                                        1.0F,
                                                                        1.0F),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                getScaleEvents(
                                                        WukongAnimations.ScaleTime.of(
                                                                0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F,
                                                                0.0F),
                                                        WukongAnimations.ScaleTime.reset(0.2F))));

        // 轻击 5 的事件列表: 1s 处播放音效/复位缩放, 并叠加客户端地面碎裂特效
        List<AnimationEvent.InTimeEvent> auto5Events =
                append(
                        AnimationEvent.InTimeEvent.create(
                                1.0F,
                                (patch, animation, args) ->
                                        patch.playSound(WuKongSounds.wave5.get(), 1.0F, 1.0F),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                WukongAnimations.ScaleTime.of(
                                        1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.reset(1.0F)));
        auto5Events.add(
                AnimationEvent.InTimeEvent.create(
                                1.0F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0.0F, 0.0F, -2.3F),
                                Armatures.BIPED.get().rootJoint,
                                1.5D,
                                0.01F));

        // 轻击 5: 大范围纵向重击, 全程追踪目标位置与转向, 期间免伤 50%并免疫下一次摔落, 结束时复位免伤
        STAFF_AUTO5 =
                builder.nextAccessor(
                        "biped/greatsage/basic/auto_5",
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
                                                (animation, patch, speed, previous, elapsed) ->
                                                        1.2F)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        (patch, animation, args) ->
                                                                patch.playSound(
                                                                        EpicFightSounds.ENTITY_MOVE
                                                                                .get(),
                                                                        1.0F,
                                                                        1.0F),
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationEvent.InPeriodEvent.create(
                                                        0.01F,
                                                        1.9833F,
                                                        (patch, animation, args) -> {
                                                            if (patch
                                                                            instanceof
                                                                            ServerPlayerPatch
                                                                                    playerPatch
                                                                    && WukongWeaponCategories
                                                                            .isWeaponValid(
                                                                                    playerPatch)) {
                                                                var data =
                                                                        playerPatch
                                                                                .getSkill(
                                                                                        SkillSlots
                                                                                                .WEAPON_INNATE)
                                                                                .getDataManager();
                                                                if (data.hasData(
                                                                        WukongSkillDataKeys
                                                                                .DAMAGE_REDUCE
                                                                                .get())) {
                                                                    data.setData(
                                                                            WukongSkillDataKeys
                                                                                    .DAMAGE_REDUCE
                                                                                    .get(),
                                                                            0.5F);
                                                                }
                                                                if (data.hasData(
                                                                        WukongSkillDataKeys
                                                                                .THRUST_PROTECT_NEXT_FALL
                                                                                .get())) {
                                                                    data.setData(
                                                                            WukongSkillDataKeys
                                                                                    .THRUST_PROTECT_NEXT_FALL
                                                                                    .get(),
                                                                            true);
                                                                }
                                                                if (data.hasData(
                                                                        WukongSkillDataKeys
                                                                                .PROTECT_NEXT_FALL
                                                                                .get())) {
                                                                    data.setData(
                                                                            WukongSkillDataKeys
                                                                                    .PROTECT_NEXT_FALL
                                                                                    .get(),
                                                                            true);
                                                                }
                                                            }
                                                        },
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        (patch, animation, args) -> {
                                                            if (patch
                                                                    instanceof
                                                                    ServerPlayerPatch playerPatch) {
                                                                var data =
                                                                        playerPatch
                                                                                .getSkill(
                                                                                        SkillSlots
                                                                                                .WEAPON_INNATE)
                                                                                .getDataManager();
                                                                if (data.hasData(
                                                                        WukongSkillDataKeys
                                                                                .DAMAGE_REDUCE
                                                                                .get())) {
                                                                    data.setData(
                                                                            WukongSkillDataKeys
                                                                                    .DAMAGE_REDUCE
                                                                                    .get(),
                                                                            -1.0F);
                                                                }
                                                            }
                                                        },
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                auto5Events.toArray(
                                                        new AnimationEvent.InTimeEvent[0])));
    }

    // 注册大圣形态的重击风格(劈棍/风云扫/扫戳等)动画
    private static void registerHeavyAttacks(
            AnimationManager.AnimationBuilder builder, HumanoidArmature biped) {
        // 劈棍式事件列表: 0.1s 落地音效与多段缩放, 2.66s 客户端地面碎裂特效
        List<AnimationEvent.InTimeEvent> chopEvents =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.1F,
                                (patch, animation, args) ->
                                        patch.playSound(WuKongSounds.HIT_GROUND.get(), 1.0F, 1.0F),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                WukongAnimations.ScaleTime.of(
                                        0.0F, 1.0F, 2.0F, 1.0F, 0.0F, 1.8F, 0.0F),
                                WukongAnimations.ScaleTime.of(
                                        3.16F, 1.0F, 2.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.reset(4.16F)));
        chopEvents.add(
                AnimationEvent.InTimeEvent.create(
                                2.66F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(new Vec3f(0.0F, -5.0F, -5.0F), biped.rootJoint, 3.0D, 0.01F));

        // 劈棍式: 变长棍下劈重击, 下落阶段(2.3~2.66s)加速, 追踪目标且允许垂直移动
        CHOP_STICK_STYLE =
                builder.nextAccessor(
                        "biped/greatsage/heavy/chop_stick_style",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.1F,
                                                2.333F,
                                                2.66F,
                                                4.16F,
                                                WukongColliders.GREATSAGE_CHOPSTICK,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(5.0F))
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
                                                        .MOVE_ON_LINK,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(0.3F, 3.5F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, previous, elapsed) ->
                                                        elapsed >= 2.3F && elapsed <= 2.66F
                                                                ? 2.5F
                                                                : 1.7F)
                                        .addEvents(
                                                chopEvents.toArray(
                                                        new AnimationEvent.InTimeEvent[0])));

        // 断棍式: 变长棍重击, 开始时标记处于特殊攻击状态, 结束时解除标记
        BROKEN_STICK_STYLE =
                builder.nextAccessor(
                        "biped/greatsage/heavy/broken_stick_style",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.1F,
                                                1.666F,
                                                1.8333F,
                                                2.33F,
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
                                                        .COORD_SET_BEGIN,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .COORD_SET_TICK,
                                                WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_ON_LINK,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, previous, elapsed) ->
                                                        elapsed >= 1.33F && elapsed <= 2.0F
                                                                ? 2.5F
                                                                : 2.0F)
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_BEGIN_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        (patch, animation, args) -> {
                                                            if (patch
                                                                    instanceof
                                                                    ServerPlayerPatch playerPatch) {
                                                                var data =
                                                                        playerPatch
                                                                                .getSkill(
                                                                                        SkillSlots
                                                                                                .WEAPON_INNATE)
                                                                                .getDataManager();
                                                                data.setDataSync(
                                                                        WukongSkillDataKeys
                                                                                .IS_IN_SPECIAL_ATTACK
                                                                                .get(),
                                                                        true);
                                                                data.setDataSync(
                                                                        WukongSkillDataKeys
                                                                                .IS_SPECIAL_SUCCESS
                                                                                .get(),
                                                                        false);
                                                            }
                                                        },
                                                        AnimationEvent.Side.SERVER))
                                        .addEvents(
                                                AnimationProperty.StaticAnimationProperty
                                                        .ON_END_EVENTS,
                                                AnimationEvent.SimpleEvent.create(
                                                        (patch, animation, args) -> {
                                                            if (patch
                                                                    instanceof
                                                                    ServerPlayerPatch playerPatch) {
                                                                playerPatch
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
                                                        },
                                                        AnimationEvent.Side.SERVER)));

        // 扫戳式: 两段多段攻击, 第二段使用大型碰撞体(江河倒转)
        SWEEP_JAB_STYLE =
                builder.nextAccessor(
                        "biped/greatsage/heavy/sweep_jab_style",
                        accessor ->
                                new BasicMultipleAttackAnimation(
                                                0.0F,
                                                typed(accessor),
                                                Armatures.BIPED,
                                                new AttackAnimation.Phase(
                                                                0.0F,
                                                                1.3F,
                                                                1.666F,
                                                                2.7F,
                                                                2.5F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.5F)),
                                                new AttackAnimation.Phase(
                                                                2.5F,
                                                                2.3F,
                                                                2.7F,
                                                                2.7F,
                                                                2.7F,
                                                                biped.toolR,
                                                                WukongColliders
                                                                        .PILLAR_HEAVY_RIVERSEAFLIP)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(1.0F)))
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
                                                        .MOVE_ON_LINK,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, previous, elapsed) -> {
                                                    if (elapsed >= 1.3F && elapsed <= 1.666F
                                                            || elapsed >= 2.3F && elapsed <= 2.7F) {
                                                        return 2.5F;
                                                    }
                                                    return 2.0F;
                                                }));

        // 风云式: 变长棍横扫, 固定 2.5 倍速并追踪目标
        WIND_CLOUD_STYLE =
                builder.nextAccessor(
                        "biped/greatsage/heavy/wind_cloud_style",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.1F,
                                                0.933F,
                                                1.333F,
                                                2.666F,
                                                null,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(1.5F))
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
                                                        .MOVE_ON_LINK,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, previous, elapsed) ->
                                                        2.5F));

        // 轻击 2/3 的蓄力重击: 三段相位, 伤害递增(0.9/1.48/4.48 倍), 中段免重力
        HEAVY_AUTO2_3 =
                builder.nextAccessor(
                        "biped/greatsage/auto2_3",
                        accessor ->
                                new BasicMultipleAttackAnimation(
                                                0.5F,
                                                typed(accessor),
                                                Armatures.BIPED,
                                                new AttackAnimation.Phase(
                                                                0.0F,
                                                                0.966F,
                                                                1.166F,
                                                                6.33F,
                                                                1.2F,
                                                                biped.toolR,
                                                                null)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(0.9F)),
                                                new AttackAnimation.Phase(
                                                                1.2F,
                                                                2.3F,
                                                                2.6666F,
                                                                6.33F,
                                                                2.7F,
                                                                biped.toolR,
                                                                WukongColliders
                                                                        .PILLAR_HEAVY_RIVERSEAFLIP)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(1.48F)),
                                                new AttackAnimation.Phase(
                                                                2.7F,
                                                                4.9F,
                                                                5.0F,
                                                                6.33F,
                                                                6.33F,
                                                                biped.toolR,
                                                                WukongColliders
                                                                        .PILLAR_HEAVY_RIVERSEAFLIP)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(4.48F)))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(3.33F, 4.533F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_ON_LINK,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                true)
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, previous, elapsed) -> {
                                                    if (elapsed > 0.0F && elapsed < 1.166F
                                                            || elapsed > 5.0F) {
                                                        return 3.0F;
                                                    }
                                                    return 2.3F;
                                                }));

        // 蓄力重击 4 的事件列表: 落地音效/多段缩放, 以及 1.3s/2.566s/2.7s/3.833s 的地面碎裂特效
        List<AnimationEvent.InTimeEvent> xuliEvents =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.1F,
                                (patch, animation, args) ->
                                        patch.playSound(WuKongSounds.HIT_GROUND.get(), 1.0F, 1.0F),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                WukongAnimations.ScaleTime.of(
                                        0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.of(
                                        1.3F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.of(
                                        2.0F, 1.0F, 3.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.of(
                                        4.233F, 1.0F, 2.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.reset(4.233F)));
        xuliEvents.add(
                AnimationEvent.InTimeEvent.create(
                                1.3F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(new Vec3f(0.0F, 0.0F, 0.0F), biped.rootJoint, 2.0D, 0.01F));
        xuliEvents.add(
                AnimationEvent.InTimeEvent.create(
                                2.566F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(new Vec3f(0.0F, -5.0F, -5.0F), biped.rootJoint, 2.0D, 0.01F));
        xuliEvents.add(
                AnimationEvent.InTimeEvent.create(
                                2.7F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(new Vec3f(0.0F, -5.0F, -5.0F), biped.rootJoint, 4.0D, 0.01F));
        xuliEvents.add(
                AnimationEvent.InTimeEvent.create(
                                3.833F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(new Vec3f(0.0F, -5.0F, -5.0F), biped.rootJoint, 7.0D, 0.01F));

        // 蓄力重击 4: 三段递进蓄力爆发, 末段 14 倍伤害; 各时间段动态调整播放速度
        XULI_HEAVY_4 =
                builder.nextAccessor(
                        "biped/greatsage/xuli4",
                        accessor ->
                                new BasicMultipleAttackAnimation(
                                                0.1F,
                                                typed(accessor),
                                                Armatures.BIPED,
                                                new AttackAnimation.Phase(
                                                                0.0F,
                                                                2.5F,
                                                                2.6F,
                                                                4.66F,
                                                                2.7F,
                                                                biped.toolR,
                                                                WukongColliders.HENSHIN)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(2.0F)),
                                                new AttackAnimation.Phase(
                                                                2.7F,
                                                                2.733F,
                                                                2.933F,
                                                                4.66F,
                                                                3.6F,
                                                                biped.toolR,
                                                                WukongColliders.HENSHIN)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(2.0F)),
                                                new AttackAnimation.Phase(
                                                                3.6F,
                                                                3.7F,
                                                                3.866F,
                                                                4.66F,
                                                                4.66F,
                                                                biped.toolR,
                                                                WukongColliders.GREATSAGE_XULI4)
                                                        .addProperty(
                                                                AnimationProperty
                                                                        .AttackPhaseProperty
                                                                        .DAMAGE_MODIFIER,
                                                                ValueModifier.multiplier(14.0F)))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_ON_LINK,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(1.33F, 3.833F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, previous, elapsed) -> {
                                                    if (elapsed >= 1.333F && elapsed <= 2.033F) {
                                                        return 1.5F;
                                                    }
                                                    if (elapsed >= 2.166F && elapsed <= 3.166F) {
                                                        return 2.4F;
                                                    }
                                                    if (elapsed >= 3.66F && elapsed <= 3.83F) {
                                                        return 1.6F;
                                                    }
                                                    return 1.3F;
                                                })
                                        .addEvents(
                                                xuliEvents.toArray(
                                                        new AnimationEvent.InTimeEvent[0])));

        // 变身动画事件列表: 落地音效/多段缩放/地面碎裂特效, 并在 4.6s 触发进入大圣形态
        List<AnimationEvent.InTimeEvent> henshinEvents =
                append(
                        AnimationEvent.InTimeEvent.create(
                                0.1F,
                                (patch, animation, args) ->
                                        patch.playSound(WuKongSounds.HIT_GROUND.get(), 1.0F, 1.0F),
                                AnimationEvent.Side.SERVER),
                        getScaleEvents(
                                WukongAnimations.ScaleTime.of(
                                        0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.of(
                                        1.3F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.of(
                                        2.0F, 1.0F, 3.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.of(
                                        2.2F, 1.0F, 3.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.of(
                                        4.233F, 1.0F, 2.0F, 1.0F, 0.0F, 0.0F, 0.0F),
                                WukongAnimations.ScaleTime.reset(4.233F)));
        henshinEvents.add(
                AnimationEvent.InTimeEvent.create(
                                1.3F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(new Vec3f(0.0F, 0.0F, 0.0F), biped.rootJoint, 2.0D, 0.01F));
        henshinEvents.add(
                AnimationEvent.InTimeEvent.create(
                                4.6F,
                                yesman.epicfight.gameasset.Animations.ReusableSources
                                        .FRACTURE_GROUND_SIMPLE,
                                AnimationEvent.Side.CLIENT)
                        .params(
                                new Vec3f(0.0F, 0.0F, 0.0F),
                                Armatures.BIPED.get().rootJoint,
                                4.0D,
                                0.01F));
        henshinEvents.add(
                AnimationEvent.InTimeEvent.create(
                        4.6F,
                        (patch, animation, args) -> BattleUnit.greatSageMode(patch),
                        AnimationEvent.Side.SERVER));

        // 变身动画: 变长棍演出, 4.6s 处通过 BattleUnit.greatSageMode 切换为大圣形态
        HENSHIN =
                builder.nextAccessor(
                        "biped/greatsage/henshin",
                        accessor ->
                                new WukongScaleStaffAttackAnimation(
                                                0.1F,
                                                0.0F,
                                                0.0F,
                                                7.0F,
                                                WukongColliders.HENSHIN,
                                                biped.toolR,
                                                typed(accessor),
                                                Armatures.BIPED)
                                        .addProperty(
                                                AnimationProperty.AttackPhaseProperty
                                                        .DAMAGE_MODIFIER,
                                                ValueModifier.multiplier(0.9F))
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_ON_LINK,
                                                false)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .STOP_MOVEMENT,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .MOVE_VERTICAL,
                                                true)
                                        .addProperty(
                                                AnimationProperty.ActionAnimationProperty
                                                        .NO_GRAVITY_TIME,
                                                TimePairList.create(1.33F, 3.833F))
                                        .addProperty(
                                                AnimationProperty.StaticAnimationProperty
                                                        .PLAY_SPEED_MODIFIER,
                                                (animation, patch, speed, previous, elapsed) -> {
                                                    if (elapsed >= 1.333F && elapsed <= 2.033F) {
                                                        return 1.5F;
                                                    }
                                                    if (elapsed >= 2.166F && elapsed <= 3.166F) {
                                                        return 2.4F;
                                                    }
                                                    if (elapsed >= 3.66F && elapsed <= 3.83F) {
                                                        return 1.6F;
                                                    }
                                                    return 1.3F;
                                                })
                                        .addEvents(
                                                henshinEvents.toArray(
                                                        new AnimationEvent.InTimeEvent[0])));
    }
}
