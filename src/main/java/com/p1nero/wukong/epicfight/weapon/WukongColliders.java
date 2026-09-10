package com.p1nero.wukong.epicfight.weapon;

import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;

// 悟空武器碰撞盒集合: 集中定义各类招式(劈/戳/立棍/大圣)对应的 MultiOBBCollider 碰撞体
public class WukongColliders {
    // 跳跃轻攻击碰撞盒
    public static final Collider JUMP_ATTACK_LIGHT =
            new MultiOBBCollider(4, 0.8, 0.8, 0.8, 0.0, 0.9, 0.0);
    // 悟空棍基础碰撞盒
    public static final Collider WK_STAFF = new MultiOBBCollider(4, 0.2, 0.3, 1.8, 0.0, 0.0, 0.0);
    // STACK_0_1 蓄力棍势碰撞盒
    public static final Collider STACK_0_1 = new MultiOBBCollider(4, 0.3, 0.3, 1.8, 0.0, 0.0, -0.4);
    // STACK_2 蓄力棍势碰撞盒
    public static final Collider STACK_2 = new MultiOBBCollider(4, 0.3, 0.3, 2.5, 0.0, 0.0, -0.8);
    // STACK_3 蓄力棍势碰撞盒
    public static final Collider STACK_3 = new MultiOBBCollider(4, 0.3, 0.3, 3.4, 0.0, 0.0, -1.2);
    // STACK_4 蓄力棍势碰撞盒
    public static final Collider STACK_4 = new MultiOBBCollider(4, 0.6, 0.6, 4.3, 0.0, 0.0, -1.6);
    // STACK_5 蓄力棍势碰撞盒
    public static final Collider STACK_5 = new MultiOBBCollider(4, 1, 1, 10, 0.0, 0.0, -1.6);
    // STACK_6 蓄力棍势碰撞盒
    public static final Collider STACK_6 = new MultiOBBCollider(4, 0.2, 0.2, 2.5, 0.0, 0.0, -1.6);
    // STACK_7 蓄力棍势碰撞盒
    public static final Collider STACK_7 = new MultiOBBCollider(4, 0.3, 0.3, 5, 0.0, 0.0, 0);
    // THRUST_FOOTAGE 戳棍招式碰撞盒
    public static final Collider THRUST_FOOTAGE =
            new MultiOBBCollider(4, 0.3, 0.3, 4, 0.0, 0.0, -3);
    // THRUST_FENGCHUANHUA 戳棍招式碰撞盒
    public static final Collider THRUST_FENGCHUANHUA =
            new MultiOBBCollider(4, 5, 5, 7, 0.0, 0.0, 6);
    // THRUST_CHARGED2 戳棍蓄力招式碰撞盒
    public static final Collider THRUST_CHARGED2 =
            new MultiOBBCollider(4, 0.22, 0.2, 3, 0.0, 0.0, 3);
    // THRUST_CHARGED3 戳棍蓄力招式碰撞盒
    public static final Collider THRUST_CHARGED3 =
            new MultiOBBCollider(4, 0.35, 0.35, 7, 0.0, 0.0, 5);
    // PILLAR_FENGYUNZHUAN 立棍招式碰撞盒
    public static final Collider PILLAR_FENGYUNZHUAN =
            new MultiOBBCollider(2, 2, 2, 0, 0.0, 0.0, 0);
    // PILLAR_HEAVY1 立棍重击碰撞盒
    public static final Collider PILLAR_HEAVY1 = new MultiOBBCollider(4, 0.3, 0.3, 2, 0.0, 0.0, 0);
    // PILLAR_HEAVY2 立棍重击碰撞盒
    public static final Collider PILLAR_HEAVY2 = new MultiOBBCollider(4, 0.3, 0.3, 3, 0.0, 0.0, 0);
    // PILLAR_HEAVY3 立棍重击碰撞盒
    public static final Collider PILLAR_HEAVY3 = new MultiOBBCollider(4, 0.3, 0.3, 4, 0.0, 0.0, 0);
    // PILLAR_HEAVY4 立棍重击碰撞盒
    public static final Collider PILLAR_HEAVY4 =
            new MultiOBBCollider(4, 0.3, 0.3, 4.5, 0.0, 0.0, 0);
    // PILLAR_HEAVY3_SAGE 立棍重击(大圣形态)碰撞盒
    public static final Collider PILLAR_HEAVY3_SAGE =
            new MultiOBBCollider(4, 1, 1, 10, 0.0, 0.0, -1.6);
    // PILLAR_HEAVY_RIVERSEAFLIP 立棍重击碰撞盒
    public static final Collider PILLAR_HEAVY_RIVERSEAFLIP =
            new MultiOBBCollider(4, 0.3, 0.3, 2.5, 0.0, 0.0, -0.8);
    // THRUST_JUESICK_START 戳棍招式起始碰撞盒
    public static final Collider THRUST_JUESICK_START =
            new MultiOBBCollider(4, 0.3, 0.3, 2.5, 0.0, 0.0, -0.8);

    // HENSHIN 变身碰撞盒
    public static final Collider HENSHIN = new MultiOBBCollider(4, 0.3, 0.3, 7.0, 0.0, 0.0, 6.0);
    // GREATSAGE_DERIVATIVE 大圣衍生碰撞盒
    public static final Collider GREATSAGE_DERIVATIVE =
            new MultiOBBCollider(1, 1.0, 1.0, 1.0, 0.0, 0.0, 6.0);
    // GREATSAGE_XULI4 大圣蓄力碰撞盒
    public static final Collider GREATSAGE_XULI4 =
            new MultiOBBCollider(4, 3.0, 3.0, 7.0, 0.0, 0.0, 6.0);
    // GREATSAGE_CHOPSTICK 大圣招式碰撞盒
    public static final Collider GREATSAGE_CHOPSTICK =
            new MultiOBBCollider(4, 0.6, 0.6, 3.0, 0.0, 0.0, 0.0);
}
