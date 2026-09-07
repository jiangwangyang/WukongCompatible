package com.p1nero.wukong.epicfight.skill.custom.wukong;

import yesman.epicfight.skill.SkillBuilder;

import com.p1nero.wukong.Config;
import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.network.PacketHandler;
import com.p1nero.wukong.network.PacketRelay;
import com.p1nero.wukong.network.packet.client.AddEntityAfterImageParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.MovementDirection;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.client.input.InputUtils;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.network.client.CPSkillRequest;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.ComboCounterHandleEvent;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.List;
import java.util.UUID;

import static yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.STAMINA;

/**
 * 瀹岀編闂伩鍥炴鍔? */
public class WukongDodgeSkill extends Skill {
    private static final UUID EVENT_UUID = UUID.fromString("d2d011cc-f30f-11ed-a05b-0242ac114515");
      public static final int RESET_TICKS = 100;
    protected final StaticAnimationProvider[][] animations;

    public static WukongDodgeSkill.Builder createDodgeBuilder() {
        return (new WukongDodgeSkill.Builder()).setCategory(SkillCategories.DODGE).setActivateType(ActivateType.ONE_SHOT).setResource(Resource.STAMINA);
    }

    public WukongDodgeSkill(WukongDodgeSkill.Builder builder) {
        super(builder);
        animations = builder.animations;
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        container.getExecutor().getEventListener().addEventListener(PlayerEventListener.EventType.DODGE_SUCCESS_EVENT, EVENT_UUID, (event -> {
            Player player = event.getPlayerPatch().getOriginal();
            if(!container.getDataManager().getDataValue(WukongSkillDataKeys.DODGE_PLAYED.get())){
                // 标记完美闪避, 供蓄力保留棍势判定使用
                player.getCapability(WKCapabilityProvider.WK_PLAYER).ifPresent(wkPlayer -> wkPlayer.setPerfectDodge(true));
                event.getPlayerPatch().playSound(WuKongSounds.PERFECT_DODGE.get(), 1, 1);
                if(player.level() instanceof ServerLevel){
                    PacketRelay.sendToAll(PacketHandler.INSTANCE, new AddEntityAfterImageParticle(player.getId()));//涓嬮潰閭ｈ鏃犳晥锛屾墜鍔ㄥ彂鍖呰В鍐?//                serverLevel.sendParticles(EpicFightParticles.ENTITY_AFTER_IMAGE.get(), player.getX(), player.getY(), player.getZ(), 0, Double.longBitsToDouble(player.getId()), 0.0, 0.0, 1.0);
                }
                SkillContainer weaponInnateContainer = event.getPlayerPatch().getSkill(SkillSlots.WEAPON_INNATE);
                if (weaponInnateContainer != null && !weaponInnateContainer.isEmpty()) {
                    weaponInnateContainer.getSkill().setConsumptionSynchronize(weaponInnateContainer, weaponInnateContainer.getResource() + Config.CHARGING_SPEED.get().floatValue() * 20);
                }
                modifyStamina(event.getPlayerPatch().getOriginal(), 3.0F);
                container.getDataManager().setData(WukongSkillDataKeys.DODGE_PLAYED.get(), true);
                int direction = Mth.clamp(container.getDataManager().getDataValue(WukongSkillDataKeys.DIRECTION.get()), 0, 3);
                event.getPlayerPatch().playAnimationSynchronized(this.animations[3][direction].get(), 0.0F);
            }
        }));
    }
    public void modifyStamina(LivingEntity livingentity, float staminaChange) {
        float currentStamina = livingentity.getEntityData().get(STAMINA);
        float newStamina = Math.max(0.0F, currentStamina + staminaChange);
        livingentity.getEntityData().set(STAMINA, newStamina);
    }
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        container.getExecutor().getEventListener().removeListener(PlayerEventListener.EventType.DODGE_SUCCESS_EVENT, EVENT_UUID);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Object getExecutionPacket(SkillContainer container, FriendlyByteBuf args) {
        LocalPlayerPatch executer = container.getClientExecutor();
        Input input = executer.getOriginal().input;
        Minecraft minecraft = Minecraft.getInstance();

        // Capture the physical movement keys before Epic Fight updates the input state.
        boolean forward = minecraft.options.keyUp.isDown();
        boolean backward = minecraft.options.keyDown.isDown();
        boolean left = minecraft.options.keyLeft.isDown();
        boolean right = minecraft.options.keyRight.isDown();

        float pulse = Mth.clamp(0.3F + EnchantmentHelper.getSneakingSpeedBonus(executer.getOriginal()), 0.0F, 1.0F);
        InputUtils.sneakingTick(executer.getOriginal(), false, pulse);

        int vertic = forward == backward ? 0 : (forward ? 1 : -1);
        int horizon = left == right ? 0 : (left ? 1 : -1);
        if (!forward && !backward && !left && !right) {
            MovementDirection direction = MovementDirection.fromInputState(InputManager.getInputState(input));
            vertic = direction.vertical();
            horizon = direction.horizontal();
        }

        float yRot = EpicFightCameraAPI.getInstance().getForwardYRot();
        float degree = Mth.wrapDegrees((float)(-(90 * horizon * (1 - Math.abs(vertic)) + 45 * vertic * horizon)) + yRot);
        CPSkillRequest packet = new CPSkillRequest(container.getSlot());
        packet.getBuffer().writeInt(vertic < 0 ? 1 : 0);
        packet.getBuffer().writeFloat(degree);
        return packet;
    }

    @OnlyIn(Dist.CLIENT)
    public List<Object> getTooltipArgsOfScreen(List<Object> list) {
        list.add(ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(this.consumption));
        return list;
    }

    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        super.executeOnServer(container, args);
        ServerPlayerPatch executer = container.getServerExecutor();
        int i = Mth.clamp(args.readInt(), 0, 3);
        float yaw = args.readFloat();
        SkillDataManager dataManager = container.getDataManager();
        dataManager.setData(WukongSkillDataKeys.DODGE_PLAYED.get(), false);
        int count = Mth.clamp(dataManager.getDataValue(WukongSkillDataKeys.COUNT.get()), 0, 2);
//        executer.playAnimationSynchronized(this.animations[0][i].get(), 0.0F);
        executer.playAnimationSynchronized(this.animations[count][i].get(), 0.0F);//杞挱
        executer.playSound(EpicFightSounds.ROLL.get(), 1.0F, 1.0F);
        dataManager.setDataSync(WukongSkillDataKeys.DIRECTION.get(), i);//瀹岀編闂伩鐢?
        if(count != 0){
            dataManager.setDataSync(WukongSkillDataKeys.RESET_TIMER.get(), RESET_TICKS);
            BasicAttack.setComboCounterWithEvent(ComboCounterHandleEvent.Causal.ANOTHER_ACTION_ANIMATION,
                    executer, executer.getSkill(SkillSlots.BASIC_ATTACK), this.animations[count][i].get(), 0);
        }
        dataManager.setDataSync(WukongSkillDataKeys.COUNT.get(), ++count % 3);

        executer.setModelYRot(yaw, true);
    }

    /**
     * 澶箙鍒欏鍘熺涓€娈?     */
    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager manager = container.getDataManager();
        if(manager.hasData(WukongSkillDataKeys.RESET_TIMER.get()) && manager.getDataValue(WukongSkillDataKeys.RESET_TIMER.get()) > 0){
            manager.setData(WukongSkillDataKeys.RESET_TIMER.get(), manager.getDataValue(WukongSkillDataKeys.RESET_TIMER.get()) - 1);
            if(manager.getDataValue(WukongSkillDataKeys.RESET_TIMER.get()) == 1 && manager.hasData(WukongSkillDataKeys.COUNT.get())){
                manager.setData(WukongSkillDataKeys.COUNT.get(), 0);
            }
        }
    }

    public boolean isExecutableState(PlayerPatch<?> executer) {
        EntityState playerState = executer.getEntityState();
        return !executer.isInAir() && playerState.canUseSkill() && !executer.getOriginal().isInWater() && !executer.getOriginal().onClimbable() && executer.getOriginal().getVehicle() == null;
    }

    public static class Builder extends SkillBuilder<WukongDodgeSkill> {
        protected StaticAnimationProvider[][] animations = new StaticAnimationProvider[4][4];//绗竴涓弬鏁板垎鍒槸1銆?銆?娈靛拰瀹岀編闂伩锛岀浜屼釜鏄墠銆佸悗銆佸乏銆佸彸

        public Builder() {
        }

        public WukongDodgeSkill.Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        public WukongDodgeSkill.Builder setActivateType(Skill.ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        public WukongDodgeSkill.Builder setResource(Skill.Resource resource) {
            this.resource = resource;
            return this;
        }

        public WukongDodgeSkill.Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }

        public WukongDodgeSkill.Builder setAnimations1(StaticAnimationProvider... animations) {
            this.animations[0] = animations;
            return this;
        }

        public WukongDodgeSkill.Builder setAnimations2(StaticAnimationProvider... animations) {
            this.animations[1] = animations;
            return this;
        }

        public WukongDodgeSkill.Builder setAnimations3(StaticAnimationProvider... animations) {
            this.animations[2] = animations;
            return this;
        }
        public WukongDodgeSkill.Builder setPerfectAnimations(StaticAnimationProvider... animations) {
            this.animations[3] = animations;
            return this;
        }
    }
}
