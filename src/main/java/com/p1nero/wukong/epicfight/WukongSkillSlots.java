package com.p1nero.wukong.epicfight;


import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;
//创建技能槽
public enum WukongSkillSlots implements SkillSlot {

    STAFF_STYLE(WukongSkillCategories.STAFF_STYLE) ,

    HAO_MAO(WukongSkillCategories.HAO_MAO),
    // 身法槽（例如：步伐、闪避、快速移动技能）
    SHENFA_SKILL_SLOT(WukongSkillCategories.SHENFA_STYLE),
    // 法术槽（例如：魔法攻击、法术技能）
    FASHU_SKILL_SLOT(WukongSkillCategories.FASHU_STYLE);

    final SkillCategory category;
    final int id;

    WukongSkillSlots(WukongSkillCategories category){
        this.category = category;
        this.id = SkillSlot.ENUM_MANAGER.assign(this);
    }


    @Override
    public SkillCategory category() {
        return category;
    }

    @Override
    public int universalOrdinal() {
        return id;
    }
}
