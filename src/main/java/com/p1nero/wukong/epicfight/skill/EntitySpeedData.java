package com.p1nero.wukong.epicfight.skill;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntitySpeedData {
    private static final Map<UUID, Double> entitySpeedMap = new HashMap<>();

    public static double getOriginalSpeed(LivingEntity entity) {
        var movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        return entitySpeedMap.getOrDefault(entity.getUUID(), movementSpeed == null ? 0.0D : movementSpeed.getBaseValue());
    }

    public static void saveOriginalSpeed(LivingEntity entity) {
        var movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null && !entitySpeedMap.containsKey(entity.getUUID())) {
            entitySpeedMap.put(entity.getUUID(), movementSpeed.getBaseValue());
            movementSpeed.setBaseValue(0.0D);
        }
    }

    public static void restoreOriginalSpeed(LivingEntity entity) {
        Double originalSpeed = entitySpeedMap.remove(entity.getUUID());
        var movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (originalSpeed != null && movementSpeed != null) {
            movementSpeed.setBaseValue(originalSpeed);
        }
    }
}
