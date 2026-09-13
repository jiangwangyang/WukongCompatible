package com.p1nero.wukong.epicfight.skill.custom.wukong;

import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.client.WuKongSounds;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.WukongSkills;
import com.p1nero.wukong.network.PacketHandler;
import com.p1nero.wukong.network.PacketRelay;
import com.p1nero.wukong.network.packet.client.AddEntityAfterImageParticle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.MovementDirection;
import yesman.epicfight.client.input.InputUtils;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.network.client.CPSkillRequest;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.List;
import java.util.UUID;

// 悟空闪避技能: 实现闪避动画轮播与完美闪避判定, 完美闪避时回棍势并播放音效与残影
public class WukongDodgeSkill extends Skill {
    // 本技能事件监听器的唯一标识
    private static final UUID EVENT_UUID = UUID.fromString("d2d011cc-f30f-11ed-a05b-0242ac114515");
    // 闪避连段归零的重置时长(单位: tick)
    public static final int RESET_TICKS = 100;
    // 闪避动画表: 第一维为1~3段与完美闪避, 第二维为前/后/左/右方向
    protected final StaticAnimationProvider[][] animations;

    // 创建闪避技能构建器, 设为闪避分类/单次激活/无资源消耗
    public static WukongDodgeSkill.Builder createDodgeBuilder() {
        return (new WukongDodgeSkill.Builder())
                .setCategory(SkillCategories.DODGE)
                .setActivateType(ActivateType.ONE_SHOT)
                .setResource(Resource.NONE);
    }

    // 构造方法, 保存闪避动画表
    public WukongDodgeSkill(WukongDodgeSkill.Builder builder) {
        super(builder);
        animations = builder.animations;
    }

    // 注册完美闪避(DODGE_SUCCESS)事件监听: 标记完美闪避状态, 播放音效与残影,
    // 回复棍势, 并播放对应方向的完美闪避动画
    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.DODGE_SUCCESS_EVENT,
                        EVENT_UUID,
                        (event -> {
                            Player player = event.getPlayerPatch().getOriginal();
                            if (!container
                                    .getDataManager()
                                    .getDataValue(WukongSkillDataKeys.DODGE_PLAYED.get())) {
                                // 标记完美闪避, 供蓄力保留棍势判定使用
                                player.getCapability(WKCapabilityProvider.WK_PLAYER)
                                        .ifPresent(wkPlayer -> wkPlayer.setPerfectDodge(true));
                                event.getPlayerPatch()
                                        .playSound(WuKongSounds.PERFECT_DODGE.get(), 1, 1);
                                if (player.level() instanceof ServerLevel) {
                                    PacketRelay.sendToAll(
                                            PacketHandler.INSTANCE,
                                            new AddEntityAfterImageParticle(
                                                    player.getId())); // 手动发包加残影
                                }
                                SkillContainer weaponInnateContainer =
                                        event.getPlayerPatch().getSkill(SkillSlots.WEAPON_INNATE);
                                if (weaponInnateContainer != null
                                        && !weaponInnateContainer.isEmpty()) {
                                    // 完美闪避获得30棍势
                                    WukongSkills.gainResource(weaponInnateContainer, 30.0F);
                                }
                                container
                                        .getDataManager()
                                        .setData(WukongSkillDataKeys.DODGE_PLAYED.get(), true);
                                int direction =
                                        Mth.clamp(
                                                container
                                                        .getDataManager()
                                                        .getDataValue(
                                                                WukongSkillDataKeys.DIRECTION
                                                                        .get()),
                                                0,
                                                3);
                                event.getPlayerPatch()
                                        .playAnimationSynchronized(
                                                this.animations[3][direction].get(), 0.0F);
                            }
                        }));
    }

    // 移除本技能注册的事件监听
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        container
                .getExecutor()
                .getEventListener()
                .removeListener(PlayerEventListener.EventType.DODGE_SUCCESS_EVENT, EVENT_UUID);
    }

    // 客户端构造闪避执行请求: 在EpicFight更新输入状态前捕获原版移动按键,
    // 计算闪避方向(垂直/水平分量)与闪避朝向角度, 写入请求包后交由网络层发送
    @Override
    @OnlyIn(Dist.CLIENT)
    public Object getExecutionPacket(SkillContainer container, FriendlyByteBuf args) {
        LocalPlayerPatch executer = container.getClientExecutor();
        Input input = executer.getOriginal().input;
        Minecraft minecraft = Minecraft.getInstance();

        // 在EpicFight更新输入状态前捕获原版物理移动按键
        boolean forward = minecraft.options.keyUp.isDown();
        boolean backward = minecraft.options.keyDown.isDown();
        boolean left = minecraft.options.keyLeft.isDown();
        boolean right = minecraft.options.keyRight.isDown();

        float pulse =
                Mth.clamp(
                        0.3F + EnchantmentHelper.getSneakingSpeedBonus(executer.getOriginal()),
                        0.0F,
                        1.0F);
        InputUtils.sneakingTick(executer.getOriginal(), false, pulse);

        int vertic = forward == backward ? 0 : (forward ? 1 : -1);
        int horizon = left == right ? 0 : (left ? 1 : -1);
        if (!forward && !backward && !left && !right) {
            MovementDirection direction =
                    MovementDirection.fromInputState(InputManager.getInputState(input));
            vertic = direction.vertical();
            horizon = direction.horizontal();
        }

        float yRot = EpicFightCameraAPI.getInstance().getForwardYRot();
        float degree =
                Mth.wrapDegrees(
                        (float) (-(90 * horizon * (1 - Math.abs(vertic)) + 45 * vertic * horizon))
                                + yRot);
        CPSkillRequest packet = new CPSkillRequest(container.getSlot());
        packet.getBuffer().writeInt(vertic < 0 ? 1 : 0);
        packet.getBuffer().writeFloat(degree);
        return packet;
    }

    // 向技能提示参数列表中添加消耗值
    @OnlyIn(Dist.CLIENT)
    public List<Object> getTooltipArgsOfScreen(List<Object> list) {
        list.add(ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(this.consumption));
        return list;
    }

    // 服务端执行闪避: 轮播闪避动画(1~3段循环), 记录完美闪避方向并同步模型朝向,
    // 闪避不打断普攻连段, 连段仅受普攻间隔超时重置约束
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        super.executeOnServer(container, args);
        ServerPlayerPatch executer = container.getServerExecutor();
        int i = Mth.clamp(args.readInt(), 0, 3);
        float yaw = args.readFloat();
        SkillDataManager dataManager = container.getDataManager();
        dataManager.setData(WukongSkillDataKeys.DODGE_PLAYED.get(), false);
        int count = Mth.clamp(dataManager.getDataValue(WukongSkillDataKeys.COUNT.get()), 0, 2);
        executer.playAnimationSynchronized(this.animations[count][i].get(), 0.0F); // 轮播
        executer.playSound(EpicFightSounds.ROLL.get(), 1.0F, 1.0F);
        dataManager.setDataSync(WukongSkillDataKeys.DIRECTION.get(), i); // 完美闪避方向
        // 每次闪避都刷新归段计时, 避免单次闪避后段数计数残留
        dataManager.setDataSync(WukongSkillDataKeys.RESET_TIMER.get(), RESET_TICKS);
        dataManager.setDataSync(WukongSkillDataKeys.COUNT.get(), ++count % 3);

        executer.setModelYRot(yaw, true);
    }

    // 每tick更新: 闪避连段计时结束后复原为第一段
    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager manager = container.getDataManager();
        if (manager.hasData(WukongSkillDataKeys.RESET_TIMER.get())
                && manager.getDataValue(WukongSkillDataKeys.RESET_TIMER.get()) > 0) {
            manager.setData(
                    WukongSkillDataKeys.RESET_TIMER.get(),
                    manager.getDataValue(WukongSkillDataKeys.RESET_TIMER.get()) - 1);
            if (manager.getDataValue(WukongSkillDataKeys.RESET_TIMER.get()) == 1
                    && manager.hasData(WukongSkillDataKeys.COUNT.get())) {
                manager.setData(WukongSkillDataKeys.COUNT.get(), 0);
            }
        }
    }

    // 判断当前状态是否可执行闪避(不在空中/水中/攀爬/骑乘且实体状态允许)
    public boolean isExecutableState(PlayerPatch<?> executer) {
        EntityState playerState = executer.getEntityState();
        return !executer.isInAir()
                && playerState.canUseSkill()
                && !executer.getOriginal().isInWater()
                && !executer.getOriginal().onClimbable()
                && executer.getOriginal().getVehicle() == null;
    }

    // 技能构建器, 收集各段与各方向的闪避动画
    public static class Builder extends SkillBuilder<WukongDodgeSkill> {
        protected StaticAnimationProvider[][] animations =
                new StaticAnimationProvider[4][4]; // 第一维为1~3段与完美闪避, 第二维为前/后/左/右

        public Builder() {}

        // 设置技能分类
        public WukongDodgeSkill.Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        // 设置激活类型
        public WukongDodgeSkill.Builder setActivateType(Skill.ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        // 设置消耗资源
        public WukongDodgeSkill.Builder setResource(Skill.Resource resource) {
            this.resource = resource;
            return this;
        }

        // 设置创造模式标签页
        public WukongDodgeSkill.Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }

        // 设置1段闪避动画(前/后/左/右)
        public WukongDodgeSkill.Builder setAnimations1(StaticAnimationProvider... animations) {
            this.animations[0] = animations;
            return this;
        }

        // 设置2段闪避动画(前/后/左/右)
        public WukongDodgeSkill.Builder setAnimations2(StaticAnimationProvider... animations) {
            this.animations[1] = animations;
            return this;
        }

        // 设置3段闪避动画(前/后/左/右)
        public WukongDodgeSkill.Builder setAnimations3(StaticAnimationProvider... animations) {
            this.animations[2] = animations;
            return this;
        }

        // 设置完美闪避动画(前/后/左/右)
        public WukongDodgeSkill.Builder setPerfectAnimations(
                StaticAnimationProvider... animations) {
            this.animations[3] = animations;
            return this;
        }
    }
}
