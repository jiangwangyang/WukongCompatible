package com.p1nero.wukong.network.packet.client;

import com.p1nero.wukong.client.event.PillarFovController;
import com.p1nero.wukong.network.packet.BasePacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

// 数据包: 客户端收到后触发定身 (pillar) 视场角 (FOV) 变化效果
// 字段: increaseAmount=FOV 增加量, transitionTicks=过渡 tick 数, repeatTimes=重复次数
public record PillarFovPacket(float increaseAmount, int transitionTicks, int repeatTimes)
        implements BasePacket {
    @Override
    // 将 FOV 增加量, 过渡 tick, 重复次数写入字节缓冲
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(increaseAmount);
        buffer.writeVarInt(transitionTicks);
        buffer.writeVarInt(repeatTimes);
    }

    // 从字节缓冲读取三个字段并构造数据包
    public static PillarFovPacket decode(FriendlyByteBuf buffer) {
        return new PillarFovPacket(buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override
    // 在客户端启动定身 FOV 变化控制器
    public void execute(Player player) {
        PillarFovController.start(increaseAmount, transitionTicks, repeatTimes);
    }
}
