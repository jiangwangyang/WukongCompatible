package com.p1nero.wukong.client;


import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillSlots;

import java.util.Arrays;
import java.util.List;

public record AnimationJudge() {
    static final List<AnimationManager.AnimationAccessor> ZERO_STOR;
    static final List<AnimationManager.AnimationAccessor> ONE_STOR;
    static final List<AnimationManager.AnimationAccessor> TWO_STOR;
    static final List<AnimationManager.AnimationAccessor> THREE_STOR;
    static final List<AnimationManager.AnimationAccessor> FOUR_STOR;
    static final List<AnimationManager.AnimationAccessor> Glow;
    static final List<AnimationManager.AnimationAccessor> GH;


    static {
        Glow = Arrays.asList(
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
                WukongAnimations. PILLAR_HEAVY0,
                WukongAnimations. PILLAR_HEAVY1,
                WukongAnimations. PILLAR_HEAVY2,
                WukongAnimations. PILLAR_HEAVY3,
                WukongAnimations. PILLAR_HEAVY4
        );
        ZERO_STOR = Arrays.asList(
                WukongAnimations.SMASH_CHARGED0,
                WukongAnimations.THRUST_CHARGED0,
                WukongAnimations. PILLAR_HEAVY0
        );
        ONE_STOR = Arrays.asList(
                WukongAnimations.SMASH_CHARGED1,
                WukongAnimations.THRUST_CHARGED1,
                WukongAnimations. PILLAR_HEAVY1
        );
        TWO_STOR = Arrays.asList(
                WukongAnimations.SMASH_CHARGED2,
                WukongAnimations.THRUST_CHARGED2,
                WukongAnimations. PILLAR_HEAVY2
        );
        THREE_STOR = Arrays.asList(
                WukongAnimations.SMASH_CHARGED3,
                WukongAnimations.THRUST_CHARGED3,
                WukongAnimations. PILLAR_HEAVY3
        );
        FOUR_STOR = Arrays.asList(
                WukongAnimations.SMASH_CHARGED4,
                WukongAnimations.THRUST_JUESICK_FENGCHUANHUA,
                WukongAnimations. PILLAR_HEAVY4
        );


        GH = Arrays.asList(
                WukongAnimations.STAFF_SPIN_ONE_HAND_LOOP,
                WukongAnimations.STAFF_SPIN_TWO_HAND_LOOP
        );


    }
    public static boolean isGh(StaticAnimation staticAnimation) {
        return contains(GH, staticAnimation);
    }
    public static boolean isGlow(StaticAnimation staticAnimation) {
       // WukongMoveset.LOGGER.error("isGlow:"+staticAnimation);
      return contains(Glow, staticAnimation);
    }
    public static boolean isQie(StaticAnimation staticAnimation) {
        return contains(ONE_STOR, staticAnimation);
    }
    public static boolean isTwo(StaticAnimation staticAnimation) {
        return contains(TWO_STOR, staticAnimation);
    }
    public static boolean isThree(StaticAnimation staticAnimation) {
        return contains(THREE_STOR, staticAnimation);
    }
    public static boolean isFour(StaticAnimation staticAnimation) {
        return contains(FOUR_STOR, staticAnimation);
    }

    private static boolean contains(List<AnimationManager.AnimationAccessor> animations, StaticAnimation staticAnimation) {
        return staticAnimation != null && animations.stream().anyMatch(accessor -> accessor.get() == staticAnimation);
    }

    public static boolean isCharging(LocalPlayerPatch lpp) {
        return lpp.getSkill(SkillSlots.WEAPON_INNATE) != null && (
                (lpp.getSkill(SkillSlots.WEAPON_INNATE).getDataManager().hasData(WukongSkillDataKeys.IS_CHARGING.get()) && lpp.getSkill(SkillSlots.WEAPON_INNATE).getDataManager().getDataValue(WukongSkillDataKeys.IS_CHARGING.get()))
                        || (lpp.getSkill(SkillSlots.WEAPON_INNATE).getDataManager().hasData(WukongSkillDataKeys.IS_CHARGING.get()) && (lpp.getSkill(SkillSlots.WEAPON_INNATE).getDataManager().getDataValue(WukongSkillDataKeys.IS_CHARGING.get())))
                        || (lpp.getSkill(SkillSlots.WEAPON_INNATE).getDataManager().hasData(WukongSkillDataKeys.Thrust_IS_CHARGING.get()) && (lpp.getSkill(SkillSlots.WEAPON_INNATE).getDataManager().getDataValue(WukongSkillDataKeys.Thrust_IS_CHARGING.get()))));
    }


}
