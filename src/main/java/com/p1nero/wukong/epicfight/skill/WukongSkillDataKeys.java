package com.p1nero.wukong.epicfight.skill;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.epicfight.skill.custom.fashu.*;
import com.p1nero.wukong.epicfight.skill.custom.wukong.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import yesman.epicfight.api.utils.PacketBufferCodec;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.SkillDataKey;

public class WukongSkillDataKeys {

    public static final DeferredRegister<SkillDataKey<?>> DATA_KEYS =
            DeferredRegister.create(
                    ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "skill_data_keys"),
                    WukongMoveset.MOD_ID);

    // 棍式
    public static final RegistryObject<SkillDataKey<Integer>> STANCE =
            DATA_KEYS.register(
                    "stance",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER, 0, false, SmashHeavyAttack.class));
    public static final RegistryObject<SkillDataKey<Boolean>> IS_ATTACK_KEY_DOWN =
            DATA_KEYS.register(
                    "is_attack_key_down",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class));
    public static final RegistryObject<SkillDataKey<Boolean>> IS_REPEATING_DERIVE =
            DATA_KEYS.register(
                    "is_repeating_derive",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class));
    public static final RegistryObject<SkillDataKey<Integer>> REPEATING_DERIVE_TIMER =
            DATA_KEYS.register(
                    "repeating_derive_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    ThrustHeavyAttack.class)); // 四段棍势持续时间

    // 重击
    public static final RegistryObject<SkillDataKey<Boolean>> KEY_PRESSING =
            DATA_KEYS.register(
                    "key_pressing",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    ThrustHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 技能键是否按下
    public static final RegistryObject<SkillDataKey<Integer>> CHARGED4_TIMER =
            DATA_KEYS.register(
                    "charged4_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    ThrustHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 四段棍势持续时间
    public static final RegistryObject<SkillDataKey<Integer>> RED_TIMER =
            DATA_KEYS.register(
                    "red_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    ThrustHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 亮灯时间
    public static final RegistryObject<SkillDataKey<Integer>> LAST_STACK =
            DATA_KEYS.register(
                    "last_stack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    ThrustHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 上一次的层数，用于判断是否加层
    public static final RegistryObject<SkillDataKey<Integer>> STARS_CONSUMED =
            DATA_KEYS.register(
                    "stars_consumed",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    ThrustHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 本次攻击是否消耗星（是否强化）
    public static final RegistryObject<SkillDataKey<Boolean>> IS_IN_SPECIAL_ATTACK =
            DATA_KEYS.register(
                    "is_in_special_attack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    ThrustHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 是否正在切手技（用来判断无敌时间）
    public static final RegistryObject<SkillDataKey<Boolean>> IS_SPECIAL_SUCCESS =
            DATA_KEYS.register(
                    "is_special_success",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    ThrustHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 是否正在切手技（用来判断无敌时间）
    public static final RegistryObject<SkillDataKey<Boolean>> IS_CHARGING =
            DATA_KEYS.register(
                    "is_charging",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    ThrustHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 是否正在蓄力
    public static final RegistryObject<SkillDataKey<Integer>> DERIVE_TIMER =
            DATA_KEYS.register(
                    "derive_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class)); // 衍生合法时间计时器
    public static final RegistryObject<SkillDataKey<Boolean>> CAN_FIRST_DERIVE =
            DATA_KEYS.register(
                    "can_first_derive",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class)); // 是否可以使用第一段衍生
    public static final RegistryObject<SkillDataKey<Boolean>> CAN_SECOND_DERIVE =
            DATA_KEYS.register(
                    "can_second_derive",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 是否可以使用第二段衍生
    public static final RegistryObject<SkillDataKey<Integer>> CAN_FIRST_TIMER =
            DATA_KEYS.register(
                    "can_first_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 是否可以使用第一段衍生时间
    public static final RegistryObject<SkillDataKey<Integer>> CAN_SECOND_TIMER =
            DATA_KEYS.register(
                    "can_second_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    GreatSageHeavyAttack.class,
                                    ThrustHeavyAttack.class)); // 是否可以使用第二段衍生时间
    public static final RegistryObject<SkillDataKey<Boolean>> CAN_JUMP_HEAVY =
            DATA_KEYS.register(
                    "can_jump_heavy",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class)); // 是否可以使用跳跃重击
    public static final RegistryObject<SkillDataKey<Boolean>> PLAY_SOUND =
            DATA_KEYS.register(
                    "play_sound",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    true,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 是否播放棍势消耗音效
    public static final RegistryObject<SkillDataKey<Boolean>> PROTECT_NEXT_FALL =
            DATA_KEYS.register(
                    "protect_next_fall",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 防止坠机
    public static final RegistryObject<SkillDataKey<Float>> DAMAGE_REDUCE =
            DATA_KEYS.register(
                    "damage_reduce",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.FLOAT,
                                    -1.0F,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    GreatSageHeavyAttack.class)); // 防止坠机
    public static final RegistryObject<SkillDataKey<Boolean>> ADD_BEANS =
            DATA_KEYS.register(
                    "add_beans",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    SmashHeavyAttack.class,
                                    PillarHeavyAttack.class,
                                    ThrustHeavyAttack.class,
                                    GreatSageHeavyAttack.class));
    // 闪避
    public static final RegistryObject<SkillDataKey<Integer>> COUNT =
            DATA_KEYS.register(
                    "count",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    WukongDodgeSkill.class)); // 闪避计数器
    public static final RegistryObject<SkillDataKey<Integer>> DIRECTION =
            DATA_KEYS.register(
                    "direction",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    WukongDodgeSkill.class)); // 方向，用于播放完美闪避
    public static final RegistryObject<SkillDataKey<Integer>> RESET_TIMER =
            DATA_KEYS.register(
                    "reset_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    WukongDodgeSkill.class)); // 回归第一段的时间
    public static final RegistryObject<SkillDataKey<Boolean>> DODGE_PLAYED =
            DATA_KEYS.register(
                    "dodge_played",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    WukongDodgeSkill.class)); // 是否播过完美闪避，防止重复播放
    // 棍花
    public static final RegistryObject<SkillDataKey<Boolean>> PLAYING_STAFF_SPIN =
            DATA_KEYS.register(
                    "playing_staff_spin",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN, false, false, StaffPassive.class));

    // 聚形散气
    public static final RegistryObject<SkillDataKey<Boolean>> MAGICARTS_CFDA =
            DATA_KEYS.register(
                    "magicarts_cfda",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN, false, false, StaffPassive.class));

    public static final RegistryObject<SkillDataKey<Boolean>> PILLAR_FENG_YU_ZHUAN =
            DATA_KEYS.register(
                    "pillar_feng_yu_zhuan",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    PillarHeavyAttack.class)); // 风云转
    public static final RegistryObject<SkillDataKey<Boolean>> PILLAR_JIANGHAIFAN =
            DATA_KEYS.register(
                    "pillar_jianghaifan",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    PillarHeavyAttack.class)); // 江海翻
    public static final RegistryObject<SkillDataKey<Integer>> PILLAR_JIANGHAIFAN_TIMER =
            DATA_KEYS.register(
                    "pillar_jianghaifan_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    PillarHeavyAttack.class)); // 江海翻时间计时器
    public static final RegistryObject<SkillDataKey<Integer>> PILLAR_FASHU_TIMER =
            DATA_KEYS.register(
                    "pillar_fashu_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    PillarHeavyAttack.class)); // 派生重击时间
    public static final RegistryObject<SkillDataKey<Boolean>> PILLAR_FASHU_STACK =
            DATA_KEYS.register(
                    "pillar_fashu_stack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    PillarHeavyAttack.class));

    // 戳棍 Thrust
    public static final RegistryObject<SkillDataKey<Boolean>> Thrust_KEY_PRESSING =
            DATA_KEYS.register(
                    "thrust_key_pressing",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class)); // 技能键是否按下
    public static final RegistryObject<SkillDataKey<Integer>> Thrust_CHARGED4_TIMER =
            DATA_KEYS.register(
                    "thrust_charged4_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ThrustHeavyAttack.class)); // 四段棍势持续时间
    public static final RegistryObject<SkillDataKey<Integer>> RECENT_DODGE_TIMER =
            DATA_KEYS.register(
                    "recent_dodge_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ThrustHeavyAttack.class)); // 闪避后的切手技判定窗口, 供戳棍4段直放凤穿花判定
    public static final RegistryObject<SkillDataKey<Integer>> Thrust_LAST_STACK =
            DATA_KEYS.register(
                    "thrust_last_stack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ThrustHeavyAttack.class)); // 上一次的层数，用于判断是否加层

    public static final RegistryObject<SkillDataKey<Boolean>> Thrust_IS_CHARGING =
            DATA_KEYS.register(
                    "thrust_is_charging",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class)); // 是否正在蓄力
    public static final RegistryObject<SkillDataKey<Integer>> Thrust_DERIVE_TIMER =
            DATA_KEYS.register(
                    "thrust_derive_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ThrustHeavyAttack.class)); // 第一段衍生合法时间计时器
    public static final RegistryObject<SkillDataKey<Integer>> Thrust_DERIVE_TIMER_TWO =
            DATA_KEYS.register(
                    "thrust_derive_timer_two",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ThrustHeavyAttack.class)); // 第二段衍生合法时间计时器
    public static final RegistryObject<SkillDataKey<Boolean>> Thrust_STEOP_BACK =
            DATA_KEYS.register(
                    "thrust_step_back",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class)); // 退寸状态用于检测免伤害
    public static final RegistryObject<SkillDataKey<Boolean>> THRUST_SECOND_BACK =
            DATA_KEYS.register(
                    "thrust_second_back",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class)); // 派生第二状态

    public static final RegistryObject<SkillDataKey<Boolean>> Thrust_CAN_FIRST_DERIVE =
            DATA_KEYS.register(
                    "thrust_can_first_derive",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class)); // 是否可以使用第一段衍生
    public static final RegistryObject<SkillDataKey<Boolean>> Thrust_CAN_SECOND_DERIVE =
            DATA_KEYS.register(
                    "thrust_can_second_derive",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class)); // 是否可以使用第二段衍生
    public static final RegistryObject<SkillDataKey<Boolean>> Thrust_PLAY_SOUND =
            DATA_KEYS.register(
                    "thrust_play_sound",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    true,
                                    false,
                                    ThrustHeavyAttack.class)); // 是否播放棍势消耗音效
    public static final RegistryObject<SkillDataKey<Boolean>> Thrust_JUESICK_BACK =
            DATA_KEYS.register(
                    "thrust_juesick_back",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class)); // 觉棍
    public static final RegistryObject<SkillDataKey<Boolean>> THRUST_METERS_BACK =
            DATA_KEYS.register(
                    "thrust_meters_back",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class)); // 进尺
    public static final RegistryObject<SkillDataKey<Boolean>> THRUST_PROTECT_NEXT_FALL =
            DATA_KEYS.register(
                    "thrust_protect_next_fall",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class)); // 防止坠机
    public static final RegistryObject<SkillDataKey<Integer>> THRUST_FASHU_TIMER =
            DATA_KEYS.register(
                    "thrust_fashu_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ThrustHeavyAttack.class)); // 派生重击时间
    public static final RegistryObject<SkillDataKey<Boolean>> THRUST_FASHU_STACK =
            DATA_KEYS.register(
                    "thrust_fashu_stack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ThrustHeavyAttack.class));
    public static final RegistryObject<SkillDataKey<Integer>> Thrust_RETREAT_TIMER =
            DATA_KEYS.register(
                    "thrust_retreat_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ThrustHeavyAttack.class)); // 退寸时间合法时间计时器

    public static final RegistryObject<SkillDataKey<Integer>> TTTB_INVINCIBLE_TIMER =
            DATA_KEYS.register(
                    "tttb_invincible_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ShenfaTongtoutiebiSkill.class)); // 铜头铁臂无敌帧
    public static final RegistryObject<SkillDataKey<Integer>> TTTB_RESTORE_TIMER =
            DATA_KEYS.register(
                    "tttb_restore_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ShenfaTongtoutiebiSkill.class)); // 铜头铁臂效果时间时间
    public static final RegistryObject<SkillDataKey<Boolean>> TTTB_RESTORE_ZT =
            DATA_KEYS.register(
                    "tttb_restore_zt",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    ShenfaTongtoutiebiSkill.class)); //
    public static final RegistryObject<SkillDataKey<Integer>> TTTB_COOLING_TIMER =
            DATA_KEYS.register(
                    "tttb_cooling_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ShenfaTongtoutiebiSkill.class)); // 冷却时间
    public static final RegistryObject<SkillDataKey<Boolean>> TTTB_COOLING_ATTACK =
            DATA_KEYS.register(
                    "tttb_cooling_attack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    true,
                                    false,
                                    ShenfaTongtoutiebiSkill.class));

    public static final RegistryObject<SkillDataKey<Float>> TTTB_DAMAGE_REDUCE =
            DATA_KEYS.register(
                    "tttb_damage_reduce",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.FLOAT,
                                    -1.0F,
                                    false,
                                    ShenfaTongtoutiebiSkill.class)); // 防止坠机

    public static final RegistryObject<SkillDataKey<Integer>> JXSQ_YINGSHEN_TIMER =
            DATA_KEYS.register(
                    "yingshen_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ShenfaJuxingsanqiSkill.class)); // 聚形散气隐身时间
    public static final RegistryObject<SkillDataKey<Boolean>> JXSQ_YINGSHEN_ZT =
            DATA_KEYS.register(
                    "jxsq_yingshen_zt",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    true,
                                    false,
                                    ShenfaJuxingsanqiSkill.class)); //
    public static final RegistryObject<SkillDataKey<Integer>> JXSQ_COOLING_TIMER =
            DATA_KEYS.register(
                    "jxsq_cooling_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ShenfaJuxingsanqiSkill.class)); // 冷却时间
    public static final RegistryObject<SkillDataKey<Boolean>> JXSQ_COOLING_ATTACK =
            DATA_KEYS.register(
                    "jxsq_cooling_attack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    true,
                                    false,
                                    ShenfaJuxingsanqiSkill.class));

    public static final RegistryObject<SkillDataKey<Boolean>> ASF_YINGSHEN_ZT =
            DATA_KEYS.register(
                    "asf_yingshen_zt",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    true,
                                    false,
                                    FashuAnshenfaSkill.class));
    public static final RegistryObject<SkillDataKey<Integer>> ASF_DERIVE_TIMER =
            DATA_KEYS.register(
                    "asf_derive_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER, 0, false, FashuAnshenfaSkill.class));
    public static final RegistryObject<SkillDataKey<Integer>> ASF_COOLING_TIMER =
            DATA_KEYS.register(
                    "asf_cooling_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    FashuAnshenfaSkill.class)); // 冷却时间
    public static final RegistryObject<SkillDataKey<Boolean>> ASF_COOLING_ATTACK =
            DATA_KEYS.register(
                    "asf_cooling_attack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    true,
                                    false,
                                    FashuAnshenfaSkill.class));

    public static final RegistryObject<SkillDataKey<Boolean>> DSF_YINGSHEN_ZT =
            DATA_KEYS.register(
                    "dsf_yingshen_zt",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    FashuDingshenfaSkill.class));
    public static final RegistryObject<SkillDataKey<Integer>> DSF_DERIVE_TIMER =
            DATA_KEYS.register(
                    "dsf_derive_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    FashuDingshenfaSkill.class));
    public static final RegistryObject<SkillDataKey<Integer>> DSF_COOLING_TIMER =
            DATA_KEYS.register(
                    "dsf_cooling_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    FashuDingshenfaSkill.class)); // 冷却时间
    public static final RegistryObject<SkillDataKey<Boolean>> DSF_COOLING_ATTACK =
            DATA_KEYS.register(
                    "dsf_cooling_attack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    true,
                                    false,
                                    FashuDingshenfaSkill.class));
    public static final RegistryObject<SkillDataKey<Boolean>> DSF_ENEMY_ATTACK =
            DATA_KEYS.register(
                    "dsf_enemy_attack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    true,
                                    false,
                                    FashuDingshenfaSkill.class));

    public static final RegistryObject<SkillDataKey<Boolean>> SWSF_COOLING_ATTACK =
            DATA_KEYS.register(
                    "swsf_cooling_attack",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    true,
                                    false,
                                    ShenWaiShenFaSkill.class));
    public static final RegistryObject<SkillDataKey<Integer>> SWSF_COOLING_TIMER =
            DATA_KEYS.register(
                    "swsf_cooling_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    ShenWaiShenFaSkill.class)); // 冷却时间

    public static final RegistryObject<SkillDataKey<Integer>> GREATSAGE_ONE_TIMER =
            DATA_KEYS.register(
                    "greatsage_one_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    GreatSageHeavyAttack.class)); // 衍生合法时间计时器
    public static final RegistryObject<SkillDataKey<Integer>> GREATSAGE_TWO_TIMER =
            DATA_KEYS.register(
                    "greatsage_two_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    GreatSageHeavyAttack.class)); // 衍生合法时间计时器
    public static final RegistryObject<SkillDataKey<Integer>> GREATSAGE_THREE_TIMER =
            DATA_KEYS.register(
                    "greatsage_three_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    GreatSageHeavyAttack.class)); // 衍生合法时间计时器
    public static final RegistryObject<SkillDataKey<Integer>> GREATSAGE_FOUR_TIMER =
            DATA_KEYS.register(
                    "greatsage_four_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    GreatSageHeavyAttack.class)); // 衍生合法时间计时器
    public static final RegistryObject<SkillDataKey<Integer>> GREATSAGE_CHARGED4_TIMER =
            DATA_KEYS.register(
                    "greatsage_charged4_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    GreatSageHeavyAttack.class)); // 四段棍势持续时间
    public static final RegistryObject<SkillDataKey<Integer>> GREATSAGE_RED_TIMER =
            DATA_KEYS.register(
                    "greatsage_red_timer",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    GreatSageHeavyAttack.class)); // 亮灯时间
    public static final RegistryObject<SkillDataKey<Integer>> GREATSAGE_STARS_CONSUMED =
            DATA_KEYS.register(
                    "greatsage_stars_consumed",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    GreatSageHeavyAttack.class)); // 本次攻击是否消耗星（是否强化）
    public static final RegistryObject<SkillDataKey<Integer>> GREATSAGE_NUMBER =
            DATA_KEYS.register(
                    "greatsage_number",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.INTEGER,
                                    0,
                                    false,
                                    GreatSageHeavyAttack.class)); // 连招计数
    public static final RegistryObject<SkillDataKey<Boolean>> GREATSAGE_PILLAR =
            DATA_KEYS.register(
                    "greatsage_pillar",
                    () ->
                            SkillDataKey.createSkillDataKey(
                                    PacketBufferCodec.BOOLEAN,
                                    false,
                                    false,
                                    GreatSageHeavyAttack.class));
}
