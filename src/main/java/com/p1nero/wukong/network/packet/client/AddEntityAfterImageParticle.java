package com.p1nero.wukong.network.packet.client;

import com.p1nero.wukong.network.packet.BasePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import yesman.epicfight.particle.EpicFightParticles;

// 手动发包加残影: 直接调 serverLevel.sendParticles 发 ENTITY_AFTER_IMAGE 无效, 故改为数据包手动生成
// 数据包: 在服务端请求下于指定实体位置生成白色残影粒子
public record AddEntityAfterImageParticle(int id) implements BasePacket {

    @Override
    // 将实体 id 写入字节缓冲
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
    }

    // 从字节缓冲读取实体 id 并构造数据包
    public static AddEntityAfterImageParticle decode(FriendlyByteBuf buf) {
        return new AddEntityAfterImageParticle(buf.readInt());
    }

    @Override
    // 在客户端于指定实体位置生成白色残影粒子
    public void execute(Player player) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(id);
            if (entity != null) {
                Minecraft.getInstance()
                        .level
                        .addParticle(
                                EpicFightParticles.WHITE_AFTERIMAGE.get(),
                                entity.getX(),
                                entity.getY(),
                                entity.getZ(),
                                Double.longBitsToDouble(entity.getId()),
                                0.0,
                                0.0);
            }
        }
    }
}
