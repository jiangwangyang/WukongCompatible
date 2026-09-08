package com.p1nero.wukong.entity.client;

import com.p1nero.wukong.client.particle.WuKongParticles;
import com.p1nero.wukong.network.packet.BasePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public record DingAfterImageParticle(int id) implements BasePacket {
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
    }

    public static DingAfterImageParticle decode(FriendlyByteBuf buf){
        return new DingAfterImageParticle(buf.readInt());
    }

    @Override
    public void execute(Player player) {
        if(Minecraft.getInstance().player != null && Minecraft.getInstance().level != null){
            Entity entity = Minecraft.getInstance().level.getEntity(id);
            if(entity != null){
                double eyeHeight = ((LivingEntity) entity).getEyeHeight();
                // 定身法显示"定"字, 使用 DING 粒子(textures/particle/ding.png);
                // ENTITY_AFTER_IMAGE 已改名为 shen, 仅供聚形散气残影显示"身"字
                Minecraft.getInstance().level.addParticle(WuKongParticles.DING.get(), entity.getX(), entity.getY()+ eyeHeight+1, entity.getZ(), Double.longBitsToDouble(entity.getId()), 1, 0);
            }
        }
    }
}