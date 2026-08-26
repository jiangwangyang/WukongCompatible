package com.p1nero.wukong.network;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.epicfight.skill.custom.wukong.UpdateWeaponInnatePacket;
import com.p1nero.wukong.entity.client.DingAfterImageParticle;
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

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(WukongMoveset.MOD_ID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    private static int index;

    public static synchronized void register() {

        //Client
        register(PlayStaffFlowerPacket.class, PlayStaffFlowerPacket::decode);
        register(ServerSyncPlayerCapabilityPacket.class, ServerSyncPlayerCapabilityPacket::decode);

        //Server
        register(DingAfterImageParticle.class, DingAfterImageParticle::decode);

        register(AddEntityAfterImageParticle.class, AddEntityAfterImageParticle::decode);
        register(ClientSyncPlayerCapabilityPacket.class, ClientSyncPlayerCapabilityPacket::decode);


        register(AddEntityAfterImageWithTextureParticle.class, AddEntityAfterImageWithTextureParticle::decode);
        register(PillarFovPacket.class, PillarFovPacket::decode);
        register(UpdateWeaponInnatePacket.class, UpdateWeaponInnatePacket::decode);

    }

    private static <MSG extends BasePacket> void register(final Class<MSG> packet, Function<FriendlyByteBuf, MSG> decoder) {
        INSTANCE.messageBuilder(packet, index++).encoder(BasePacket::encode).decoder(decoder).consumerMainThread(BasePacket::handle).add();
    }
}
