package com.p1nero.wukong.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 所有网络数据包的基类接口: 定义编码/处理/执行的契约, 默认在主线程排队执行
public interface BasePacket {
    // 将数据包写入字节缓冲
    void encode(FriendlyByteBuf var1);

    // 在主线程队列中执行数据包逻辑, 返回 true 表示已处理
    default boolean handle(Supplier<NetworkEvent.Context> context) {
        context.get()
                .enqueueWork(
                        () -> {
                            this.execute(context.get().getSender());
                        });
        return true;
    }

    // 数据包的实际处理逻辑 (客户端或服务端侧)
    void execute(Player var1);
}
