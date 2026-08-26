package com.p1nero.wukong.epicfight.skill.custom.avatar;

import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.entity.FakeWukongEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FakeWukongEntityRegistry {
    private static final Map<ServerPlayer, List<Integer>> playerSummonedEntities = new HashMap<>();
    public static void registerFakeWukongEntity(ServerPlayer player, int entityId) {
        playerSummonedEntities
                .computeIfAbsent(player, k -> new ArrayList<>())
                .add(entityId);
    }
    public static List<Integer> getFakeWukongEntityIds(ServerPlayer player) {
        return playerSummonedEntities.getOrDefault(player, new ArrayList<>());
    }

    public static Integer getFirstFakeWukongEntityId(ServerPlayer player) {
        List<Integer> entityIds = playerSummonedEntities.get(player);
        return entityIds != null && !entityIds.isEmpty() ? entityIds.get(0) : null;
    }

    public static void clearFakeWukongEntityIdsIfNotExist(ServerPlayer player, int entityId) {
        List<Integer> entityIds = playerSummonedEntities.get(player);
        if (entityIds == null || !entityIds.contains(entityId)) {
            playerSummonedEntities.remove(player);
        }
    }
    public static void killFakeWukongEntitiesIfExist(ServerPlayer player) {
        List<Integer> entityIds = playerSummonedEntities.get(player);
        if (entityIds != null && !entityIds.isEmpty()) {
            for (int entityId : entityIds) {
                Entity fakeWukongEntity = player.level().getEntity(entityId);
                if (fakeWukongEntity instanceof FakeWukongEntity) {
                    fakeWukongEntity.remove(Entity.RemovalReason.KILLED);

                }
            }

            playerSummonedEntities.remove(player);

        }
    }
}