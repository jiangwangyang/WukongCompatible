package com.p1nero.wukong.epicfight;

import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;

// 创建技能槽: 定义棍势/毫毛/身法/法术四类技能槽
public enum WukongSkillSlots implements SkillSlot {
    STAFF_STYLE(WukongSkillCategories.STAFF_STYLE),

    HAO_MAO(WukongSkillCategories.HAO_MAO),
    // 身法槽(例如: 步伐, 闪避, 快速移动技能)
    SHENFA_SKILL_SLOT(WukongSkillCategories.SHENFA_STYLE),
    // 法术槽(例如: 魔法攻击, 法术技能)
    FASHU_SKILL_SLOT(WukongSkillCategories.FASHU_STYLE);

    // 该槽位对应的技能类别
    final SkillCategory category;
    // 枚举管理器分配的唯一 ID
    final int id;

    // 构造技能槽: 绑定技能类别并向管理器申请 ID
    WukongSkillSlots(WukongSkillCategories category) {
        this.category = category;
        this.id = SkillSlot.ENUM_MANAGER.assign(this);
    }

    // 返回该槽位对应的技能类别
    @Override
    public SkillCategory category() {
        return category;
    }

    // 返回枚举管理器分配的唯一 ID
    @Override
    public int universalOrdinal() {
        return id;
    }
}
