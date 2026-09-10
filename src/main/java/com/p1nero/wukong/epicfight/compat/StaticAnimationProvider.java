package com.p1nero.wukong.epicfight.compat;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.MainFrameAnimation;

// 静态动画提供器接口: 以函数式方式获取一个静态动画访问器
@FunctionalInterface
public interface StaticAnimationProvider {
    // 返回对应静态动画的访问器
    AnimationManager.AnimationAccessor<? extends MainFrameAnimation> get();
}
