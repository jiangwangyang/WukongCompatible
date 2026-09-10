package com.p1nero.wukong.epicfight.skill.custom.avatar;

import com.p1nero.wukong.entity.FakeWukongEntity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 假悟空实体注册表: 维护玩家与其召唤出的假悟空实体ID之间的映射, 用于追踪与批量清理
public class FakeWukongEntityRegistry {
    // 玩家 -> 其召唤的假悟空实体ID列表
    private static final Map<ServerPlayer, List<Integer>> playerSummonedEntities = new HashMap<>();

    // 记录某玩家新召唤出的假悟空实体ID
    public static void registerFakeWukongEntity(ServerPlayer player, int entityId) {
        playerSummonedEntities.computeIfAbsent(player, k -> new ArrayList<>()).add(entityId);
    }

    // 获取某玩家已召唤的所有假悟空实体ID(不存在时返回空列表)
    public static List<Integer> getFakeWukongEntityIds(ServerPlayer player) {
        return playerSummonedEntities.getOrDefault(player, new ArrayList<>());
    }

    // 获取某玩家召唤的第一个假悟空实体ID, 无则返回null
    public static Integer getFirstFakeWukongEntityId(ServerPlayer player) {
        List<Integer> entityIds = playerSummonedEntities.get(player);
        return entityIds != null && !entityIds.isEmpty() ? entityIds.get(0) : null;
    }

    // 若指定实体ID不属于该玩家(或玩家无记录), 则清除该玩家的映射记录
    public static void clearFakeWukongEntityIdsIfNotExist(ServerPlayer player, int entityId) {
        List<Integer> entityIds = playerSummonedEntities.get(player);
        if (entityIds == null || !entityIds.contains(entityId)) {
            playerSummonedEntities.remove(player);
        }
    }

    // 击杀并清除某玩家召唤的所有假悟空实体(若存在)
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
