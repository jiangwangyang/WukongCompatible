package com.p1nero.wukong.client.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

// 定身结束时的特效处理类, 在实体位置播放发光与落蜡粒子
public class DingEndEvent {
    // 在服务端为实体位置生成发光(GLOW)与落蜡(WAX_OFF)粒子
    public static void execute(Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.GLOW,
                    entity.getX(),
                    entity.getY() + 1,
                    entity.getZ(),
                    12,
                    0.25,
                    0.5,
                    0.25,
                    0.05);
            serverLevel.sendParticles(
                    ParticleTypes.WAX_OFF,
                    entity.getX(),
                    entity.getY() + 1,
                    entity.getZ(),
                    12,
                    0.25,
                    0.5,
                    0.25,
                    0.05);
        }
    }
}
