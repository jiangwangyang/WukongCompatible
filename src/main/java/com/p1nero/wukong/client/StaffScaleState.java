package com.p1nero.wukong.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

// 棍子渲染缩放/位移状态的客户端缓存, 按物品栈实例记录;
// 用于替代原先写入物品 NBT 的做法: 单人局域连接下物品栈对象在服务端与客户端之间共享,
// 写物品 NBT 会让服务端每 tick 误判装备变化并重建持棍生物的战斗 AI (表现为 Boss 循环释放开场招)
public class StaffScaleState {
    // 每个物品栈的变换状态: 索引 0~2 为缩放 xyz, 3~5 为位移 xyz; 无记录表示不缩放不位移
    private static final Map<ItemStack, float[]> STATES = new WeakHashMap<>();

    // 写入指定物品栈的缩放/位移参数(由动画事件在播放期间每帧调用)
    public static void set(
            ItemStack stack, float sx, float sy, float sz, float tx, float ty, float tz) {
        STATES.put(stack, new float[] {sx, sy, sz, tx, ty, tz});
    }

    // 清除指定物品栈的变换状态(动画首尾帧与结束时调用)
    public static void reset(ItemStack stack) {
        STATES.remove(stack);
    }

    // 渲染前应用已记录的缩放/位移变换, 无记录则不处理
    public static void apply(ItemStack stack, PoseStack poseStack) {
        float[] state = STATES.get(stack);
        if (state == null) {
            return;
        }
        poseStack.scale(state[0], state[1], state[2]);
        poseStack.translate(state[3], state[4], state[5]);
    }
}
