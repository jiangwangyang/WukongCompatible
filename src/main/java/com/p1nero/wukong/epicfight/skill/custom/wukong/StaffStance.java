package com.p1nero.wukong.epicfight.skill.custom.wukong;

import com.p1nero.wukong.epicfight.WukongSkillCategories;
import com.p1nero.wukong.epicfight.WukongStyles;

import net.minecraft.world.item.CreativeModeTab;

import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;

// 棍式技能: 记录当前棍式(劈棍/戳棍/立棍/大圣), 供武器能力读取以确定连段与重击
public class StaffStance extends Skill {

    // 当前棍式(为空时默认劈棍)
    public final WukongStyles style;

    // 构造方法, 保存棍式
    public StaffStance(Builder builder) {
        super(builder);
        this.style = builder.style;
    }

    // 技能激活时的回调(当前无额外逻辑, 仅调用父类)
    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
    }

    // 创建棍式技能构建器, 设为STAFF_STYLE分类且无需消耗资源
    public static Builder createStaffStyle() {
        return new Builder()
                .setCategory(WukongSkillCategories.STAFF_STYLE)
                .setResource(Resource.NONE);
    }

    // 技能移除时的回调(当前无额外逻辑, 仅调用父类)
    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
    }

    // 返回当前棍式, 必须按容器取值以保证客户端同步(onInitiate中setDataSync会出错, 故不在此写)
    public WukongStyles getStyle(SkillContainer container) {
        return style == null ? WukongStyles.SMASH : style;
    }

    // 技能构建器, 收集棍式
    public static class Builder extends SkillBuilder<StaffStance> {
        protected WukongStyles style; // 棍式

        // 设置棍式
        public Builder setStyle(WukongStyles style) {
            this.style = style;
            return this;
        }

        // 设置消耗资源
        @Override
        public Builder setResource(Resource resource) {
            this.resource = resource;
            return this;
        }

        // 设置技能分类
        @Override
        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        // 设置创造模式标签页
        @Override
        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }
    }
}
