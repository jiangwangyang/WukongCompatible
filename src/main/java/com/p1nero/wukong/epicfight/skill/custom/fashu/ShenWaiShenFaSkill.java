package com.p1nero.wukong.epicfight.skill.custom.fashu;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.wukong.WukongMoveset;
import com.p1nero.wukong.capability.WKCapabilityProvider;
import com.p1nero.wukong.capability.entity.FakeWukongEntityPatch;
import com.p1nero.wukong.entity.FakeWukongEntity;
import com.p1nero.wukong.epicfight.WukongSkillCategories;
import com.p1nero.wukong.epicfight.animation.WukongAnimations;
import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;
import com.p1nero.wukong.epicfight.skill.WukongSkillDataKeys;
import com.p1nero.wukong.epicfight.skill.custom.avatar.HeavyAttack;
import com.p1nero.wukong.epicfight.weapon.WukongWeaponCategories;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

import java.util.UUID;

// 法术: 身外身法技能, 施放后召唤假悟空分身并同步其攻击动作
public class ShenWaiShenFaSkill extends Skill {
    // 事件监听器注册用唯一UUID
    private static final UUID EVENT_UUID = UUID.fromString("d2d057cc-f30f-11ed-a05b-0282ac114513");
    // 衍生(施法)动画
    protected StaticAnimationProvider deriveAnimation1;

    // 构造身外身法技能, 记录衍生(施法)动画
    public ShenWaiShenFaSkill(Builder builder) {
        super(builder);
        deriveAnimation1 = builder.derive1;
    }

    // 创建技能构建器并设置分类(毫毛)与资源类型
    public static ShenWaiShenFaSkill.Builder create() {
        return new ShenWaiShenFaSkill.Builder()
                .setCategory(WukongSkillCategories.HAO_MAO)
                .setResource(Resource.NONE);
    }

    // 技能初始化: 注册受击与动作事件监听, 实现免自伤与分身动作同步
    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);

        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK,
                        EVENT_UUID,
                        (hurtEvent -> {
                            // 伤害来源为玩家自身或其分身时, 取消该次伤害(避免误伤)
                            if (hurtEvent.getPlayerPatch().getOriginal()
                                            == hurtEvent.getDamageSource().getEntity()
                                    || (hurtEvent.getDamageSource().getEntity()
                                                    instanceof FakeWukongEntity fakeWukongEntity
                                            && fakeWukongEntity.getOwner() != null
                                            && hurtEvent.getPlayerPatch().getOriginal().getId()
                                                    == fakeWukongEntity.getOwner().getId())) {
                                hurtEvent.setCanceled(true);
                                hurtEvent.setResult(AttackResult.ResultType.MISSED);
                            }
                        }),
                        10);
        container
                .getExecutor()
                .getEventListener()
                .addEventListener(
                        PlayerEventListener.EventType.ACTION_EVENT_SERVER,
                        EVENT_UUID,
                        (actionEvent -> {
                            AnimationManager.AnimationAccessor<? extends MainFrameAnimation>
                                    animation = actionEvent.getAnimation();
                            ServerPlayerPatch executor = actionEvent.getPlayerPatch();
                            SkillContainer weaponContainer =
                                    executor.getSkill(SkillSlots.WEAPON_INNATE);
                            if (weaponContainer == null || weaponContainer.isEmpty()) {
                                return;
                            }
                            Skill weaponInnate = weaponContainer.getSkill();

                            // 武器天赋实现重击接口且动作匹配时, 命令距离目标4格内的分身同步播放相同攻击动画
                            if (weaponInnate instanceof HeavyAttack heavyAttacks) {
                                if (animation.equals(WukongAnimations.STAFF_AUTO5)
                                        || isAnimationInList(heavyAttacks, animation)) {
                                    executor.getOriginal()
                                            .getCapability(WKCapabilityProvider.WK_PLAYER)
                                            .ifPresent(
                                                    wkPlayer -> {
                                                        for (int id : wkPlayer.getFakeWukongIds()) {
                                                            if (executor.getOriginal()
                                                                            .level()
                                                                            .getEntity(id)
                                                                    instanceof
                                                                    FakeWukongEntity
                                                                            fakeWukongEntity) {
                                                                if (executor.getTarget() != null
                                                                        && fakeWukongEntity
                                                                                        .distanceTo(
                                                                                                executor
                                                                                                        .getTarget())
                                                                                < 4) {
                                                                    fakeWukongEntity
                                                                            .getLookControl()
                                                                            .setLookAt(
                                                                                    executor
                                                                                            .getTarget());
                                                                    FakeWukongEntityPatch
                                                                            fakeWukongEntityPatch =
                                                                                    EpicFightCapabilities
                                                                                            .getEntityPatch(
                                                                                                    fakeWukongEntity,
                                                                                                    FakeWukongEntityPatch
                                                                                                            .class);
                                                                    if (fakeWukongEntityPatch
                                                                            != null) {
                                                                        fakeWukongEntityPatch
                                                                                .playAnimationSynchronized(
                                                                                        animation,
                                                                                        0.15F);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    });
                                }
                            }
                        }));
    }

    // 判断指定动画是否在该重击的动画列表中
    private boolean isAnimationInList(
            HeavyAttack animations,
            AnimationManager.AnimationAccessor<? extends MainFrameAnimation> animation) {
        for (StaticAnimationProvider animationz : animations.getHeavyAttacks()) {
            if (animationz.get().equals(animation)) {
                return true;
            }
        }
        return false;
    }

    // 技能移除: 注销受击事件监听器
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
        container
                .getExecutor()
                .getEventListener()
                .removeListener(PlayerEventListener.EventType.TAKE_DAMAGE_EVENT_ATTACK, EVENT_UUID);
    }

    // 服务端执行: 冷却完毕时播放施法动画并开启冷却, 否则提示冷却中
    @Override
    public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
        ServerPlayerPatch executor = container.getServerExecutor();
        super.executeOnServer(container, args);
        SkillDataManager dataManager = container.getDataManager();
        ServerPlayer player = executor.getOriginal();
        if (dataManager.getDataValue(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get())) {
            executor.playAnimationSynchronized(deriveAnimation1.get(), 0F);
            dataManager.setDataSync(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get(), false);
            dataManager.setDataSync(WukongSkillDataKeys.SWSF_COOLING_TIMER.get(), 2400); // 800
        } else {
            player.sendSystemMessage(Component.literal("Shenwaishenfa is cooling down."));
        }
    }

    // 每帧更新: 递减冷却计时, 归零时复位可释放状态
    @Override
    public void updateContainer(SkillContainer container) {
        super.updateContainer(container);
        SkillDataManager dataManager = container.getDataManager();
        if (container.getExecutor().isLogicalClient()) {

        } else {
            ServerPlayerPatch serverPlayerPatch = ((ServerPlayerPatch) container.getExecutor());
            ServerPlayer serverPlayer = serverPlayerPatch.getOriginal();

            if (!dataManager.getDataValue(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get())) {
                dataManager.setDataSync(
                        WukongSkillDataKeys.SWSF_COOLING_TIMER.get(),
                        Math.max(
                                dataManager.getDataValue(
                                                WukongSkillDataKeys.SWSF_COOLING_TIMER.get())
                                        - 1,
                                0));
                if (dataManager.getDataValue(WukongSkillDataKeys.SWSF_COOLING_TIMER.get()) == 0)
                    dataManager.setDataSync(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get(), true);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    // 根据技能状态绘制自定义技能图标与冷却显示; 完全重写Epic Fight默认绘制, 战斗模式HUD仅显示此自定义画面
    public void drawOnGui(
            BattleModeGui gui,
            SkillContainer container,
            GuiGraphics guiGraphics,
            float x,
            float y,
            float partialTick) {
        Window sr = Minecraft.getInstance().getWindow();
        int width = sr.getGuiScaledWidth();
        int height = sr.getGuiScaledHeight();
        int alpha = 128; // 50% 透明度
        Vec2i pos = ClientConfig.getWeaponInnatePosition(width, height);
        ResourceLocation styleTexture =
                ResourceLocation.fromNamespaceAndPath(
                        WukongMoveset.MOD_ID, "textures/gui/skills/spell_swsf.png");
        if (container
                .getDataManager()
                .getDataValue(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get())) {
            alpha = 255;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        guiGraphics.blit(styleTexture, pos.x - 42, pos.y - 4, 20, 20, 0.0f, 0f, 1, 1, 1, 1);
        if (!container
                .getDataManager()
                .getDataValue(WukongSkillDataKeys.SWSF_COOLING_ATTACK.get())) {
            float second =
                    (container
                                    .getDataManager()
                                    .getDataValue(WukongSkillDataKeys.SWSF_COOLING_TIMER.get())
                            / 20.0F);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 255.0f);
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    String.format("%.1f", second),
                    pos.x
                            - 42
                            + (20
                                            - Minecraft.getInstance()
                                                    .font
                                                    .width(String.format("%.1f", second)))
                                    / 2
                            + 1,
                    pos.y - 4 + (20 - Minecraft.getInstance().font.lineHeight) / 2 + 1,
                    16777215 // 文本颜色(白色)
                    );
        }
    }

    // 仅当装备合法武器时绘制该技能
    @Override
    public boolean shouldDraw(SkillContainer container) {
        return WukongWeaponCategories.isWeaponValid(container.getExecutor());
    }

    // 是否可执行的判定(此处直接调用父类逻辑)
    @Override
    public boolean canExecute(SkillContainer container) {
        return super.canExecute(container);
    }

    // 身外身法技能构建器
    public static class Builder extends SkillBuilder<ShenWaiShenFaSkill> {
        // 衍生(施法)动画
        protected StaticAnimationProvider derive1;

        public Builder() {}

        // 设置技能分类
        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        // 设置激活类型
        public Builder setActivateType(ActivateType activateType) {
            this.activateType = activateType;
            return this;
        }

        // 设置技能资源类型
        public Builder setResource(Resource resource) {
            this.resource = resource;
            return this;
        }

        // 设置创造模式标签页
        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }

        // 设置衍生(施法)动画
        public Builder setDeriveAnimations(StaticAnimationProvider derive1) {
            this.derive1 = derive1;
            return this;
        }
    }
}
