package com.p1nero.wukong.effects;

import com.p1nero.wukong.epicfight.skill.lizi.ParticleRenderTypeN;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class DingEntityAfterImageParticle extends TextureSheetParticle {
    private final int ID;

    @OnlyIn(Dist.CLIENT)
    public static class DangerParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public DangerParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType typeIn, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            Entity entity = level.getEntity((int) Double.doubleToLongBits(xSpeed));  // 通过 xSpeed 获取对应的实体
            if (entity != null && entity instanceof LivingEntity) {  // 确保是活体实体
                LivingEntity livingEntity = (LivingEntity) entity;
                if (livingEntity.isRemoved()) {
                    return null;  // 如果实体已经死亡，不创建粒子
                }
                // 创建并返回粒子
                return new DingEntityAfterImageParticle(level, x, y, z, this.spriteSet, entity.getId());
            }
            return null;
        }
    }

    // 在构造方法中传入 ID
    protected DingEntityAfterImageParticle(ClientLevel world, double x, double y, double z, SpriteSet spriteSet, int ID) {
        super(world, x, y, z);
        this.ID = ID;  // 保存 ID
        this.setSize(2.5f, 2.5f);
        this.quadSize *= 2.85f;
        this.lifetime = 100;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.setColor(1.0F, 1.0F, 1.0F); // 设置白色不透明
        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;  // 设置光照颜色
    }

    @Override
    public void tick() {
        super.tick();
        Entity entity = level.getEntity(ID);
        if (entity != null) {
            float health = ((LivingEntity) entity).getHealth();
            // 如果实体的健康为 0，移除粒子
            if (health == 0) this.remove();
        } else {
            this.remove();
        }
    }


    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderTypeN.PARTICLE_SHEET_LIT_NO_CULL;  // 自定义的渲染类型
    }
}
