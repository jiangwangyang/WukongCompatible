package com.p1nero.wukong.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

import java.util.UUID;

// 假悟空实体: 由玩家分身出的诱饵生物, 继承拥有者的装备与目标, 用于聚形散气吸引仇恨
public class FakeWukongEntity extends TamableAnimal {

    // 以拥有者玩家构造: 驯服拥有者并复制其最大生命
    public FakeWukongEntity(ServerPlayer owner) {
        super(WukongEntities.FAKE_WUKONG_ENTITY.get(), owner.level());
        tame(owner);
        AttributeInstance instance = this.getAttribute(Attributes.MAX_HEALTH);
        if (instance != null && getOwner() != null) {
            instance.addPermanentModifier(
                    new AttributeModifier(
                            UUID.randomUUID(),
                            "original health",
                            getOwner().getMaxHealth(),
                            AttributeModifier.Operation.ADDITION));
        }
    }

    // 反序列化用构造
    public FakeWukongEntity(EntityType<? extends TamableAnimal> p_21803_, Level p_21804_) {
        super(p_21803_, p_21804_);
    }

    // 驯服时复制拥有者的手持/盔甲装备与目标
    @Override
    public void tame(@NotNull Player player) {
        super.tame(player);
        setItemSlot(EquipmentSlot.MAINHAND, player.getItemBySlot(EquipmentSlot.MAINHAND).copy());
        setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD).copy());
        setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
        setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET).copy());
        setTarget(EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class).getTarget());
    }

    // 受伤判定: 拥有者自身及其同类造成的伤害免疫, 坠落伤害免疫, 其余走父类逻辑
    @Override
    public boolean hurt(@NotNull DamageSource source, float p_27568_) {
        if (getOwner() != null && source.getEntity() != null && source.getEntity().is(getOwner())) {
            return false;
        }
        if (!source.isCreativePlayer()
                && source.getEntity() != null
                && (source.getEntity() instanceof FakeWukongEntity
                        || (getOwner() != null && source.getEntity().is(getOwner())))) {
            return false;
        }

        if (source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }

        return super.hurt(source, p_27568_);
    }

    // 注册 AI 目标与行为: 优先攻击/跟随拥有者的敌人, 其次跟随拥有者随机游走
    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(0, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, LivingEntity.class));
        this.goalSelector.addGoal(0, new FollowOwnerGoal(this, 0.5, 20.0F, 2.0F, false));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
    }

    // 构建属性: 重量/移速/破甲/冲击/连击数/攻击伤害等
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(EpicFightAttributes.WEIGHT.get())
                .add(Attributes.MOVEMENT_SPEED, 0.4F)
                .add(EpicFightAttributes.ARMOR_NEGATION.get())
                .add(EpicFightAttributes.IMPACT.get())
                .add(EpicFightAttributes.MAX_STRIKES.get())
                .add(Attributes.ATTACK_DAMAGE);
    }

    // 装备掉落概率为 0(不掉落)
    @Override
    protected float getEquipmentDropChance(@NotNull EquipmentSlot slot) {
        return 0;
    }

    // 不可繁殖, 返回 null
    @Nullable
    @Override
    public AgeableMob getBreedOffspring(
            @NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) {
        return null;
    }

    // 每帧更新: 失去拥有者则移除; 目标为残留实体则清空; 存活超过 500 tick 则消散并播放音效
    @Override
    public void tick() {
        super.tick();
        if (this.getOwner() == null) {
            this.remove(Entity.RemovalReason.DISCARDED);
        }
        if (this.getTarget() instanceof CloudStepLeftEntity) {
            this.setTarget(null);
            this.setLastHurtMob(null);
            this.targetSelector.getRunningGoals().forEach(goal -> goal.getGoal().stop());
        }

        if (this.getTarget() instanceof CloudStepLeftEntity) {
            this.setTarget(null);
            this.setLastHurtMob(null);
        }
        if (this.tickCount >= 500) {
            this.remove(Entity.RemovalReason.DISCARDED);
            this.discard();
            level().addParticle(ParticleTypes.POOF, getX(), getY() + 2, getZ(), 0, 0, 0);
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            SoundEvents.GENERIC_EXPLODE,
                            getSoundSource(),
                            1.0F,
                            1.0F);
        }
    }
}
