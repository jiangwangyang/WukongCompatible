package com.p1nero.wukong.capability;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class WKPlayer {
    private String lastSkill = "";//用于恢复闪避技能
    private boolean perfectDodge;
    private float damageReduce = 0.0f; // 伤害减少值，默认值为 0
    private int blfov = 0;  // 静态字段

    private  final List<Integer> fakeWukongIds= new ArrayList<>();

    public  void setFovsz(int fov) {
        blfov = fov;  // 访问静态字段
    }

    public  int getFovsz() {
        return blfov;
    }

    public void setPerfectDodge(boolean perfectDodge) {
        this.perfectDodge = perfectDodge;
    }
    public void addFakeWukongId(int id){
        fakeWukongIds.add(id);
    }


    public boolean isPerfectDodge() {
        return !perfectDodge;
    }
    public float getDamageReduce() {
        return damageReduce;
    }
    public void setLastDodgeSkill(String lastSkill) {
        this.lastSkill = lastSkill;
    }

    public String getLastDodgeSkill() {
        return lastSkill;
    }

    public void saveNBTData(CompoundTag tag){
        tag.putString("lastSkill", lastSkill);
    }

    public void loadNBTData(CompoundTag tag){
        lastSkill = tag.getString("lastSkill");
    }

    public void copyFrom(WKPlayer old){
        lastSkill = old.lastSkill;
    }

    public List<Integer> getFakeWukongIds() {
        return fakeWukongIds;
    }
}
