package com.p1nero.wukong.client.keymapping;

import net.minecraft.client.KeyMapping;

// 方便判断是否是松开状态, 并且可以判断是奇次还是偶次按下
public class MyKeyMapping extends KeyMapping {
    // 按键是否刚松开(读取后自动复位)
    boolean isRelease;
    // 按下锁: 确保一次按下-松开只计一次
    boolean lock;
    // 当前是否处于偶数次按下
    boolean isEvenNumber;
    // 累计按下次数
    int pressCnt = 0;

    // 构造按键映射
    public MyKeyMapping(String p_90821_, int p_90822_, String p_90823_) {
        super(p_90821_, p_90822_, p_90823_);
    }

    // 按键状态回调: 按下时上锁, 松开时记录松开标记/奇偶次与累计次数
    @Override
    public void setDown(boolean down) {
        super.setDown(down);
        if (down) {
            lock = true;
            isRelease = false;
        } else if (lock) {
            lock = false;
            isRelease = true;
            isEvenNumber = !isEvenNumber;
            pressCnt++;
        }
    }

    // 判断是否松开并重置
    public boolean isRelease() {
        if (isRelease) {
            isRelease = false;
            return true;
        }
        return false;
    }

    // 是否是偶数次按下
    public boolean isEvenNumber() {
        return isEvenNumber;
    }

    // 返回总的按下的次数(含当前仍按住的一次)
    public int getPressCnt() {
        if (pressCnt == 0 && isDown()) {
            return 1;
        }
        if (isDown()) {
            return pressCnt + 1;
        }
        return pressCnt;
    }
}
