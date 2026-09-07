package com.p1nero.wukong.epicfight.skill.custom.fashu;

import yesman.epicfight.skill.SkillBuilder;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.capability.WKPlayer;
import com.p1nero.wukong.capability.entity.FakeWukongEntityPatch;
import com.p1nero.wukong.entity.FakeWukongEntity;
import com.p1nero.wukong.epicfight.WukongSkillCategories;
import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.avatar.FakeWukongEntityRegistry;
import com.p1nero.wukong.epicfight.skill.custom.avatar.HeavyAttack;
import com.p1nero.wukong.epicfight.skill.custom.wukong.SmashHeavyAttack;
import com.p1nero.wukong.epicfight.skill.custom.wukong.ThrustHeavyAttack;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.http.message.HeaderValueParser;
import yesman.epicfight.api.animation.AnimationManager;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.skill.*;
import yesman.epicfight.skill.weaponinnate.WeaponInnateSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ShenWaiShenFaSkill extends Skill {
    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0282ac114513");
    protected StaticAnimationProvider deriveAnimation1;
    public ShenWaiShenFaSkill(Builder builder) {
        super(builder);
        deriveAnimation1 = builder.derive1;
    }

    public static ShenWaiShenFaSkill.Builder create() {
        return new ShenWaiShenFaSkill.Builder().setCategory(WukongSkillCategories.HAO_MAO).setResource(Resource.NONE);
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);

        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID, (hurtEvent -> {
            if(hurtEvent.getPlayerPatch().getOriginal() == hurtEvent.getDamageSource().getEntity() || (hurtEvent.getDamageSource().getEntity() instanceof FakeWukongEntity fakeWukongEntity && fakeWukongEntity.getOwner() != null && hurtEvent.getPlayerPatch().getOriginal().getId() == fakeWukongEntity.getOwner().getId())){
                hurtEvent.setCanceled(true);
                hurtEvent.setResult(AttackResult.ResultType.MISSED);
                hurtEvent.setCanceled(true);
            }
        }),10);
        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.ACTION_EVENT_SERVER, EVENT_UUID, (actionEvent -> {

            AnimationManager.AnimationAccessor<? extends MainFrameAnimation> animation = actionEvent.getAnimation();
            ServerPlayerPatch executor = actionEvent.getPlayerPatch();
            SkillContainer weaponContainer = executor.getSkill(SkillSlots.WEAPON_INNATE);
            if (weaponContainer == null || weaponContainer.isEmpty()) {
                return;
            }
            Skill weaponInnate = weaponContainer.getSkill();

            if(weaponInnate instanceof HeavyAttack heavyAttacks){
                if( animation.equals(WukongAnimations.STAFF_AUTO5)||isAnimationInList(heavyAttacks,animation)){
                    executor.getOriginal().getCapability(WKCapabilityProvider.WK_PLAYER).ifPresent(wkPlayer -> {
                        for(int id : wkPlayer.getFakeWukongIds()){
                            if(executor.getOriginal().level().getEntity(id) instanceof FakeWukongEntity fakeWukongEntity){
                                if(executor.getTarget() != null && fakeWukongEntity.distanceTo(executor.getTarget()) < 4){
                                    fakeWukongEntity.getLookControl().setLookAt(executor.getTarget());
                                    FakeWukongEntityPatch fakeWukongEntityPatch = EpicFightCapabilities.getEntityPatch(fakeWukongEntity, FakeWukongEntityPatch.class);
                                    if (fakeWukongEntityPatch != null) {
                                        fakeWukongEntityPatch.playAnimationSynchronized(animation, 0.15F);
                                    }
                                }
                            }
                        }
                    });
                }
            };
        }));
    }
    private boolean isAnimationInList(HeavyAttack animations, AnimationManager.AnimationAccessor<? extends MainFrameAnimation> animation) {
        for (StaticAnimationProvider animationz : animations.getHeavyAttacks()) {
            if (animationz.get().equals(animation)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        container.getExecutor().getEventListener().removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
    }

    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executor = container.getServerExecutor();
        super.executeOnServer(container, args);
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executor.getOriginal();
        if( dataManager.getDataValue(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get()) ){
            executor.playAnimationSynchronized(deriveAnimation1.get(), 0F);
            dataManager.setDataSync(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get(),false, player);
            dataManager.setDataSync(WukongSkillDataKeys.SWSF_COOLING_TIMER.get(),2400, player);//800
           // PILLAR_CHARGED_HEAVY4(player,executor);
        }else{
            player.sendSystemMessage(Component.literal("Shenwaishenfa is cooling down."));
        }


    }
  /*  public static void PILLAR_CHARGED_HEAVY4(ServerPlayer player,ServerPlayerPatch executor) {
        Vec3 playerPos = player.position();
        Vec3 particleOrigin = playerPos.subtract(0, 1, 0);
        ServerLevel serverLevel = (ServerLevel) player.level();
        int particleCount = 7;
        float radius = 5F;
        for (int i = 0; i < particleCount; i++) {
            float angle = (float) i / particleCount * (float) Math.PI * 2;
            float xOffset = radius * (float) Math.cos(angle);
            float zOffset = radius * (float) Math.sin(angle);
            Vec3 particlePos = particleOrigin.add(xOffset, 0, zOffset);
            serverLevel.sendParticles(ParticleTypes.POOF, particlePos.x, particlePos.y + 2, particlePos.z, 20, 0, 0, 0, 0.1);
            FakeWukongEntity fakeWukongEntity = new FakeWukongEntity(executor.getOriginal());
            fakeWukongEntity.setPos(particleOrigin.add(xOffset, 1, zOffset));
            executor.getOriginal().serverLevel().addFreshEntity(fakeWukongEntity);
            FakeWukongEntityRegistry.registerFakeWukongEntity(player, fakeWukongEntity.getId());
//            int entityId=fakeWukongEntity.getId();
//            Entity entity = serverLevel.getEntity(entityId);
//            FakeWukongEntityPatch patch = EpicFightCapabilities.getEntityPatch(entity, FakeWukongEntityPatch.class);
//            if (patch != null) {
//                patch.playAnimationSynchronized(WukongAnimations.THRUST_FOOTAGE, 0);
//            }
        }

    }*/

    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();
        if(container.getExecutor().isLogicalClient()){

        } else {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();

            if(!dataManager.getDataValue(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get())){
                dataManager.setDataSync(WukongSkillDataKeys.SWSF_COOLING_TIMER.get(), Math.max(dataManager.getDataValue(WukongSkillDataKeys.SWSF_COOLING_TIMER.get()) - 1, 0), serverPlayer);
                if (dataManager.getDataValue(WukongSkillDataKeys.SWSF_COOLING_TIMER.get()) == 0)
                    dataManager.setDataSync(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get(), true, serverPlayer);
            }

        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
        Window sr = Minecraft.getInstance().getWindow();
        int width = sr.getGuiScaledWidth();
        int height = sr.getGuiScaledHeight();
        int alpha = 128; // 50% 閫忔槑搴?
        Vec2i pos = ClientConfig.getWeaponInnatePosition(width, height);
        ResourceLocation styleTexture = new ResourceLocation(WukongMoveset.MOD_ID, "textures/gui/skills/spell_swsf.png");
        if (container.getDataManager().getDataValue(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get())) {
            alpha = 255;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        guiGraphics.blit(styleTexture, pos.x - 42, pos.y-4, 20, 20, 0.0f, 0f, 1, 1, 1, 1);
        if (!container.getDataManager().getDataValue(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get()) ) {
            float second = (container.getDataManager().getDataValue(WukongSkillDataKeys.SWSF_COOLING_TIMER.get()) / 20.0F);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 255.0f);
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    String.format("%.1f", second),
                    pos.x - 42 + (20 -  Minecraft.getInstance().font.width(String.format("%.1f", second))) / 2+1,pos.y - 4 + (20 -  Minecraft.getInstance().font.lineHeight) / 2+1,
                    16777215 // 鏂囨湰棰滆壊锛堢櫧鑹诧級
            );
        }
    }
    @Override
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }
    @Override
    public Skill registerPropertiesToAnimation() {
        return this;
    }
    @Override
    public boolean canExecute(SkillContainer container) {
        return super.canExecute(container);
    }



    public static class Builder extends SkillBuilder<ShenWaiShenFaSkill> {
        protected StaticAnimationProvider derive1;
        public Builder() {
        }

        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        public Builder setActivateType(ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        public Builder setResource(Resource resource) {
            this.resource = resource;
            return this;
        }

        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }
        public Builder setDeriveAnimations(StaticAnimationProvider derive1) {
            this.derive1 = derive1;
            return this;
        }
    }

}
