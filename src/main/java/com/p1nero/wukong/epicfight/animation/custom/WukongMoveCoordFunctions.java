package com.p1nero.wukong.epicfight.animation.custom;


import com.p1nero.wukong.WukongMoveset;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.property.MoveCoordFunctions;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class WukongMoveCoordFunctions extends MoveCoordFunctions {
    public static boolean SJZT=true;


    public static final MoveCoordSetter TRACE_LOCROT_TARGETO = (self, entitypatch, transformSheet) -> {
        LivingEntity attackTarget = entitypatch.getTarget();
        TransformSheet transform = self.getCoord().copyAll();
        Keyframe[] keyframes = transform.getKeyframes();
        int startFrame = 0;
        int endFrame = keyframes.length - 1;
        Vec3f keyLast = keyframes[endFrame].transform().translation();
        Vec3 pos = ((LivingEntity)entitypatch.getOriginal()).position();
        if (attackTarget != null && !isFakeWukong(attackTarget)) {
            Vec3 targetpos = attackTarget.position();
            Vec3 toTarget = targetpos.subtract(pos);
            float horizontalDistance = Math.max((float)toTarget.horizontalDistance() - (attackTarget.getBbWidth() + ((LivingEntity)entitypatch.getOriginal()).getBbWidth()) * 0.75F, 0.0F);
            Vec3f worldPosition = new Vec3f(keyLast.x, 0.0F, -horizontalDistance);
            float scale = Math.min(worldPosition.length() / keyLast.length(), 2.0F);
            float yRot = (float)MathUtils.getYRotOfVector(toTarget);
            float clampedYRot = MathUtils.rotlerp(entitypatch.getYRot(), yRot, entitypatch.getYRotLimit());
            entitypatch.setYRot(clampedYRot);
            for(int i = startFrame; i <= endFrame; ++i) {
                Vec3f translation = keyframes[i].transform().translation();
                if (translation.z < 0.0F) {
                    translation.z *= scale;
                }
            }
           // isSjzt(entitypatch);
            transformSheet.readFrom(transform);
        } else {
         //   WukongMoveset.LOGGER.info("TRACE_LOCROT_TARGET: {}","走备用方案");
            attackTarget = findClosestEnemyPosition(entitypatch);
            if (attackTarget != null) {
                    Vec3 targetpos = attackTarget.position();
                    Vec3 toTarget = targetpos.subtract(pos);
                    float horizontalDistance = Math.max((float)toTarget.horizontalDistance() - (attackTarget.getBbWidth() + ((LivingEntity)entitypatch.getOriginal()).getBbWidth()) * 0.75F, 0.0F);
                    Vec3f worldPosition = new Vec3f(keyLast.x, 0F, -horizontalDistance);
                    float scale = Math.min(worldPosition.length() / keyLast.length(), 2.0F);
                    float yRot = (float)MathUtils.getYRotOfVector(toTarget);
                    float clampedYRot = MathUtils.rotlerp(entitypatch.getYRot(), yRot, entitypatch.getYRotLimit());
                    entitypatch.setYRot(clampedYRot);
                    for(int i = startFrame; i <= endFrame; ++i) {
                        Vec3f translation = keyframes[i].transform().translation();
                        if (translation.z < 0.0F) {
                            translation.z *= scale;
                        }
                    }
                   // isSjzt(entitypatch);
                    transformSheet.readFrom(transform);
            } else {
                transformSheet.readFrom(self.getCoord().copyAll());
            }

        }
      //
    };
    private static LivingEntity findClosestEnemyPosition(LivingEntityPatch<?> entitypatch) {
        if (entitypatch instanceof LocalPlayerPatch) {
            LocalPlayerPatch localPlayerPatch = (LocalPlayerPatch) entitypatch;
            LocalPlayer Player = localPlayerPatch.getOriginal();
            Vec3 position = Player.position();
            LivingEntity closestMonster = null;
            double closestDistance = Double.MAX_VALUE;
            List<LivingEntity> nearbyEntities = Player.level().getEntitiesOfClass(LivingEntity.class, Player.getBoundingBox().inflate(3), entity -> entity != Player && entity.isAlive() && !isFakeWukong(entity));
            for (LivingEntity entity : nearbyEntities) {
                double distance = calculateDistance(position, entity.position());
                if (distance < closestDistance) {
                    closestMonster = entity;
                    closestDistance = distance;
                }
            }
            if (closestMonster != null) {
                return closestMonster;
            }
        }
        return null;
    }
    private static void isSjzt(LivingEntityPatch<?> entitypatch) {
        if (SJZT){
            if (entitypatch instanceof LocalPlayerPatch localPlayerPatch) {
                LocalPlayer Player = localPlayerPatch.getOriginal();
                Player.setXRot(-7);
            }
            SJZT=false;
        }
    }
    private static double calculateDistance(Vec3 position, Vec3 monsterPos) {
        double deltaX = monsterPos.x - position.x;
        double deltaY = monsterPos.y - position.y;
        double deltaZ = monsterPos.z - position.z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ; // 使用平方距离避免开根号
    }
    private static boolean isFakeWukong(LivingEntity entity) {
        return entity.getType().toString().equals("entity.wukong.fake_wukong_entity");
    }
    public static void reseTSjzt() {
        SJZT = true;
    }

}
