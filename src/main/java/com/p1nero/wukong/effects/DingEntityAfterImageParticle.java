package com.p1nero.wukong.effects;

import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.fashu.ShenfaJuxingsanqiSkill;
import com.p1nero.wukong.epicfight.skill.lizi.ParticleRenderTypeN;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

// 定身法残影粒子: 在聚形散气隐身期间的施法者头顶显示"定"字残影, 实体消失或隐身结束时自动移除
@OnlyIn(Dist.CLIENT)
public class DingEntityAfterImageParticle extends TextureSheetParticle {
    // 施法者实体 ID, 用于 tick 中定位对应实体以同步残影的存活与位置
    private final int id;

    // 构造定身残影粒子: 记录施法者实体 ID, 设置尺寸(2.5x2.85), 生命周期与聚形散气隐身时长一致, 关闭重力与物理碰撞
    protected DingEntityAfterImageParticle(
            ClientLevel world, double x, double y, double z, SpriteSet spriteSet, int id) {
        super(world, x, y, z);
        this.id = id;
        this.setSize(2.5f, 2.5f);
        this.quadSize *= 2.85f;
        this.lifetime = ShenfaJuxingsanqiSkill.MAX_TIME; // 与聚形散气隐身时长保持一致(200 tick = 10s)
        this.gravity = 0f;
        this.hasPhysics = false;
        this.setColor(1.0F, 1.0F, 1.0F);
        this.setSpriteFromAge(spriteSet);
    }

    // 禁用视锥剔除, 保证残影始终被渲染
    @Override
    public boolean shouldCull() {
        return false;
    }

    // 返回全亮光照值(15728880), 使残影在暗处也完全可见
    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    // 每帧根据实体 ID 同步残影: 实体消失/生命归零/隐身结束时移除粒子
    @Override
    public void tick() {
        super.tick();
        Entity entity = level.getEntity(id);
        if (entity != null) {
            float health = ((LivingEntity) entity).getHealth();
            if (health == 0) this.remove();
            // 聚形散气结束时(主动攻击/受击/倒计时归零)残影同步消失, 前 10 tick 是施法同步缓冲期
            if (this.age > 10 && this.isJxsqEnded(entity)) {
                this.remove();
            }
        } else {
            this.remove();
        }
    }

    // 判断聚形散气隐身状态是否已结束: 隐身激活时 JXSQ_YINGSHEN_ZT 为 false 且 JXSQ_YINGSHEN_TIMER > 10;
    // 主动攻击或受击会把计时器清零, 自然到期时状态位翻为 true
    private boolean isJxsqEnded(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        LocalPlayerPatch patch =
                EpicFightCapabilities.getEntityPatch(player, LocalPlayerPatch.class);
        if (patch == null) {
            return false;
        }
        SkillContainer shenFa = patch.getSkill(WukongSkillSlots.SHENFA_SKILL_SLOT);
        if (shenFa == null) {
            return false;
        }
        SkillDataManager manager = shenFa.getDataManager();
        Integer timer = manager.getDataValue(WukongSkillDataKeys.JXSQ_YINGSHEN_TIMER.get());
        Boolean zt = manager.getDataValue(WukongSkillDataKeys.JXSQ_YINGSHEN_ZT.get());
        if (timer == null || zt == null) {
            return false;
        }
        // 刚施法的一瞬间计时器可能还未同步到满值, 用 ZT 为准
        return zt || timer <= 10;
    }

    // 使用自定义的受光且不剔除渲染类型
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderTypeN.PARTICLE_SHEET_LIT_NO_CULL; // 自定义的渲染类型
    }

    // 定身残影粒子的提供器: 从 xSpeed 中解码实体 ID, 校验存活后创建残影粒子
    @OnlyIn(Dist.CLIENT)
    public static class DangerParticleProvider implements ParticleProvider<SimpleParticleType> {
        // 粒子动画帧(SpriteSet), 用于驱动残影贴图
        private final SpriteSet spriteSet;

        // 构造提供器并持有 SpriteSet
        public DangerParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        // 解码 xSpeed 中的实体 ID, 校验实体为存活活体后创建定身残影粒子
        @Override
        public Particle createParticle(
                SimpleParticleType typeIn,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed) {
            Entity entity = level.getEntity((int) Double.doubleToLongBits(xSpeed));
            if (entity != null && entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                if (livingEntity.isRemoved()) {
                    return null;
                }
                return new DingEntityAfterImageParticle(
                        level, x, y, z, this.spriteSet, entity.getId());
            }
            return null;
        }
    }
}
