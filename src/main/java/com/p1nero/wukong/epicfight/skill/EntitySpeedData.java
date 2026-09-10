package com.p1nero.wukong.epicfight.skill;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 实体移速数据管理类: 临时保存并清零/恢复实体的基础移动速度, 配合定身等控制效果使用
public class EntitySpeedData {
    // 实体UUID -> 原始基础移动速度, 用于定身时清零并在解除后恢复
    private static final Map<UUID, Double> entitySpeedMap = new HashMap<>();

    // 获取实体原始基础移动速度; 未记录过时回退到实体当前基础值, 属性缺失时返回0
    public static double getOriginalSpeed(LivingEntity entity) {
        var movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        return entitySpeedMap.getOrDefault(
                entity.getUUID(), movementSpeed == null ? 0.0D : movementSpeed.getBaseValue());
    }

    // 保存实体原始基础移动速度(仅首次记录), 并立即将其基础移动速度清零
    public static void saveOriginalSpeed(LivingEntity entity) {
        var movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && !entitySpeedMap.containsKey(entity.getUUID())) {
            entitySpeedMap.put(entity.getUUID(), movementSpeed.getBaseValue());
            movementSpeed.setBaseValue(0.0D);
        }
    }

    // 从记录中恢复实体的原始基础移动速度, 并清除该实体的记录
    public static void restoreOriginalSpeed(LivingEntity entity) {
        Double originalSpeed = entitySpeedMap.remove(entity.getUUID());
        var movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (originalSpeed != null && movementSpeed != null) {
            movementSpeed.setBaseValue(originalSpeed);
        }
    }
}
