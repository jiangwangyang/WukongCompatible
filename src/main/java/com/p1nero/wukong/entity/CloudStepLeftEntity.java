package com.p1nero.wukong.entity;

import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.WukongSkills;
import com.p1nero.wukong.epicfight.skill.custom.fashu.ShenfaJuxingsanqiSkill;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.EpicFightEntities;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CloudStepLeftEntity extends LivingEntity {
    private static final List<ItemStack> EMPTY_LIST = Collections.emptyList();
    private LivingEntityPatch<?> entityPatch;

    public CloudStepLeftEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    public CloudStepLeftEntity(LivingEntityPatch<?> entityPatch) {
        this(EpicFightEntities.DODGE_LOCATION_INDICATOR.get(), entityPatch.getOriginal().level());
        this.entityPatch = entityPatch;
        AttributeInstance instance = this.getAttribute(Attributes.MAX_HEALTH);
        if (instance != null) {
            instance.addPermanentModifier(
                    new AttributeModifier(
                            UUID.randomUUID(),
                            "original health",
                            entityPatch.getOriginal().getMaxHealth(),
                            AttributeModifier.Operation.ADDITION));
        }
        Vec3 pos = entityPatch.getOriginal().position();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        this.setPos(x, y, z);
        this.setBoundingBox(
                entityPatch.getOriginal().getBoundingBox().expandTowards(1.0, 0.0, 1.0));
        if (this.level().isClientSide()) {
            this.discard();
        }
    }

    public void tick() {
        // tickCount 手动自增, 否则下方的超时兜底永远不会触发
        this.tickCount++;
        if (entityPatch == null) {
            this.discard();
            return;
        }
        level().getNearbyEntities(
                        LivingEntity.class,
                        TargetingConditions.forCombat(),
                        entityPatch.getOriginal(),
                        new AABB(
                                this.position().add(-30, -30, -30),
                                this.position().add(30, 30, 30)))
                .forEach(
                        entity -> {
                            if (entityPatch.getOriginal().equals(entity.getLastHurtMob())) {
                                entity.setLastHurtMob(this);
                            }
                            if (entity instanceof FakeWukongEntity) {
                                return;
                            }
                            if (entity instanceof Warden warden) {
                                warden.increaseAngerAt(this);
                            } else if (entity instanceof Mob mob) {
                                mob.setTarget(this);
                            }
                        });

        if (entityPatch instanceof ServerPlayerPatch serverPlayerPatch) {
            if (serverPlayerPatch
                    .getSkill(WukongSkillSlots.SHENFA_SKILL_SLOT)
                    .hasSkill(WukongSkills.SPELL_JUXINGSANQI)) {
                if (serverPlayerPatch
                                .getSkill(WukongSkillSlots.SHENFA_SKILL_SLOT)
                                .getDataManager()
                                .getDataValue(WukongSkillDataKeys.JXSQ_YINGSHEN_TIMER.get())
                        < 10) {
                    this.discard();
                }
            }
        }
        if (this.tickCount > ShenfaJuxingsanqiSkill.MAX_TIME) {
            this.discard();
        }
    }

    public @NotNull Iterable<ItemStack> getArmorSlots() {
        return EMPTY_LIST;
    }

    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    public void setItemSlot(@NotNull EquipmentSlot slot, @NotNull ItemStack stack) {}

    public @NotNull HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        return true;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }
}
