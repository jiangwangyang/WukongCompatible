package com.p1nero.wukong.epicfight.skill.custom.wukong;

import com.p1nero.wukong.epicfight.WukongSkillCategories;
import com.p1nero.wukong.epicfight.WukongStyles;
import net.minecraft.world.item.CreativeModeTab;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;



public class StaffStance extends Skill {

    public final WukongStyles style;

    public StaffStance(Builder builder) {
        super(builder);
        this.style = builder.style;
    }

    @Override
    public void onInitiate(SkillContainer container) {
        super.onInitiate(container);
    }



    public static Builder createStaffStyle() {
        return new Builder().setCategory(WukongSkillCategories.STAFF_STYLE).setResource(Resource.NONE);
    }

    @Override
    public void onRemoved(SkillContainer container) {
        super.onRemoved(container);
    }


    /**
     * 寰楁牴鎹甼ey杩斿洖锛屼笉鐒跺鎴风涓嶅悓姝ャ€傘€傚緢濂囨€紝onInitiate閲岄潰setDataSync浼氬嚭閿?     */
    public WukongStyles getStyle(SkillContainer container) {
        return style == null ? WukongStyles.SMASH : style;
    }


    public static class Builder extends SkillBuilder<StaffStance> {
        protected WukongStyles style;

        public Builder setStyle(WukongStyles style) {
            this.style = style;
            return this;
        }

        @Override
        public Builder setResource(Resource resource) {
            this.resource = resource;
            return this;
        }

        @Override
        public Builder setCategory(SkillCategory category) {
            this.category = category;
            return this;
        }

        @Override
        public Builder setCreativeTab(CreativeModeTab tab) {
            this.tab = tab;
            return this;
        }
    }
}
