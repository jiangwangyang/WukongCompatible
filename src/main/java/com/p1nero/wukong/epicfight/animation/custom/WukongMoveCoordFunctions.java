package com.p1nero.wukong.epicfight.animation.custom;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.property.MoveCoordFunctions;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class WukongMoveCoordFunctions extends MoveCoordFunctions {
    public static boolean SJZT = true;

    public static final MoveCoordSetter TRACE_LOCROT_TARGETO =
            (self, entitypatch, transformSheet) -> {
                LivingEntity attackTarget = entitypatch.getTarget();
                TransformSheet transform = self.getCoord().copyAll();
                Keyframe[] keyframes = transform.getKeyframes();
                int startFrame = 0;
                int endFrame = keyframes.length - 1;
                Vec3f keyLast = keyframes[endFrame].transform().translation();
                Vec3 pos = ((LivingEntity) entitypatch.getOriginal()).position();
                if (attackTarget != null && !isFakeWukong(attackTarget)) {
                    Vec3 targetpos = attackTarget.position();
                    Vec3 toTarget = targetpos.subtract(pos);
                    float horizontalDistance =
                            Math.max(
                                    (float) toTarget.horizontalDistance()
                                            - (attackTarget.getBbWidth()
                                                            + ((LivingEntity)
                                                                            entitypatch
                                                                                    .getOriginal())
                                                                    .getBbWidth())
                                                    * 0.75F,
                                    0.0F);
                    Vec3f worldPosition = new Vec3f(keyLast.x, 0.0F, -horizontalDistance);
                    float scale = Math.min(worldPosition.length() / keyLast.length(), 2.0F);
                    float yRot = (float) MathUtils.getYRotOfVector(toTarget);
                    float clampedYRot =
                            MathUtils.rotlerp(
                                    entitypatch.getYRot(), yRot, entitypatch.getYRotLimit());
                    entitypatch.setYRot(clampedYRot);
                    for (int i = startFrame; i <= endFrame; ++i) {
                        Vec3f translation = keyframes[i].transform().translation();
                        if (translation.z < 0.0F) {
                            translation.z *= scale;
                        }
                    }
                    transformSheet.readFrom(transform);
                } else {
                    attackTarget = findClosestEnemyPosition(entitypatch);
                    if (attackTarget != null) {
                        Vec3 targetpos = attackTarget.position();
                        Vec3 toTarget = targetpos.subtract(pos);
                        float horizontalDistance =
                                Math.max(
                                        (float) toTarget.horizontalDistance()
                                                - (attackTarget.getBbWidth()
                                                                + ((LivingEntity)
                                                                                entitypatch
                                                                                        .getOriginal())
                                                                        .getBbWidth())
                                                        * 0.75F,
                                        0.0F);
                        Vec3f worldPosition = new Vec3f(keyLast.x, 0F, -horizontalDistance);
                        float scale = Math.min(worldPosition.length() / keyLast.length(), 2.0F);
                        float yRot = (float) MathUtils.getYRotOfVector(toTarget);
                        float clampedYRot =
                                MathUtils.rotlerp(
                                        entitypatch.getYRot(), yRot, entitypatch.getYRotLimit());
                        entitypatch.setYRot(clampedYRot);
                        for (int i = startFrame; i <= endFrame; ++i) {
                            Vec3f translation = keyframes[i].transform().translation();
                            if (translation.z < 0.0F) {
                                translation.z *= scale;
                            }
                        }
                        transformSheet.readFrom(transform);
                    } else {
                        transformSheet.readFrom(self.getCoord().copyAll());
                    }
                }
                //
            };
    public static final double MAX_DASH_DISTANCE = 6.0D;
    public static final float DASH_WINDOW_END = 0.6F;

    // 旧版 Epic Fight TRACE_DEST_LOCATION 的等价实现: 把位移表重写为绝对世界坐标, 从动画起始点滑步到目标身上, 供 WORLD_COORD 读取器消费
    public static final MoveCoordSetter TRACE_TARGET_DASH =
            (self, entitypatch, destSheet) -> {
                LivingEntity attackTarget = entitypatch.getTarget();
                if (attackTarget == null && entitypatch.getOriginal() instanceof Player player) {
                    LivingEntity closestMonster = null;
                    double closestDistance = Double.MAX_VALUE;
                    List<LivingEntity> nearbyEntities =
                            player.level()
                                    .getEntitiesOfClass(
                                            LivingEntity.class,
                                            player.getBoundingBox().inflate(10.0D),
                                            entity ->
                                                    entity instanceof Monster
                                                            && entity.isAlive()
                                                            && !isFakeWukong(entity));
                    for (LivingEntity entity : nearbyEntities) {
                        double distance = calculateDistance(player.position(), entity.position());
                        if (distance < closestDistance) {
                            closestMonster = entity;
                            closestDistance = distance;
                        }
                    }
                    attackTarget = closestMonster;
                }
                if (attackTarget == null) {
                    destSheet.readFrom(self.getCoord().copyAll());
                    return;
                }
                Vec3 startPos =
                        entitypatch
                                .getAnimator()
                                .getVariables()
                                .getOrDefault(
                                        ActionAnimation.BEGINNING_LOCATION,
                                        self.getRealAnimation());
                if (startPos == null) {
                    startPos = ((LivingEntity) entitypatch.getOriginal()).position();
                }
                Vec3 toTarget = attackTarget.position().subtract(startPos);
                Vec3 horizontalToTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);
                double horizontalDistance = horizontalToTarget.horizontalDistance();
                Vec3 dashDirection =
                        horizontalDistance > 0.01D ? horizontalToTarget.normalize() : Vec3.ZERO;
                float stopDistance =
                        (attackTarget.getBbWidth()
                                        + ((LivingEntity) entitypatch.getOriginal()).getBbWidth())
                                * 0.75F;
                double dashDistance =
                        Math.min(
                                Math.max(horizontalDistance - stopDistance, 0.0D),
                                MAX_DASH_DISTANCE);
                if (horizontalDistance > 0.01D) {
                    float yRot = (float) MathUtils.getYRotOfVector(toTarget);
                    float clampedYRot =
                            MathUtils.rotlerp(
                                    entitypatch.getYRot(), yRot, entitypatch.getYRotLimit());
                    entitypatch.setYRot(clampedYRot);
                }
                TransformSheet transform = self.getCoord().copyAll();
                for (Keyframe keyframe : transform.getKeyframes()) {
                    float progress = Math.min(keyframe.time() / DASH_WINDOW_END, 1.0F);
                    Vec3 worldPosition = startPos.add(dashDirection.scale(dashDistance * progress));
                    Vec3f translation = keyframe.transform().translation();
                    translation.x = (float) worldPosition.x;
                    translation.y = (float) worldPosition.y;
                    translation.z = (float) worldPosition.z;
                }
                destSheet.readFrom(transform);
            };

    private static LivingEntity findClosestEnemyPosition(LivingEntityPatch<?> entitypatch) {
        if (entitypatch instanceof LocalPlayerPatch) {
            LocalPlayerPatch localPlayerPatch = (LocalPlayerPatch) entitypatch;
            LocalPlayer Player = localPlayerPatch.getOriginal();
            Vec3 position = Player.position();
            LivingEntity closestMonster = null;
            double closestDistance = Double.MAX_VALUE;
            List<LivingEntity> nearbyEntities =
                    Player.level()
                            .getEntitiesOfClass(
                                    LivingEntity.class,
                                    Player.getBoundingBox().inflate(3),
                                    entity ->
                                            entity != Player
                                                    && entity.isAlive()
                                                    && !isFakeWukong(entity));
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
        if (SJZT) {
            if (entitypatch instanceof LocalPlayerPatch localPlayerPatch) {
                LocalPlayer Player = localPlayerPatch.getOriginal();
                Player.setXRot(-7);
            }
            SJZT = false;
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
