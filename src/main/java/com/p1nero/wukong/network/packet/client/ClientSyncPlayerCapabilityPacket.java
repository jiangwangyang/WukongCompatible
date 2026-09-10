package com.p1nero.wukong.network.packet.client;

import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.network.packet.BasePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

// 同步数据: 服务端 -> 客户端, 携带玩家能力 NBT 数据以在客户端加载
public record ClientSyncPlayerCapabilityPacket(CompoundTag old) implements BasePacket {

    @Override
    // 将能力 NBT 数据写入字节缓冲
    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(old);
    }

    // 从字节缓冲读取 NBT 数据并构造数据包
    public static ClientSyncPlayerCapabilityPacket decode(FriendlyByteBuf buf) {
        return new ClientSyncPlayerCapabilityPacket(buf.readNbt());
    }

    @Override
    // 在客户端将收到的 NBT 数据载入玩家能力
    public void execute(Player player) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            Minecraft.getInstance()
                    .player
                    .getCapability(WKCapabilityProvider.WK_PLAYER)
                    .ifPresent(
                            (wkPlayer -> {
                                wkPlayer.loadNBTData(old);
                            }));
        }
    }
}
