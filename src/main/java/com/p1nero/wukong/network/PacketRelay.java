package com.p1nero.wukong.network;

import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.network.packet.client.ClientSyncPlayerCapabilityPacket;
import com.p1nero.wukong.network.packet.server.ServerSyncPlayerCapabilityPacket;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.TargetPoint;
import net.minecraftforge.network.simple.SimpleChannel;

// 网络包发送工具类: 封装向玩家/区域/全体/服务端/维度发送数据包, 以及玩家能力数据的双向同步
public class PacketRelay {
    public PacketRelay() {}

    // 向指定玩家发送数据包
    public static <MSG> void sendToPlayer(SimpleChannel handler, MSG message, ServerPlayer player) {
        handler.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    // 向以 (x,y,z) 为中心, 指定半径与维度内的玩家发送数据包
    public static <MSG> void sendToNear(
            SimpleChannel handler,
            MSG message,
            double x,
            double y,
            double z,
            double radius,
            ResourceKey<Level> dimension) {
        handler.send(
                PacketDistributor.NEAR.with(TargetPoint.p(x, y, z, radius, dimension)), message);
    }

    // 向所有玩家广播数据包
    public static <MSG> void sendToAll(SimpleChannel handler, MSG message) {
        handler.send(PacketDistributor.ALL.noArg(), message);
    }

    // 向服务端发送数据包 (客户端调用)
    public static <MSG> void sendToServer(SimpleChannel handler, MSG message) {
        handler.sendToServer(message);
    }

    // 向指定维度内的所有玩家发送数据包
    public static <MSG> void sendToDimension(
            SimpleChannel handler, MSG message, ResourceKey<Level> dimension) {
        handler.send(
                PacketDistributor.DIMENSION.with(
                        () -> {
                            return dimension;
                        }),
                message);
    }

    // 服务端 -> 客户端: 将玩家能力数据保存为 NBT 并发送给该玩家同步
    public static void syncPlayer(ServerPlayer serverPlayer) {
        serverPlayer
                .getCapability(WKCapabilityProvider.WK_PLAYER)
                .ifPresent(
                        wkPlayer -> {
                            CompoundTag oldData = new CompoundTag();
                            wkPlayer.saveNBTData(oldData);
                            sendToPlayer(
                                    PacketHandler.INSTANCE,
                                    new ClientSyncPlayerCapabilityPacket(oldData),
                                    serverPlayer);
                        });
    }

    // 客户端 -> 服务端: 将本地玩家能力数据保存为 NBT 并发送到服务端同步
    public static void syncPlayer(LocalPlayer localPlayer) {
        localPlayer
                .getCapability(WKCapabilityProvider.WK_PLAYER)
                .ifPresent(
                        wkPlayer -> {
                            CompoundTag oldData = new CompoundTag();
                            wkPlayer.saveNBTData(oldData);
                            sendToServer(
                                    PacketHandler.INSTANCE,
                                    new ServerSyncPlayerCapabilityPacket(oldData));
                        });
    }
}
