package com.p1nero.wukong.network.packet.client;

import com.p1nero.wukong.client.event.PillarFovController;
import com.p1nero.wukong.network.packet.BasePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public record PillarFovPacket(float increaseAmount, int transitionTicks, int repeatTimes) implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(increaseAmount);
        buffer.writeVarInt(transitionTicks);
        buffer.writeVarInt(repeatTimes);
    }

    public static PillarFovPacket decode(FriendlyByteBuf buffer) {
        return new PillarFovPacket(buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override
    public void execute(Player player) {
        PillarFovController.start(increaseAmount, transitionTicks, repeatTimes);
    }
}
