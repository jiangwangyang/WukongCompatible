package com.p1nero.wukong.network.packet.server;

import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.network.packet.BasePacket;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

// 同步数据: 客户端 -> 服务端, 携带玩家能力 NBT 数据以在服务端加载
public record ServerSyncPlayerCapabilityPacket(CompoundTag old) implements BasePacket {

    @Override
    // 将能力 NBT 数据写入字节缓冲
    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(old);
    }

    // 从字节缓冲读取 NBT 数据并构造数据包
    public static ServerSyncPlayerCapabilityPacket decode(FriendlyByteBuf buf) {
        return new ServerSyncPlayerCapabilityPacket(buf.readNbt());
    }

    @Override
    // 在服务端将收到的 NBT 数据载入玩家能力
    public void execute(Player player) {
        if (player != null) {
            player.getCapability(WKCapabilityProvider.WK_PLAYER)
                    .ifPresent(
                            (wkPlayer -> {
                                wkPlayer.loadNBTData(old);
                            }));
        }
    }
}
