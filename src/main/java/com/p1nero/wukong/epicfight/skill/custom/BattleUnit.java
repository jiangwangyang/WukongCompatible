package com.p1nero.wukong.epicfight.skill.custom;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.capability.WKPlayer;
import com.p1nero.wukong.client.particle.WuKongEffect;
import com.p1nero.wukong.entity.FakeWukongEntity;
import com.p1nero.wukong.entity.client.DingAfterImageParticle;
import com.p1nero.wukong.epicfight.WukongSkillSlots;

import com.p1nero.wukong.epicfight.skill.EntitySpeedData;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.WukongSkills;
import com.p1nero.wukong.epicfight.skill.custom.avatar.FakeWukongEntityRegistry;
import com.p1nero.wukong.epicfight.skill.custom.fashu.FashuDingshenfaSkill;
import com.p1nero.wukong.epicfight.skill.custom.wukong.UpdateWeaponInnatePacket;
import com.p1nero.wukong.network.PacketHandler;
import com.p1nero.wukong.network.PacketRelay;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.DodgeAnimation;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import java.util.List;

public class BattleUnit {

    public BattleUnit() {
    }
    public static void fenshen(LivingEntityPatch<?> entitypatch) {
        if (entitypatch instanceof ServerPlayerPatch serverPlayerPatch) {
            ServerPlayer player = serverPlayerPatch.getOriginal();
            Vec3 position = player.position(); // 获取玩家位置
            Vec3 particleOrigin = position.subtract(0, 1, 0);
            if(entitypatch.getOriginal() instanceof ServerPlayer serverPlayer){
                ServerLevel serverLevel = (ServerLevel) serverPlayer.level();
                int particleCount = 7;
                float radius = 5F;
                float angleIncrement = (float) Math.PI * 2 / particleCount;
                for (int i = 0; i < particleCount; i++) {
                    float angle = i * angleIncrement;
                    float xOffset = radius * (float) Math.cos(angle);
                    float zOffset = radius * (float) Math.sin(angle);
                    Vec3 particlePos = particleOrigin.add(xOffset, 0, zOffset);
                    serverLevel.sendParticles(ParticleTypes.POOF, particlePos.x, particlePos.y + 2, particlePos.z, 20, 0, 0, 0, 0.1);
                    FakeWukongEntity fakeWukongEntity = new FakeWukongEntity(serverPlayerPatch.getOriginal());
                    fakeWukongEntity.setPos(particleOrigin.add(xOffset, 1, zOffset));  // 设置位置
                    serverLevel.getLevel().addFreshEntity(fakeWukongEntity);
                   // serverLevel.getCapability(WKCapabilityProvider.WK_PLAYER).ifPresent(wkPlayer -> wkPlayer.addFakeWukongId(fakeWukongEntity.getId()));


                }
            }


        }
    }

    public static void greatSageMode(LivingEntityPatch<?> entitypatch) {
        if (!(entitypatch instanceof ServerPlayerPatch serverPlayerPatch)) {
            return;
        }

        ServerPlayer player = serverPlayerPatch.getOriginal();
        new UpdateWeaponInnatePacket(3).execute(player);
        player.displayClientMessage(
                Component.translatable("tips.wukong.style_change").append(WukongSkills.GREATSAGE_STYLE.getDisplayName()),
                true
        );
    }


    public static void ding(LivingEntityPatch<?> entitypatch) {
        LivingEntity attackTarget = entitypatch.getTarget();
        if (attackTarget==null){
            if (entitypatch instanceof ServerPlayerPatch serverPlayerPatch) {
                ServerPlayer player = serverPlayerPatch.getOriginal();
                Vec3 position = player.position(); // 获取玩家位置
                LivingEntity closestMonster = null;
                double closestDistance = Double.MAX_VALUE;
                 List<LivingEntity> nearbyEntities = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50), entity -> entity != player && entity.isAlive() && !isFakeWukong(entity));
                for (LivingEntity entity : nearbyEntities) {
                    double distance = calculateDistance(position, entity.position());
                    if (distance < closestDistance) {
                        closestMonster = entity;
                        closestDistance = distance;
                    }
                }
                if (closestMonster != null) {
                    PacketRelay.sendToAll(PacketHandler.INSTANCE, new DingAfterImageParticle(closestMonster.getId()));
                    applyDingEffect(closestMonster);
                    updatePlayerSkillData(serverPlayerPatch, closestMonster);
                }

            }
        }
        if (attackTarget != null) {
            EntityType<?> entityType = attackTarget.getType();
            LivingEntityPatch<?> ep = EpicFightCapabilities.getEntityPatch(attackTarget, LivingEntityPatch.class);
            if (ep != null && ep.getAnimator().getPlayerFor(null).getAnimation() instanceof DodgeAnimation) return;
            // 进行定身操作

            if (isFakeWukong(attackTarget)) {return;}
            if (attackTarget.level() instanceof ServerLevel serverLevel) {
                PacketRelay.sendToAll(PacketHandler.INSTANCE, new DingAfterImageParticle(attackTarget.getId()));
                applyDingEffect(attackTarget);
            }
            if (entitypatch instanceof ServerPlayerPatch serverPlayerPatch) {
                updatePlayerSkillData(serverPlayerPatch, attackTarget);
            }
        }
    }
    private static boolean isFakeWukong(LivingEntity entity) {
        return entity.getType().toString().equals("entity.wukong.fake_wukong_entity");
    }
    private static double calculateDistance(Vec3 position, Vec3 monsterPos) {
        double deltaX = monsterPos.x - position.x;
        double deltaY = monsterPos.y - position.y;
        double deltaZ = monsterPos.z - position.z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ; // 使用平方距离避免开根号
    }
    private static void updatePlayerSkillData(ServerPlayerPatch serverPlayerPatch, LivingEntity target) {
        var spellContainer = serverPlayerPatch.getSkill(WukongSkillSlots.FASHU_SKILL_SLOT);
        if (spellContainer == null || spellContainer.isEmpty()) {
            return;
        }
        FashuDingshenfaSkill.trackTarget(serverPlayerPatch.getOriginal(), target);
        spellContainer.getDataManager().setDataSync(WukongSkillDataKeys.DSF_YINGSHEN_ZT.get(), true, serverPlayerPatch.getOriginal());
        spellContainer.getDataManager().setDataSync(WukongSkillDataKeys.DSF_COOLING_ATTACK.get(), false, serverPlayerPatch.getOriginal());
        spellContainer.getDataManager().setDataSync(WukongSkillDataKeys.DSF_DERIVE_TIMER.get(), 192, serverPlayerPatch.getOriginal());
        spellContainer.getDataManager().setDataSync(WukongSkillDataKeys.DSF_COOLING_TIMER.get(), 1000, serverPlayerPatch.getOriginal());
    }
    private static void applyDingEffect(LivingEntity attackTarget) {
        if (attackTarget != null) {
            EntityType<?> entityType = attackTarget.getType();
            attackTarget.addEffect(new MobEffectInstance(WuKongEffect.DING.get(), 192, 0));
            attackTarget.addEffect(new MobEffectInstance(MobEffects.GLOWING, 192, 0));
            EntitySpeedData.saveOriginalSpeed(attackTarget);


            attackTarget.addTag("ding");

            if (entityType == EntityType.ENDER_DRAGON) {  // 针对末影龙
                EnderDragon enderDragon = (EnderDragon) attackTarget;
                enderDragon.setNoAi(true);
                enderDragon.setAggressive(false);
              //  enderDragon.setDeltaMovement(0, 0, 0);  // 停止飞行
              //  WukongMoveset.LOGGER.info("禁用目标末影龙的AI并使其不再攻击: {}", enderDragon.getName().getString());
            } else if (attackTarget instanceof Monster monster) {  // 针对怪物类实体
                monster.setNoAi(true);
                monster.setAggressive(false);
               // monster.setDeltaMovement(0, 0, 0);  // 停止移动
              //  monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Integer.MAX_VALUE, 255, false, false));  // 使其极度减速

            }
        }
    }
    public static void CUNTUI_JIESUO(LivingEntityPatch<?> entitypatch) {
        LivingEntity attackTarget = entitypatch.getTarget();
        if(entitypatch instanceof ServerPlayerPatch serverPlayerPatch){
            serverPlayerPatch.getSkill(SkillSlots.WEAPON_INNATE).getDataManager().setDataSync(WukongSkillDataKeys.Thrust_STEOP_BACK.get(), true, serverPlayerPatch.getOriginal());
            serverPlayerPatch.getSkill(SkillSlots.WEAPON_INNATE).getDataManager().setDataSync(WukongSkillDataKeys.REPEATING_DERIVE_TIMER.get(), 30, serverPlayerPatch.getOriginal());
            serverPlayerPatch.getSkill(SkillSlots.WEAPON_INNATE).getDataManager().setDataSync(WukongSkillDataKeys.CAN_SECOND_TIMER.get(), 30, serverPlayerPatch.getOriginal());


        }
    }
    public static void CUNTUI_SHANGSUO(LivingEntityPatch<?> entitypatch) {
        LivingEntity attackTarget = entitypatch.getTarget();
        if(entitypatch instanceof ServerPlayerPatch serverPlayerPatch){
            serverPlayerPatch.getSkill(SkillSlots.WEAPON_INNATE).getDataManager().setDataSync(WukongSkillDataKeys.Thrust_STEOP_BACK.get(), false, serverPlayerPatch.getOriginal());

        }
    }





}
