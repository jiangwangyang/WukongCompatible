package com.p1nero.wukong.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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

import java.util.Objects;
import java.util.UUID;
public class FakeWukongEntity extends TamableAnimal {

    public FakeWukongEntity(ServerPlayer owner){
        super(WukongEntities.FAKE_WUKONG_ENTITY.get(), owner.level());
        tame(owner);
        AttributeInstance instance = this.getAttribute(Attributes.MAX_HEALTH);
        if(instance != null && getOwner() != null){
            instance.addPermanentModifier(new AttributeModifier(UUID.randomUUID(), "original health", getOwner().getMaxHealth(), AttributeModifier.Operation.ADDITION));
        }
    }

    public FakeWukongEntity(EntityType<? extends TamableAnimal> p_21803_, Level p_21804_) {
        super(p_21803_, p_21804_);
    }

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

    @Override
    public boolean hurt(@NotNull DamageSource source, float p_27568_) {
        if (getOwner() != null && source.getEntity() != null && source.getEntity().is(getOwner())) {
            return false;
        }
        if(!source.isCreativePlayer() && source.getEntity() != null && (source.getEntity() instanceof FakeWukongEntity || (getOwner() != null && source.getEntity().is(getOwner())))){
            return false;
        }

        if (source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }

        return super.hurt(source, p_27568_);
    }

    @Override
    protected void registerGoals() {
       // this.goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.0, true));
        this.targetSelector.addGoal(0, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, LivingEntity.class));
        this.goalSelector.addGoal(0, new FollowOwnerGoal(this, 0.5, 20.0F, 2.0F, false));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(EpicFightAttributes.WEIGHT.get())
                .add(Attributes.MOVEMENT_SPEED, 0.4F)
                .add(EpicFightAttributes.ARMOR_NEGATION.get())
                .add(EpicFightAttributes.IMPACT.get())
                .add(EpicFightAttributes.MAX_STRIKES.get())
                .add(Attributes.ATTACK_DAMAGE);
    }

    @Override
    protected float getEquipmentDropChance(@NotNull EquipmentSlot slot) {
        return 0;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) {
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getOwner()==null){
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
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, getSoundSource(), 1.0F, 1.0F);
        }

    }

}
