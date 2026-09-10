package com.p1nero.wukong.client.event;

import com.p1nero.wukong.WukongMoveset;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, value = Dist.CLIENT)
public final class PillarFovController {
    private static int originalFov = -1;
    private static int targetFov;
    private static int elapsedTicks;
    private static int transitionTicks;
    private static int holdTicks;
    private static boolean active;

    private PillarFovController() {}

    public static void start(float increaseAmount, int legacyTransitionTicks, int repeatTimes) {
        Minecraft minecraft = Minecraft.getInstance();
        if (originalFov < 0) {
            originalFov = minecraft.options.fov().get();
        }

        targetFov = Math.min(Math.round(originalFov + increaseAmount), 97);
        // The old implementation advanced every 10 ms; convert that timing to 20 TPS.
        transitionTicks = Math.max(1, (int) Math.ceil(legacyTransitionTicks / 5.0D));
        holdTicks = transitionTicks * Math.max(0, repeatTimes - 1);
        elapsedTicks = 0;
        active = true;
    }

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
