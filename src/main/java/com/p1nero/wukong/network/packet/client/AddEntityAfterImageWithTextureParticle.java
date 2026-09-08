package com.p1nero.wukong.network.packet.client;


import com.p1nero.wukong.client.particle.WuKongParticles;
import com.p1nero.wukong.network.packet.BasePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 手动加残影，不知道为何serverLevel.sendParticles(EpicFightParticles.ENTITY_AFTER_IMAGE.get(), player.getX(), player.getY(), player.getZ(), 0, Double.longBitsToDouble(player.getId()), 0.0, 0.0, 1.0);无效
 */
public record AddEntityAfterImageWithTextureParticle(int id) implements BasePacket {

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
    }

    public static AddEntityAfterImageWithTextureParticle decode(FriendlyByteBuf buf){
        return new AddEntityAfterImageWithTextureParticle(buf.readInt());
    }

    @Override
    public void execute(Player player) {
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().level != null){
            Entity entity = Minecraft.getInstance().level.getEntity(id);
            if(entity != null){
                // 字符粒子以 y 为中心渲染, 用脚底坐标会半个身子陷进地面, 抬高 1 格
                Minecraft.getInstance().level.addParticle(WuKongParticles.ENTITY_AFTER_IMAGE.get(), entity.getX(), entity.getY() + 1.0, entity.getZ(), Double.longBitsToDouble(entity.getId()), 0.0, 0.0);
            }
        }
    }
}
