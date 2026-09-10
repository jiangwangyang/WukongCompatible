package com.p1nero.wukong.capability;

import com.p1nero.wukong.WukongMoveset;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class WKCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<WKPlayer> WK_PLAYER =
            CapabilityManager.get(new CapabilityToken<>() {});

    private WKPlayer wkPlayer = null;

    private final LazyOptional<WKPlayer> optional = LazyOptional.of(this::createWKPlayer);

    private WKPlayer createWKPlayer() {
        if (this.wkPlayer == null) {
            this.wkPlayer = new WKPlayer();
        }

        return this.wkPlayer;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(
            @NotNull Capability<T> capability, @Nullable Direction direction) {
        if (capability == WK_PLAYER) {
            return optional.cast();
        }

        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        createWKPlayer().saveNBTData(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        createWKPlayer().loadNBTData(tag);
    }

    // 能力注册事件监听内部类: 处理能力附加与玩家克隆时的数据迁移
    @Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID)
    public static class Registration {
        // 为玩家实体附加 WKPlayer 能力(仅当尚未附加时)
        @SubscribeEvent
        public static void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                if (!event.getObject().getCapability(WKCapabilityProvider.WK_PLAYER).isPresent()) {
                    event.addCapability(
                            ResourceLocation.fromNamespaceAndPath(
                                    WukongMoveset.MOD_ID, "wk_player"),
                            new WKCapabilityProvider());
                }
            }
        }

        // 玩家克隆(下界传送返回/死亡重生)时, 若为死亡重生则将旧实体的 WKPlayer 数据复制到新实体
        @SubscribeEvent
        public static void onPlayerCloned(PlayerEvent.Clone event) {
            event.getOriginal().reviveCaps();
            if (event.isWasDeath()) {
                event.getOriginal()
                        .getCapability(WKCapabilityProvider.WK_PLAYER)
                        .ifPresent(
                                oldStore -> {
                                    event.getEntity()
                                            .getCapability(WKCapabilityProvider.WK_PLAYER)
                                            .ifPresent(
                                                    newStore -> {
                                                        newStore.copyFrom(oldStore);
                                                    });
                                });
            }
        }

        @SubscribeEvent
        // 注册 WKPlayer 能力类型
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(WKPlayer.class);
        }
    }
}
