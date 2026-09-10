package com.p1nero.wukong.epicfight;

import yesman.epicfight.world.capabilities.item.Style;

// 悟空武器风格枚举: 定义劈棍/戳棍/立棍/大圣四类战斗风格
public enum WukongStyles implements Style {
    SMASH(false),
    THRUST(false),
    PILLAR(false),
    GREATSAGE(false);

    // 是否可使用副手
    private final boolean canUseOffhand;
    // 枚举管理器分配的唯一 ID
    private final int id;

    // 构造风格项: 向管理器申请 ID 并记录副手可用性
    WukongStyles(boolean canUseOffhand) {
        this.id = Style.ENUM_MANAGER.assign(this);
        this.canUseOffhand = canUseOffhand;
    }

    // 返回枚举管理器分配的唯一 ID
    @Override
    public int universalOrdinal() {
        return this.id;
    }

    // 是否允许使用副手
    @Override
    public boolean canUseOffhand() {
        return this.canUseOffhand;
    }
}
