package com.p1nero.wukong.entity.client;

import com.p1nero.wukong.client.particle.WuKongParticles;
import com.p1nero.wukong.network.packet.BasePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

// 定身残影网络包: 客户端收到实体 ID 后在目标头顶生成"定"字粒子(聚形散气残影则用"身"字)
public record DingAfterImageParticle(int id) implements BasePacket {
    // 将实体 ID 写入数据包
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
    }

    // 从数据包中读取实体 ID 并重建对象
    public static DingAfterImageParticle decode(FriendlyByteBuf buf) {
        return new DingAfterImageParticle(buf.readInt());
    }

    // 客户端执行: 在目标头顶生成"定"字粒子
    @Override
    public void execute(Player player) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(id);
            if (entity != null) {
                double eyeHeight = ((LivingEntity) entity).getEyeHeight();
                // 定身法显示"定"字, 使用 DING 粒子(textures/particle/ding.png);
                // ENTITY_AFTER_IMAGE 已改名为 shen, 仅供聚形散气残影显示"身"字
                Minecraft.getInstance()
                        .level
                        .addParticle(
                                WuKongParticles.DING.get(),
                                entity.getX(),
                                entity.getY() + eyeHeight + 1,
                                entity.getZ(),
                                Double.longBitsToDouble(entity.getId()),
                                1,
                                0);
            }
        }
    }
}
