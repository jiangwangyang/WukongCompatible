package com.p1nero.wukong.client.event;

import com.p1nero.wukong.WukongMoveset;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 立棍蓄力视野(FOV)控制器: 客户端每 tick 插值调整视野, 按拉近-保持-还原的节奏过渡
@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, value = Dist.CLIENT)
public final class PillarFovController {
    // 过渡开始前的原始 FOV, 负值表示未记录
    private static int originalFov = -1;
    // 过渡目标 FOV
    private static int targetFov;
    // 过渡已经过的 tick 数
    private static int elapsedTicks;
    // 单向过渡(拉近或还原)所需的 tick 数
    private static int transitionTicks;
    // 到达目标 FOV 后保持的 tick 数
    private static int holdTicks;
    // 是否处于过渡中
    private static boolean active;

    // 工具类, 禁止实例化
    private PillarFovController() {}

    // 开始 FOV 过渡: 记录原始 FOV, 计算目标 FOV 与过渡/保持时长
    public static void start(float increaseAmount, int legacyTransitionTicks, int repeatTimes) {
        Minecraft minecraft = Minecraft.getInstance();
        if (originalFov < 0) {
            originalFov = minecraft.options.fov().get();
        }

        targetFov = Math.min(Math.round(originalFov + increaseAmount), 97);
        // 旧实现每 10ms 推进一帧, 此处换算为 20 TPS 的 tick 数
        transitionTicks = Math.max(1, (int) Math.ceil(legacyTransitionTicks / 5.0D));
        holdTicks = transitionTicks * Math.max(0, repeatTimes - 1);
        elapsedTicks = 0;
        active = true;
    }

    // 客户端每 tick 推进 FOV 过渡: 拉近-保持-还原, 玩家不存在时直接还原
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            restore(minecraft);
            return;
        }

        elapsedTicks++;
        int restoreStart = transitionTicks + holdTicks;
        int totalTicks = restoreStart + transitionTicks;
        float fov;
        if (elapsedTicks <= transitionTicks) {
            fov = lerp(originalFov, targetFov, (float) elapsedTicks / transitionTicks);
        } else if (elapsedTicks <= restoreStart) {
            fov = targetFov;
        } else if (elapsedTicks <= totalTicks) {
            fov =
                    lerp(
                            targetFov,
                            originalFov,
                            (float) (elapsedTicks - restoreStart) / transitionTicks);
        } else {
            restore(minecraft);
            return;
        }
        minecraft.options.fov().set(Math.round(fov));
    }

    // 线性插值, progress 会被限制在 0 到 1 之间
    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * Math.min(Math.max(progress, 0.0F), 1.0F);
    }

    // 恢复原始FOV并重置所有控制状态
    private static void restore(Minecraft minecraft) {
        if (originalFov >= 0) {
            minecraft.options.fov().set(originalFov);
        }
        originalFov = -1;
        active = false;
        elapsedTicks = 0;
    }
}
