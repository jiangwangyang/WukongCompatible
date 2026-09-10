package com.p1nero.wukong.effects;

import com.p1nero.wukong.epicfight.skill.lizi.ParticleRenderTypeN;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
// "定"字粒子: 在目标头顶显示固定的"定"字贴图, 用于定身法的视觉表现
public class DingParticle extends TextureSheetParticle {
    // "定"字粒子的提供器: 通过 xSpeed 中编码的实体 ID 创建粒子
    @OnlyIn(Dist.CLIENT)
    public static class DangerParticleProvider implements ParticleProvider<SimpleParticleType> {
        // 粒子动画帧(SpriteSet), 用于驱动贴图
        private final SpriteSet spriteSet;

        // 构造提供器并持有 SpriteSet
        public DangerParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        // 创建"定"字粒子, 由 xSpeed 解码出实体 ID 用于服务端定位
        public Particle createParticle(
                SimpleParticleType typeIn,
                ClientLevel worldIn,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed) {
            Entity entity = worldIn.getEntity((int) Double.doubleToLongBits(xSpeed));
            return new DingParticle(worldIn, x, y, z, this.spriteSet);
        }
    }

    // 构造"定"字粒子: 设置尺寸(2.5x2.85), 生命周期 200 tick, 关闭重力与物理碰撞
    protected DingParticle(ClientLevel world, double x, double y, double z, SpriteSet spriteSet) {
        super(world, x, y, z);

        this.setSize(2.5f, 2.5f);
        this.quadSize *= 2.85f;
        this.lifetime = 200;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.setSpriteFromAge(spriteSet);
    }

    // 禁用视锥剔除, 保证"定"字始终渲染
    @Override
    public boolean shouldCull() {
        return false;
    }

    // 返回全亮光照值(15728880)
    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    // 使用自定义的受光且不剔除渲染类型
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderTypeN.PARTICLE_SHEET_LIT_NO_CULL;
    }
}
