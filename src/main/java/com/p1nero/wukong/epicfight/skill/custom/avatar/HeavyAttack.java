package com.p1nero.wukong.epicfight.skill.custom.avatar;

import com.p1nero.wukong.epicfight.compat.StaticAnimationProvider;

import java.util.List;

// 重击接口: 提供重击招式对应的动画列表, 供身外身法等同步假悟空的动作
public interface HeavyAttack {
    // 获取该重击的全部重击动画
    List<StaticAnimationProvider> getHeavyAttacks();
}
