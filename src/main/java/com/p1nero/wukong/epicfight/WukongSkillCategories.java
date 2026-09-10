package com.p1nero.wukong.epicfight;

import yesman.epicfight.skill.SkillCategory;

// 悟空技能类别枚举: 定义棍势/毫毛/身法/奇术四类技能的分类标识
public enum WukongSkillCategories implements SkillCategory {
    // 棍势
    STAFF_STYLE(true, true, true),

    HAO_MAO(true, true, true),
    // 身法技能类别
    SHENFA_STYLE(true, true, true),
    // 奇术
    FASHU_STYLE(true, true, true);
    // 是否保存
    final boolean save;
    // 是否同步到客户端
    final boolean sync;
    // 是否可学习
    final boolean modifiable;
    // 枚举管理器分配的唯一 ID
    final int id;

    // 构造枚举项: 记录保存/同步/可学习标志并向管理器申请 ID
    WukongSkillCategories(boolean ShouldSave, boolean ShouldSync, boolean Modifiable) {
        this.modifiable = Modifiable;
        this.save = ShouldSave;
        this.sync = ShouldSync;
        this.id = SkillCategory.ENUM_MANAGER.assign(this);
    }

    // 是否需要保存
    @Override
    public boolean shouldSave() {
        return this.save;
    }

    // 是否需要同步
    @Override
    public boolean shouldSynchronize() {
        return this.sync;
    }

    // 是否可学习
    @Override
    public boolean learnable() {
        return this.modifiable;
    }

    // 返回枚举管理器分配的唯一 ID
    @Override
    public int universalOrdinal() {
        return this.id;
    }
}
