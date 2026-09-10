package com.p1nero.wukong.network;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.entity.client.DingAfterImageParticle;
import com.p1nero.wukong.epicfight.skill.custom.wukong.UpdateWeaponInnatePacket;
import com.p1nero.wukong.network.packet.BasePacket;
import com.p1nero.wukong.network.packet.client.AddEntityAfterImageParticle;
import com.p1nero.wukong.network.packet.client.AddEntityAfterImageWithTextureParticle;
import com.p1nero.wukong.network.packet.client.ClientSyncPlayerCapabilityPacket;
import com.p1nero.wukong.network.packet.client.PillarFovPacket;
import com.p1nero.wukong.network.packet.server.PlayStaffFlowerPacket;
import com.p1nero.wukong.network.packet.server.ServerSyncPlayerCapabilityPacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

// 网络包注册中心: 维护主通信通道 INSTANCE 并集中注册所有数据包的编解码与处理逻辑
public class PacketHandler {
    // 网络协议版本号, 客户端与服务端需一致才能通信
    private static final String PROTOCOL_VERSION = "1";
    // 主通信通道实例, 所有数据包经此收发
    public static final SimpleChannel INSTANCE =
            NetworkRegistry.newSimpleChannel(
                    ResourceLocation.fromNamespaceAndPath(WukongMoveset.MOD_ID, "main"),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals);

    // 报文 id 自增计数器, 每注册一个数据包分配一个递增 id
    private static int index;

    // 注册所有数据包: 区分客户端收包与服务端收包, 绑定各自的解码器
    public static synchronized void register() {

        // Client
        register(PlayStaffFlowerPacket.class, PlayStaffFlowerPacket::decode);
        register(ServerSyncPlayerCapabilityPacket.class, ServerSyncPlayerCapabilityPacket::decode);

        // Server
        register(DingAfterImageParticle.class, DingAfterImageParticle::decode);

        register(AddEntityAfterImageParticle.class, AddEntityAfterImageParticle::decode);
        register(ClientSyncPlayerCapabilityPacket.class, ClientSyncPlayerCapabilityPacket::decode);

        register(
                AddEntityAfterImageWithTextureParticle.class,
                AddEntityAfterImageWithTextureParticle::decode);
        register(PillarFovPacket.class, PillarFovPacket::decode);
        register(UpdateWeaponInnatePacket.class, UpdateWeaponInnatePacket::decode);
    }

    // 注册单个数据包: 分配递增报文 id, 绑定编码/解码与主线程消费逻辑
    private static <MSG extends BasePacket> void register(
            final Class<MSG> packet, Function<FriendlyByteBuf, MSG> decoder) {
        INSTANCE.messageBuilder(packet, index++)
                .encoder(BasePacket::encode)
                .decoder(decoder)
                .consumerMainThread(BasePacket::handle)
                .add();
    }
}
