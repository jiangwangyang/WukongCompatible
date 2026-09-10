package com.p1nero.wukong.entity.client;

import com.p1nero.wukong.client.event.DingEndEvent;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

import org.jetbrains.annotations.NotNull;

// 定身效果: 一种增益型 MobEffect, 效果结束后触发定身结束事件
public class Ding extends MobEffect {
    // 构造定身效果: 归类为增益(BENEFICIAL), 颜色值为 -13261
    public Ding() {
        super(MobEffectCategory.BENEFICIAL, -13261);
    }

    // 返回效果的语言文件 Key(effect.wukong.ding)
    @Override
    public @NotNull String getDescriptionId() {
        return "effect.wukong.ding";
    }

    // 每个 tick 都触发效果逻辑
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    // 移除效果修饰符时调用: 触发定身结束事件(DingEndEvent)
    @Override
    public void removeAttributeModifiers(
            LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        DingEndEvent.execute(entity.level(), entity);
    }
}
