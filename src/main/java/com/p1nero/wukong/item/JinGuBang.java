package com.p1nero.wukong.item;

import com.p1nero.wukong.item.client.JinGuBangRenderer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import yesman.epicfight.world.item.WeaponItem;

import java.util.List;
import java.util.function.Consumer;

// 金箍棒物品: EpicFight武器物品, 使用GeckoLib动画渲染, 不可损耗耐久
public class JinGuBang extends WeaponItem implements GeoItem {
    // GeckoLib动画实例缓存
    AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 构造方法
    public JinGuBang(Tier tier, int damageIn, float speedIn, Properties builder) {
        super(tier, damageIn, speedIn, builder);
    }

    // 添加物品悬停提示文本
    @Override
    public void appendHoverText(
            @NotNull ItemStack itemStack,
            @Nullable Level p_41422_,
            @NotNull List<Component> list,
            @NotNull TooltipFlag p_41424_) {
        super.appendHoverText(itemStack, p_41422_, list, p_41424_);
        list.add(Component.nullToEmpty("凝星制作组赞助"));
    }

    // 物品不可损耗耐久
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    // 客户端初始化: 注册GeckoLib自定义物品渲染器
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(
                new IClientItemExtensions() {
                    private JinGuBangRenderer renderer = null;

                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        if (this.renderer == null) this.renderer = new JinGuBangRenderer();
                        return renderer;
                    }
                });
    }

    // 注册动画控制器: 循环播放idle动画
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(
                        this,
                        "idle",
                        0,
                        (animationState -> {
                            animationState
                                    .getController()
                                    .setAnimation(
                                            RawAnimation.begin()
                                                    .then("idle", Animation.LoopType.LOOP));
                            return PlayState.CONTINUE;
                        })));
    }

    // 返回GeckoLib动画实例缓存
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
