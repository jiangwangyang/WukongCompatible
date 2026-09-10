package com.p1nero.wukong.capability;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class WKPlayer {
    private String lastSkill = ""; // 用于恢复闪避技能
    private boolean perfectDodge;
    private float damageReduce = 0.0f; // 伤害减少值, 默认值为 0

    // 已生成的假悟空实体ID列表
    private final List<Integer> fakeWukongIds = new ArrayList<>();

    // 设置是否处于完美闪避状态
    public void setPerfectDodge(boolean perfectDodge) {
        this.perfectDodge = perfectDodge;
    }

    // 记录一个已生成的假悟空分身实体ID
    public void addFakeWukongId(int id) {
        fakeWukongIds.add(id);
    }

    // 查询是否处于完美闪避状态
    public boolean isPerfectDodge() {
        return perfectDodge;
    }

    // 获取当前伤害减少值
    public float getDamageReduce() {
        return damageReduce;
    }

    // 记录玩家上一次使用的闪避技能ID
    public void setLastDodgeSkill(String lastSkill) {
        this.lastSkill = lastSkill;
    }

    // 获取玩家上一次使用的闪避技能ID
    public String getLastDodgeSkill() {
        return lastSkill;
    }

    // 将需要持久化的数据(上次闪避技能)写入NBT
    public void saveNBTData(CompoundTag tag) {
        tag.putString("lastSkill", lastSkill);
    }

    // 从NBT读取数据
    public void loadNBTData(CompoundTag tag) {
        lastSkill = tag.getString("lastSkill");
    }

    // 玩家克隆时从旧实例复制需要保留的数据
    public void copyFrom(WKPlayer old) {
        lastSkill = old.lastSkill;
    }

    // 返回假悟空实体ID列表
    public List<Integer> getFakeWukongIds() {
        return fakeWukongIds;
    }
}
