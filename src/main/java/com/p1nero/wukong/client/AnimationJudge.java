package com.p1nero.wukong.client;

import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.SkillSlots;

import java.util.Arrays;
import java.util.List;

// 动画判定工具: 按蓄力星级/发光特效/棍花等类别判定动画归属
public record AnimationJudge() {
    static final List<AnimationManager.AnimationAccessor> ONE_STOR;
    static final List<AnimationManager.AnimationAccessor> TWO_STOR;
    static final List<AnimationManager.AnimationAccessor> THREE_STOR;
    static final List<AnimationManager.AnimationAccessor> FOUR_STOR;
    static final List<AnimationManager.AnimationAccessor> GLOW;
    static final List<AnimationManager.AnimationAccessor> GH;

    static {
        GLOW =
                Arrays.asList(
                        WukongAnimations.SMASH_CHARGED0,
                        WukongAnimations.SMASH_CHARGED1,
                        WukongAnimations.SMASH_CHARGED2,
                        WukongAnimations.SMASH_CHARGED3,
                        WukongAnimations.SMASH_CHARGED4,
                        WukongAnimations.THRUST_CHARGED0,
                        WukongAnimations.THRUST_CHARGED1,
                        WukongAnimations.THRUST_CHARGED2,
                        WukongAnimations.THRUST_CHARGED3,
                        WukongAnimations.THRUST_JUESICK_FENGCHUANHUA,
                        WukongAnimations.PILLAR_HEAVY0,
                        WukongAnimations.PILLAR_HEAVY1,
                        WukongAnimations.PILLAR_HEAVY2,
                        WukongAnimations.PILLAR_HEAVY3,
                        WukongAnimations.PILLAR_HEAVY4);
        ONE_STOR =
                Arrays.asList(
                        WukongAnimations.SMASH_CHARGED1,
                        WukongAnimations.THRUST_CHARGED1,
                        WukongAnimations.PILLAR_HEAVY1);
        TWO_STOR =
                Arrays.asList(
                        WukongAnimations.SMASH_CHARGED2,
                        WukongAnimations.THRUST_CHARGED2,
                        WukongAnimations.PILLAR_HEAVY2);
        THREE_STOR =
                Arrays.asList(
                        WukongAnimations.SMASH_CHARGED3,
                        WukongAnimations.THRUST_CHARGED3,
                        WukongAnimations.PILLAR_HEAVY3);
        FOUR_STOR =
                Arrays.asList(
                        WukongAnimations.SMASH_CHARGED4,
                        WukongAnimations.THRUST_JUESICK_FENGCHUANHUA,
                        WukongAnimations.PILLAR_HEAVY4);

        GH =
                Arrays.asList(
                        WukongAnimations.STAFF_SPIN_ONE_HAND_LOOP,
                        WukongAnimations.STAFF_SPIN_TWO_HAND_LOOP);
    }

    // 判断动画是否为棍花循环动画
    public static boolean isGh(AssetAccessor<? extends DynamicAnimation> animation) {
        return contains(GH, animation);
    }

    // 判断动画是否为带发光特效的蓄力动画
    public static boolean isGlow(AssetAccessor<? extends DynamicAnimation> animation) {
        return contains(GLOW, animation);
    }

    // 判断动画是否为一层蓄力动画
    public static boolean isQie(AssetAccessor<? extends DynamicAnimation> animation) {
        return contains(ONE_STOR, animation);
    }

    // 判断动画是否为二层蓄力动画
    public static boolean isTwo(AssetAccessor<? extends DynamicAnimation> animation) {
        return contains(TWO_STOR, animation);
    }

    // 判断动画是否为三层蓄力动画
    public static boolean isThree(AssetAccessor<? extends DynamicAnimation> animation) {
        return contains(THREE_STOR, animation);
    }

    // 判断动画是否为四层(满层)蓄力动画
    public static boolean isFour(AssetAccessor<? extends DynamicAnimation> animation) {
        return contains(FOUR_STOR, animation);
    }

    // 判定指定动画是否存在于给定列表中(访问器按注册名比较, 动画播放器保存的是访问器而非动画实例)
    private static boolean contains(
            List<AnimationManager.AnimationAccessor> animations,
            AssetAccessor<? extends DynamicAnimation> animation) {
        return animation != null && animations.contains(animation);
    }

    // 判断玩家当前是否处于蓄力状态(劈棍/戳棍)
    public static boolean isCharging(LocalPlayerPatch lpp) {
        SkillContainer innate = lpp.getSkill(SkillSlots.WEAPON_INNATE);
        if (innate == null) {
            return false;
        }
        SkillDataManager dataManager = innate.getDataManager();
        return (dataManager.hasData(WukongSkillDataKeys.IS_CHARGING.get())
                        && dataManager.getDataValue(WukongSkillDataKeys.IS_CHARGING.get()))
                || (dataManager.hasData(WukongSkillDataKeys.Thrust_IS_CHARGING.get())
                        && dataManager.getDataValue(WukongSkillDataKeys.Thrust_IS_CHARGING.get()));
    }
}
