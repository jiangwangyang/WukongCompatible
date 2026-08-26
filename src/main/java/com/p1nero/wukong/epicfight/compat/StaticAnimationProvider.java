package com.p1nero.wukong.epicfight.compat;

import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.MainFrameAnimation;

@FunctionalInterface
public interface StaticAnimationProvider {
    AnimationManager.AnimationAccessor<? extends MainFrameAnimation> get();
}
