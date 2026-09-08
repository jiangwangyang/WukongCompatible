package com.p1nero.wukong.effects;

import com.p1nero.wukong.epicfight.skill.custom.fashu.ShenfaJuxingsanqiSkill;
import com.p1nero.wukong.epicfight.WukongSkillSlots;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.lizi.ParticleRenderTypeN;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;


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
        this.lifetime = ShenfaJuxingsanqiSkill.MAX_TIME; // 与聚形散气隐身时长保持一致(200 tick = 10s)
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
            // 聚形散气结束时(主动攻击/受击/倒计时归零)残影同步消失, 前 10 tick 是施法同步缓冲期
            if (this.age > 10 && this.isJxsqEnded(entity)) {
                this.remove();
            }
        } else {
            this.remove();
        }
    }

    /**
     * 判断聚形散气隐身状态是否已结束。
     * 隐身激活时: JXSQ_YINGSHEN_ZT == false 且 JXSQ_YINGSHEN_TIMER > 10;
     * 主动攻击或受击会把计时器清零, 自然到期时状态位翻为 true。
     */
    private boolean isJxsqEnded(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        LocalPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, LocalPlayerPatch.class);
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


    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderTypeN.PARTICLE_SHEET_LIT_NO_CULL;  // 自定义的渲染类型
    }
}
