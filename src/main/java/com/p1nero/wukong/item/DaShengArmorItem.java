package com.p1nero.wukong.item;


import com.p1nero.wukong.item.client.DaShengArmorRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;


import java.util.List;
import java.util.function.Consumer;


public class DaShengArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DaShengArmorItem(ArmorMaterial materialIn, Type type, Properties builder) {
        super(materialIn, type, builder);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 20, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop("idle"));
            Entity entity = state.getData(DataTickets.ENTITY);

            if (entity instanceof ArmorStand){
                return PlayState.CONTINUE;
            }

            for (ItemStack stack : entity.getArmorSlots()) {
                if (stack.isEmpty()){
                    return PlayState.STOP;
                }
            }

            return PlayState.CONTINUE;
        }));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level p_41422_, @NotNull List<Component> list, @NotNull TooltipFlag p_41424_) {
        super.appendHoverText(itemStack, p_41422_, list, p_41424_);
        list.add(Component.literal("好！好！好！").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        list.add(Component.literal("全套穿着获得筋斗云（创造飞行）"));
        list.add(Component.literal("【凝星制作组】赞助").withStyle(ChatFormatting.GREEN));
    }
    @Override
    public void inventoryTick(@NotNull ItemStack p_41404_, @NotNull Level level, @NotNull Entity entity, int p_41407_, boolean p_41408_) {
        super.inventoryTick(p_41404_, level, entity, p_41407_, p_41408_);
        if (entity instanceof Player player) {
            if (isFullArmor(player)) {
                if (!level.isClientSide) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1, 1));

                    if (player instanceof ServerPlayer serverPlayer && serverPlayer.getAbilities().flying) {
                        ServerLevel serverLevel = serverPlayer.serverLevel(); // 获取服务器世界实例
                        double x = serverPlayer.getX();
                        double y = serverPlayer.getY();
                        double z = serverPlayer.getZ();

                        serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 5, 0, 0, 0, 0.01);
                        serverLevel.sendParticles(ParticleTypes.CLOUD, x + 0.4, y, z, 5, 0, 0, 0, 0.01);
                        serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z + 0.4, 5, 0, 0, 0, 0.01);
                        serverLevel.sendParticles(ParticleTypes.CLOUD, x - 0.4, y, z, 5, 0, 0, 0, 0.01);
                        serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z - 0.4, 5, 0, 0, 0, 0.01);
                    }
                }
                if (!player.isCreative()) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities(); // 1.20.1 必须调用以同步更新能力
                }
            } else if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities(); // 关闭飞行能力后需要同步更新
            }
        }
    }

    private boolean isFullArmor(Player player) {
        return !player.getMainHandItem().isEmpty() && player.getMainHandItem().is(WukongItems.JIN_GU_BANG.get()) &&
                !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && player.getItemBySlot(EquipmentSlot.HEAD).is(WukongItems.DASHENG_H.get()) &&
                !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && player.getItemBySlot(EquipmentSlot.CHEST).is(WukongItems.DASHENG_C.get()) &&
                !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && player.getItemBySlot(EquipmentSlot.LEGS).is(WukongItems.DASHENG_L.get()) &&
                !player.getItemBySlot(EquipmentSlot.FEET).isEmpty() && player.getItemBySlot(EquipmentSlot.FEET).is(WukongItems.DASHENG_F.get());
    }
    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GeoArmorRenderer<?> renderer;
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.renderer == null)
                    this.renderer = new DaShengArmorRenderer();

                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

}