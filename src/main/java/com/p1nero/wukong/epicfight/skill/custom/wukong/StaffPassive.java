package com.p1nero.wukong.epicfight.skill.custom.wukong;

import static com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys.PLAYING_STAFF_SPIN;

import com.p1nero.wukong.Config;
import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.client.keymapping.WukongKeyMappings;
import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.skill.WukongSkills;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import com.p1nero.wukong.network.PacketHandler;
import com.p1nero.wukong.network.PacketRelay;
import com.p1nero.wukong.network.packet.server.PlayStaffFlowerPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.particle.EpicFightParticles;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.UUID;
import java.util.stream.Collectors;

// 棍花与闪避被动技能: 自动学习悟空闪避, 处理棍花格挡/减伤与棍势回能, 拦截并替换默认闪避
public class StaffPassive extends Skill {
    // 本技能事件监听器的唯一标识
    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0242ac191981");

    // 类型擦除辅助: 将动画访问器强制转为指定动画类型(配合棍花命中动画)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <A extends StaticAnimation> AnimationManager.AnimationAccessor<A> typed(
            AnimationManager.AnimationAccessor accessor) {
        return accessor;
    }

    // 类型擦除辅助: 将动画访问器强制转为MainFrameAnimation类型
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <A extends MainFrameAnimation> AnimationManager.AnimationAccessor<A> typedMain(
            AnimationManager.AnimationAccessor accessor) {
        return accessor;
    }

    // 构造方法
    public StaffPassive(SkillBuilder<? extends Skill> builder) {
        super(builder);
    }

    // 注册各类事件监听: 自动学闪避/棍花禁移/格挡减伤/棍势回能/拦截默认闪避
    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        // 自动学闪避
        Skill dodge = container.getExecutor().getSkill(SkillSlots.DODGE).getSkill();
        if (dodge != WukongSkills.WUKONG_DODGE) {
            container.getExecutor().getSkill(SkillSlots.DODGE).setSkill(WukongSkills.WUKONG_DODGE);
            container
                    .getExecutor()
                    .getOriginal()
                    .getCapability(WKCapabilityProvider.WK_PLAYER)
                    .ifPresent(
                            wkPlayer ->
                                    wkPlayer.setLastDodgeSkill(
                                            dodge == null ? "" : dodge.toString()));
        }
        // 棍花期间禁止移动
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT,
                        EVENT_UUID,
                        (event -> {
                            if (event.getPlayerPatch().isEpicFightMode()
                                    && WukongKeyMappings.STAFF_FLOWER.isDown()) {
                                // 弃用API迁移: getMovementInput()改为getInputState(),
                                // 通过InputManager.setInputState应用回原版输入(已核实与直接改Input字段等价,
                                // shiftKeyDown对应sneaking);
                                // 并以KeyMapping.setDown替代已弃用的ControlEngine.setKeyBind
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
                            if (event.getDamageSource().is(DamageTypes.LIGHTNING_BOLT)
                                    && WukongAnimations.STAFF_AUTO4.equals(
                                            event.getPlayerPatch()
                                                    .getAnimator()
                                                    .getPlayerFor(null)
                                                    .getAnimation())) {
                                event.setCanceled(true);
                            }

                            if (container.getDataManager().getDataValue(PLAYING_STAFF_SPIN.get())
                                    && (canBeBlocked(event.getDamageSource().getDirectEntity())
                                            || event.getDamageSource()
                                                    .is(DamageTypes.MOB_PROJECTILE))) {
                                if (!isBlocked(
                                        event.getDamageSource(),
                                        event.getPlayerPatch().getOriginal())) {
                                    return;
                                }
                                event.setCanceled(true);
                                event.setResult(AttackResult.ResultType.BLOCKED);
                                LivingEntityPatch<?> attackerPatch =
                                        (LivingEntityPatch<?>)
                                                EpicFightCapabilities.getEntityPatch(
                                                        event.getDamageSource().getEntity(),
                                                        LivingEntityPatch.class);
                                if (attackerPatch != null) {
                                    attackerPatch.setLastAttackEntity(
                                            event.getPlayerPatch().getOriginal());
                                }
                                Entity directEntity = event.getDamageSource().getDirectEntity();
                                LivingEntityPatch<?> entityPatch =
                                        (LivingEntityPatch<?>)
                                                EpicFightCapabilities.getEntityPatch(
                                                        directEntity, LivingEntityPatch.class);
                                if (entityPatch != null) {
                                    entityPatch.onAttackBlocked(
                                            event.getDamageSource(), event.getPlayerPatch());
                                }
                                showBlockedEffect(
                                        event.getPlayerPatch(),
                                        event.getDamageSource().getDirectEntity());
                                SkillContainer skillContainer =
                                        event.getPlayerPatch().getSkill(SkillSlots.WEAPON_INNATE);
                                if (skillContainer != null && !skillContainer.isEmpty()) {
                                    // 成功格挡回5棍势
                                    skillContainer
                                            .getSkill()
                                            .setConsumptionSynchronize(
                                                    skillContainer,
                                                    skillContainer.getResource() + 5.0F);
                                }
                            }
                        }));

        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE,
                        EVENT_UUID,
                        (dealtDamageEvent -> {
                            AnimationManager.AnimationAccessor<?> animation =
                                    dealtDamageEvent.getDamageSource().getAnimation();
                            if (WukongAnimations.JUMP_ATTACK_LIGHT.equals(animation)) {
                                dealtDamageEvent
                                        .getPlayerPatch()
                                        .playAnimationSynchronized(
                                                typed(WukongAnimations.JUMP_ATTACK_LIGHT_HIT),
                                                0.0F);
                                BasicAttack.setComboCounterWithEvent(
                                        ComboCounterHandleEvent.Causal.ANOTHER_ACTION_ANIMATION,
                                        dealtDamageEvent.getPlayerPatch(),
                                        dealtDamageEvent
                                                .getPlayerPatch()
                                                .getSkill(SkillSlots.BASIC_ATTACK),
                                        typedMain(WukongAnimations.JUMP_ATTACK_LIGHT_HIT),
                                        1);
                                return;
                            }
                            if (WukongAnimations.STAFF_SPIN_ONE_HAND_LOOP.equals(animation)
                                    || WukongAnimations.STAFF_SPIN_TWO_HAND_LOOP.equals(
                                            animation)) {
                                // 打中加5棍势(因为加的要比造成的伤害多)
                                SkillContainer skillContainer =
                                        dealtDamageEvent
                                                .getPlayerPatch()
                                                .getSkill(SkillSlots.WEAPON_INNATE);
                                if (skillContainer != null && !skillContainer.isEmpty()) {
                                    skillContainer
                                            .getSkill()
                                            .setConsumptionSynchronize(
                                                    skillContainer,
                                                    skillContainer.getResource() + 5.0F);
                                }
                            }
                        }));

        // 拦截闪避事件, 替换为自己的闪避并执行, 算是保险
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.SKILL_CAST_EVENT,
                        EVENT_UUID,
                        (event -> {
                            PlayerPatch<?> executer = event.getPlayerPatch();
                            Skill ordinalSkill = event.getSkillContainer().getSkill();
                            if (!ordinalSkill.getCategory().equals(SkillCategories.DODGE)
                                    || ordinalSkill.equals(WukongSkills.WUKONG_DODGE)) {
                                return;
                            }
                            if (executer.isLogicalClient()) {
                                // 临时替换为悟空闪避
                                if (!ordinalSkill.equals(WukongSkills.WUKONG_DODGE)
                                        && executer.hasStamina(this.getConsumption())) {
                                    executer.getSkill(SkillSlots.DODGE)
                                            .setSkill(WukongSkills.WUKONG_DODGE);
                                    EpicFightNetworkManager.sendToServer(
                                            new CPChangeSkill(
                                                    SkillSlots.DODGE,
                                                    -1,
                                                    WukongSkills.WUKONG_DODGE));
                                    executer.getSkill(SkillSlots.DODGE)
                                            .sendCastRequest(
                                                    (LocalPlayerPatch) executer,
                                                    ClientEngine.getInstance().controlEngine);
                                    executer.getOriginal()
                                            .getCapability(WKCapabilityProvider.WK_PLAYER)
                                            .ifPresent(
                                                    wkPlayer -> {
                                                        wkPlayer.setLastDodgeSkill(
                                                                ordinalSkill.toString());
                                                        PacketRelay.syncPlayer(
                                                                ((LocalPlayer)
                                                                        executer.getOriginal()));
                                                    });
                                    event.setCanceled(true);
                                }
                            }
                        }));
    }

    // 还原闪避技能并移除所有事件监听
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        // 把技能还原回去
        if (!container.getExecutor().isLogicalClient()) {
            PacketRelay.syncPlayer(((ServerPlayer) container.getExecutor().getOriginal()));
        }
        container
                .getExecutor()
                .getOriginal()
                .getCapability(WKCapabilityProvider.WK_PLAYER)
                .ifPresent(
                        wkPlayer -> {
                            if (wkPlayer.getLastDodgeSkill().isEmpty()) {
                                container.getExecutor().getSkill(SkillSlots.DODGE).setSkill(null);
                            } else {
                                container
                                        .getExecutor()
                                        .getSkill(SkillSlots.DODGE)
                                        .setSkill(
                                                SkillManager.getSkill(
                                                        wkPlayer.getLastDodgeSkill()));
                            }
                        });

        container
                .getExecutor()
                .getEventListener()
                .removeListener(PlayerEventListener.EventType.MOVEMENT_INPUT_EVENT, EVENT_UUID);
        container
                .getExecutor()
                .getEventListener()
                .removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
        container
                .getExecutor()
                .getEventListener()
                .removeListener(PlayerEventListener.EventType.DEAL_DAMAGE_EVENT_DAMAGE, EVENT_UUID);
        container
                .getExecutor()
                .getEventListener()
                .removeListener(PlayerEventListener.EventType.SKILL_CAST_EVENT, EVENT_UUID);
    }

    // 判断实体是否在可被棍花格挡的实体集合中(首次调用时懒加载配置)
    public static boolean canBeBlocked(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (Config.entities_can_be_blocked.isEmpty()) {
            Config.entities_can_be_blocked =
                    Config.ENTITIES_CAN_BE_BLOCKED_BY_STAFF_FLOWER.get().stream()
                            .map(
                                    entityName ->
                                            ForgeRegistries.ENTITY_TYPES.getValue(
                                                    ResourceLocation.parse(entityName)))
                            .collect(Collectors.toSet());
        }
        return Config.entities_can_be_blocked.contains(entity.getType());
    }

    // 判断伤害来源是否位于玩家正面(视线方向点积>0), 即正面且可被格挡
    private boolean isBlocked(DamageSource damageSource, ServerPlayer player) {
        Vec3 sourceLocation = damageSource.getSourcePosition();
        if (sourceLocation != null) {
            Vec3 viewVector = player.getViewVector(1.0F);
            Vec3 toSourceLocation = sourceLocation.subtract((player).position()).normalize();
            return toSourceLocation.dot(viewVector) > 0.0;
        }
        return false;
    }

    // 播放棍花格挡成功的音效与粒子效果
    public static void showBlockedEffect(ServerPlayerPatch playerPatch, Entity directEntity) {
        playerPatch.playSound(EpicFightSounds.CLASH.get(), -0.05F, 0.1F);
        ServerPlayer serverPlayer = playerPatch.getOriginal();
        EpicFightParticles.HIT_BLUNT
                .get()
                .spawnParticleWithArgument(
                        serverPlayer.serverLevel(),
                        HitParticleType.FRONT_OF_EYES,
                        HitParticleType.ZERO,
                        serverPlayer,
                        directEntity);
    }

    // 每tick检测: 地面持有效武器时长按棍花键则触发单/双手棍花并通知服务端同步
    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        if (!container.getExecutor().isLogicalClient()
                || !WukongWeaponCategories.isWeaponValid(container.getExecutor())
                || !container.getExecutor().isEpicFightMode()
                || !container.getExecutor().getOriginal().onGround()) {
            return;
        }

        if (WukongKeyMappings.STAFF_FLOWER.isDown()
                && container
                        .getExecutor()
                        .hasStamina(Config.STAFF_FLOWER_STAMINA_CONSUME.get().floatValue())) {
            if (!container.getDataManager().getDataValue(PLAYING_STAFF_SPIN.get())
                    && Minecraft.getInstance().player != null) {
                PacketRelay.sendToServer(
                        PacketHandler.INSTANCE,
                        new PlayStaffFlowerPacket(WukongKeyMappings.W.isDown())); // 按w可变双手棍花
                container.getDataManager().setDataSync(PLAYING_STAFF_SPIN.get(), true);
            }
        }
    }
}
