package com.p1nero.wukong.network.packet.client;

import com.p1nero.wukong.client.particle.WuKongParticles;
import com.p1nero.wukong.network.packet.BasePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

// 手动发包加残影: 直接调 serverLevel.sendParticles 发 ENTITY_AFTER_IMAGE 无效, 故改为数据包手动生成
// 数据包: 在服务端请求下于指定实体位置生成带贴图的残影粒子 (以 y 为中心抬高渲染)
public record AddEntityAfterImageWithTextureParticle(int id) implements BasePacket {

    @Override
    // 将实体 id 写入字节缓冲
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
    }

    // 从字节缓冲读取实体 id 并构造数据包
    public static AddEntityAfterImageWithTextureParticle decode(FriendlyByteBuf buf) {
        return new AddEntityAfterImageWithTextureParticle(buf.readInt());
    }

    @Override
    // 在客户端于指定实体位置 (抬高 1 格) 生成带贴图的残影粒子
    public void execute(Player player) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(id);
            if (entity != null) {
                // 字符粒子以 y 为中心渲染, 用脚底坐标会半个身子陷进地面, 抬高 1 格
                Minecraft.getInstance()
                        .level
                        .addParticle(
                                WuKongParticles.ENTITY_AFTER_IMAGE.get(),
                                entity.getX(),
                                entity.getY() + 1.0,
                                entity.getZ(),
                                Double.longBitsToDouble(entity.getId()),
                                0.0,
                                0.0);
            }
        }
    }
}
