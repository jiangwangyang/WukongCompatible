package com.p1nero.wukong.epicfight.skill;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.epicfight.WukongStyles;
import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.animation.WukongGreatSageAnimations;
import com.p1nero.wukong.epicfight.skill.custom.fashu.FashuAnshenfaSkill;
import com.p1nero.wukong.epicfight.skill.custom.fashu.FashuDingshenfaSkill;
import com.p1nero.wukong.epicfight.skill.custom.fashu.ShenWaiShenFaSkill;
import com.p1nero.wukong.epicfight.skill.custom.fashu.ShenfaJuxingsanqiSkill;
import com.p1nero.wukong.epicfight.skill.custom.fashu.ShenfaTongtoutiebiSkill;
import com.p1nero.wukong.epicfight.skill.custom.wukong.GreatSageHeavyAttack;
import com.p1nero.wukong.epicfight.skill.custom.wukong.PillarHeavyAttack;
import com.p1nero.wukong.epicfight.skill.custom.wukong.SmashHeavyAttack;
import com.p1nero.wukong.epicfight.skill.custom.wukong.StaffPassive;
import com.p1nero.wukong.epicfight.skill.custom.wukong.StaffStance;
import com.p1nero.wukong.epicfight.skill.custom.wukong.ThrustHeavyAttack;
import com.p1nero.wukong.epicfight.skill.custom.wukong.WukongDodgeSkill;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import java.util.concurrent.atomic.AtomicInteger;

// 悟空技能注册类: 在MOD事件总线上注册全部技能(棍式/重击/闪避/法术), 并提供可选项列表
@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class WukongSkills {
    public static Skill SMASH_STYLE; // 劈棍式技能实例
    public static Skill THRUST_STYLE; // 戳棍式技能实例
    public static Skill PILLAR_STYLE; // 立棍式技能实例
    public static Skill GREATSAGE_STYLE; // 大圣式技能实例
    public static Skill SMASH_HEAVY_ATTACK; // 劈棍重击技能实例
    public static Skill THRUST_HEAVY_ATTACK; // 戳棍重击技能实例
    public static Skill PILLAR_HEAVY_ATTACK; // 立棍重击技能实例
    public static Skill GREATSAGE_HEAVY_ATTACK; // 大圣重击技能实例

    public static Skill STAFF_SPIN; // 棍花被动技能实例
    public static Skill WUKONG_DODGE; // 悟空闪避技能实例
    public static Skill SPELL_JUXINGSANQI; // 聚形散气
    public static Skill SPELL_TONGTOUTIEBI; // 铜头铁臂
    public static Skill MAGI_ANSHENFA; // 安身术
    public static Skill MAGI_DINGSHENFA; // 定身术
    public static Skill SHEN_WAI_SHEN_FA; // 身外身法

    // 返回可在技能栏选择的技能列表(棍式/聚形散气/铜头铁臂/安身/定身/身外身法)
    public static Skill[] getSelectableSkills() {
        return new Skill[] {
            SMASH_STYLE,
            THRUST_STYLE,
            PILLAR_STYLE,
            GREATSAGE_STYLE,
            SPELL_JUXINGSANQI,
            SPELL_TONGTOUTIEBI,
            MAGI_ANSHENFA,
            MAGI_DINGSHENFA,
            SHEN_WAI_SHEN_FA
        };
    }

    // 增加棍势: 跨段自动升星并保留多余棍势给下一段(阈值为上限的30%/50%/70%), 4星攒满上限封顶, 超出部分舍弃
    public static void gainResource(SkillContainer container, float amount) {
        float resource = container.getResource() + amount;
        while (container.getStack() < 3) {
            float threshold =
                    (float)
                            (container.getMaxResource()
                                    * (container.getStack() == 0
                                            ? 0.3
                                            : container.getStack() == 1 ? 0.5 : 0.7));
            if (resource <= threshold) {
                break;
            }
            resource -= threshold;
            container.getSkill().setStackSynchronize(container, container.getStack() + 1);
        }
        container.getSkill().setConsumptionSynchronize(container, resource);
    }

    // 获取玩家当前武器天赋(棍式)的星级层数
    public static int getCurrentStack(Player player) {
        AtomicInteger stack = new AtomicInteger(0);
        player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY)
                .ifPresent(
                        entityPatch -> {
                            if (entityPatch instanceof PlayerPatch<?> patch) {
                                stack.set(patch.getSkill(SkillSlots.WEAPON_INNATE).getStack());
                            }
                        });
        return stack.get();
    }

    @SubscribeEvent
    // 技能构建事件处理: 注册并装配所有悟空技能及其对应动画
    public static void BuildSkills(SkillBuildEvent event) {
        SkillBuildEvent.ModRegistryWorker registryWorker =
                event.createRegistryWorker(WukongMoveset.MOD_ID);

        WUKONG_DODGE =
                registryWorker.build(
                        "dodge",
                        WukongDodgeSkill::new,
                        WukongDodgeSkill.createDodgeBuilder()
                                .setAnimations1(
                                        () -> WukongAnimations.DODGE_F1,
                                        () -> WukongAnimations.DODGE_B1,
                                        () -> WukongAnimations.DODGE_L1,
                                        () -> WukongAnimations.DODGE_R1)
                                .setAnimations2(
                                        () -> WukongAnimations.DODGE_F2,
                                        () -> WukongAnimations.DODGE_B2,
                                        () -> WukongAnimations.DODGE_L2,
                                        () -> WukongAnimations.DODGE_R2)
                                .setAnimations3(
                                        () -> WukongAnimations.DODGE_F3,
                                        () -> WukongAnimations.DODGE_B3,
                                        () -> WukongAnimations.DODGE_L3,
                                        () -> WukongAnimations.DODGE_R3)
                                .setPerfectAnimations(
                                        () -> WukongAnimations.DODGE_FP,
                                        () -> WukongAnimations.DODGE_BP,
                                        () -> WukongAnimations.DODGE_LP,
                                        () -> WukongAnimations.DODGE_RP));
        STAFF_SPIN =
                registryWorker.build(
                        "staff_spin",
                        StaffPassive::new,
                        Skill.createBuilder()
                                .setResource(Skill.Resource.NONE)
                                .setCategory(SkillCategories.WEAPON_PASSIVE));
        SMASH_HEAVY_ATTACK =
                registryWorker.build(
                        "smash_heavy_attack",
                        SmashHeavyAttack::new,
                        SmashHeavyAttack.createChargedAttack()
                                .setChargePreAnimation(() -> WukongAnimations.SMASH_CHARGING_PRE)
                                .setHeavyAttacks(
                                        () -> WukongAnimations.SMASH_CHARGED0,
                                        () -> WukongAnimations.SMASH_CHARGED1,
                                        () -> WukongAnimations.SMASH_CHARGED2,
                                        () -> WukongAnimations.SMASH_CHARGED3,
                                        () -> WukongAnimations.SMASH_CHARGED4)
                                .setDeriveAnimations(
                                        () -> WukongAnimations.SMASH_DERIVE1,
                                        () -> WukongAnimations.SMASH_DERIVE2)
                                .setGreatSageCharged4Animation(
                                        () -> WukongGreatSageAnimations.XULI_HEAVY_4)
                                .setJumpAttackHeavy(() -> WukongAnimations.JUMP_ATTACK_HEAVY));
        // 立棍
        PILLAR_HEAVY_ATTACK =
                registryWorker.build(
                        "pillar_heavy_attack",
                        PillarHeavyAttack::new,
                        PillarHeavyAttack.createChargedAttack()
                                .setStartAnimations(
                                        () -> WukongAnimations.PILLAR_START0,
                                        () -> WukongAnimations.PILLAR_START1,
                                        () -> WukongAnimations.PILLAR_START2,
                                        () -> WukongAnimations.PILLAR_START3,
                                        () -> WukongAnimations.PILLAR_START4) // 蓄力前摇
                                .setUpAnimations(
                                        () -> WukongAnimations.PILLAR_UP,
                                        () -> WukongAnimations.PILLAR_UP,
                                        () -> WukongAnimations.PILLAR_UP)
                                .setHeavyAttacks(
                                        () -> WukongAnimations.PILLAR_HEAVY0,
                                        () -> WukongAnimations.PILLAR_HEAVY1,
                                        () -> WukongAnimations.PILLAR_HEAVY2,
                                        () -> WukongAnimations.PILLAR_HEAVY3,
                                        () -> WukongAnimations.PILLAR_HEAVY4)
                                .setDeriveAnimations(
                                        () -> WukongAnimations.PILLAR_HEAVY_FENGYUNZHUAN,
                                        () -> WukongAnimations.PILLAR_HEAVY_FENGYUNZHUANEND,
                                        () -> WukongAnimations.PILLAR_HEAVY_RIVERSEAFLIP));

        // THRUST_HEAVY_ATTACK 戳棍
        THRUST_HEAVY_ATTACK =
                registryWorker.build(
                        "thrust_heavy_attack",
                        ThrustHeavyAttack::new,
                        ThrustHeavyAttack.createChargedAttack()
                                .setDeriveAnimations(
                                        () -> WukongAnimations.THRUST_RETREAT,
                                        () -> WukongAnimations.THRUST_FOOTAGE,
                                        () -> WukongAnimations.THRUST_JUESICK_FENGCHUANHUA,
                                        () -> WukongAnimations.THRUST_JUESICK_START,
                                        () -> WukongAnimations.THRUST_JUESICK_LOOP,
                                        () -> WukongAnimations.THRUST_JUESICK_END)
                                .setHeavyAttacks(
                                        () -> WukongAnimations.THRUST_CHARGED0,
                                        () -> WukongAnimations.THRUST_CHARGED1,
                                        () -> WukongAnimations.THRUST_CHARGED2,
                                        () -> WukongAnimations.THRUST_CHARGED3,
                                        () -> WukongAnimations.THRUST_JUESICK_FENGCHUANHUA)
                                .setStartAttacks(() -> WukongAnimations.THRUST_XULI_START));

        GREATSAGE_HEAVY_ATTACK =
                registryWorker.build(
                        "greatsage_heavy_attack",
                        GreatSageHeavyAttack::new,
                        GreatSageHeavyAttack.createChargedAttack()
                                .setDerivedHeavyAttacks1(
                                        () -> WukongGreatSageAnimations.BROKEN_STICK_STYLE,
                                        () -> WukongGreatSageAnimations.SWEEP_JAB_STYLE,
                                        () -> WukongGreatSageAnimations.WIND_CLOUD_STYLE)
                                .setDerivedHeavyAttacks2(
                                        () -> WukongGreatSageAnimations.CHOP_STICK_STYLE,
                                        () -> WukongGreatSageAnimations.CHOP_STICK_STYLE,
                                        () -> WukongAnimations.PILLAR_HEAVY_RIVERSEAFLIP,
                                        () -> WukongGreatSageAnimations.HEAVY_AUTO2_3)
                                .setChargePreAnimation(() -> WukongAnimations.SMASH_CHARGING_PRE)
                                // 大圣蓄力重棍整体上调一档: 0~3星依次对应劈棍1~4星, 4星为独立大圣棍
                                .setHeavyAttacks(
                                        () -> WukongAnimations.SMASH_CHARGED1,
                                        () -> WukongAnimations.SMASH_CHARGED2,
                                        () -> WukongAnimations.SMASH_CHARGED3,
                                        () -> WukongAnimations.SMASH_CHARGED4,
                                        () -> WukongGreatSageAnimations.XULI_HEAVY_4)
                                .setPillarHeavyAttacks(
                                        () -> WukongAnimations.PILLAR_HEAVY0,
                                        () -> WukongAnimations.PILLAR_HEAVY1,
                                        () -> WukongAnimations.PILLAR_HEAVY2,
                                        () -> WukongAnimations.PILLAR_HEAVY3,
                                        () -> WukongAnimations.PILLAR_HEAVY4)
                                .setPillarStartAttacks(
                                        () -> WukongAnimations.PILLAR_START0,
                                        () -> WukongAnimations.PILLAR_START1,
                                        () -> WukongAnimations.PILLAR_START2,
                                        () -> WukongAnimations.PILLAR_START3,
                                        () -> WukongAnimations.PILLAR_START4)
                                .setUpStartAttack(() -> WukongAnimations.PILLAR_UP));

        SMASH_STYLE =
                registryWorker.build(
                        "smash_style",
                        StaffStance::new,
                        StaffStance.createStaffStyle().setStyle(WukongStyles.SMASH));
        THRUST_STYLE =
                registryWorker.build(
                        "thrust_style",
                        StaffStance::new,
                        StaffStance.createStaffStyle().setStyle(WukongStyles.THRUST));
        PILLAR_STYLE =
                registryWorker.build(
                        "pillar_style",
                        StaffStance::new,
                        StaffStance.createStaffStyle().setStyle(WukongStyles.PILLAR));
        GREATSAGE_STYLE =
                registryWorker.build(
                        "greatsage_style",
                        StaffStance::new,
                        StaffStance.createStaffStyle().setStyle(WukongStyles.GREATSAGE));

        SPELL_JUXINGSANQI =
                registryWorker.build(
                        "spell_jxsq",
                        ShenfaJuxingsanqiSkill::new,
                        ShenfaJuxingsanqiSkill.create()
                                .setDeriveAnimations(
                                        () -> WukongAnimations.SHENFA_MAGICARTS_JQSQ_START,
                                        () -> WukongAnimations.SHENFA_MAGICARTS_JQSQ_END,
                                        () ->
                                                WukongAnimations
                                                        .SHENFA_MAGICARTS_JQSQ_DISPLACEMENT_END));
        SPELL_TONGTOUTIEBI =
                registryWorker.build(
                        "spell_tttb",
                        ShenfaTongtoutiebiSkill::new,
                        ShenfaTongtoutiebiSkill.create()
                                .setDeriveAnimations(
                                        () -> WukongAnimations.SHENFA_MAGICARTS_TTTB_START,
                                        () -> WukongAnimations.SHENFA_MAGICARTS_TTTB_FAIL));

        MAGI_ANSHENFA =
                registryWorker.build(
                        "spell_asf",
                        FashuAnshenfaSkill::new,
                        FashuAnshenfaSkill.create()
                                .setDeriveAnimations(
                                        () -> WukongAnimations.FASHU_MAGICARTS_ASF_START));

        MAGI_DINGSHENFA =
                registryWorker.build(
                        "spell_dsf",
                        FashuDingshenfaSkill::new,
                        FashuDingshenfaSkill.create()
                                .setDeriveAnimations(
                                        () -> WukongAnimations.FASHU_MAGICARTS_DSF_START));

        SHEN_WAI_SHEN_FA =
                registryWorker.build(
                        "spell_swsf",
                        ShenWaiShenFaSkill::new,
                        ShenWaiShenFaSkill.create()
                                .setDeriveAnimations(() -> WukongAnimations.HAOMAO_MAGICARTS_FS));
    }
}
