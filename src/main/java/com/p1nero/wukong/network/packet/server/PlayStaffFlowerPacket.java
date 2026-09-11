package com.p1nero.wukong.network.packet.server;

import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.network.packet.BasePacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

// 根据客户端的需求来播动画: 服务端收到后播放金箍棒花 (staff flower) 动画
public record PlayStaffFlowerPacket(boolean isTwoHand) implements BasePacket {

    @Override
    // 将是否双手持棒标志写入字节缓冲
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isTwoHand);
    }

    // 从字节缓冲读取标志并构造数据包
    public static PlayStaffFlowerPacket decode(FriendlyByteBuf buf) {
        return new PlayStaffFlowerPacket(buf.readBoolean());
    }

    @Override
    // 在服务端播放对应的金箍棒花循环动画
    public void execute(Player player) {
        if (player != null) {

            player.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY)
                    .ifPresent(
                            (entityPatch -> {
                                if (entityPatch instanceof ServerPlayerPatch playerPatch) {
                                    playerPatch.playAnimationSynchronized(
                                            (isTwoHand)
                                                    ? WukongAnimations.STAFF_SPIN_TWO_HAND_LOOP
                                                    : WukongAnimations.STAFF_SPIN_ONE_HAND_LOOP,
                                            0);
                                }
                            }));
        }
    }
}
