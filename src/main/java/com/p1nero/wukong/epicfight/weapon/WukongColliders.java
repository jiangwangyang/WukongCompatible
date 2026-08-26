package com.p1nero.wukong.epicfight.weapon;

import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;

public class WukongColliders {
    public static final Collider JUMP_ATTACK_LIGHT = new MultiOBBCollider(4, 0.8, 0.8, 0.8, 0.0, 0.9, 0.0);
    public static final Collider WK_STAFF = new MultiOBBCollider(4, 0.2, 0.3, 1.8, 0.0, 0.0, 0.0);
    public static final Collider STACK_0_1 = new MultiOBBCollider(4, 0.3, 0.3, 1.8, 0.0, 0.0, -0.4);
    public static final Collider STACK_2 = new MultiOBBCollider(4, 0.3, 0.3, 2.5, 0.0, 0.0, -0.8);
    public static final Collider STACK_3 = new MultiOBBCollider(4, 0.3, 0.3, 3.4, 0.0, 0.0, -1.2);
    public static final Collider STACK_4 = new MultiOBBCollider(4, 0.6, 0.6, 4.3, 0.0, 0.0, -1.6);
    public static final Collider STACK_5 = new MultiOBBCollider(4, 1, 1, 10, 0.0, 0.0, -1.6);
    public static final Collider STACK_6 = new MultiOBBCollider(4, 0.2, 0.2, 2.5, 0.0, 0.0, -1.6);
    public static final Collider STACK_7 = new MultiOBBCollider(4, 0.3, 0.3, 5, 0.0, 0.0, 0);
    public static final Collider THRUST_FOOTAGE = new MultiOBBCollider(4, 0.3, 0.3, 4, 0.0, 0.0, -3);
    public static final Collider THRUST_FENGCHUANHUA = new MultiOBBCollider(4, 5, 5, 7, 0.0, 0.0, 6);
    public static final Collider THRUST_CHARGED2 = new MultiOBBCollider(4, 0.22, 0.2, 3, 0.0, 0.0, 3);
    public static final Collider THRUST_CHARGED3 = new MultiOBBCollider(4, 0.35, 0.35, 7, 0.0, 0.0, 5);
    public static final Collider PILLAR_FENGYUNZHUAN = new MultiOBBCollider(2, 2, 2, 0, 0.0, 0.0, 0);
    public static final Collider PILLAR_HEAVY1 = new MultiOBBCollider(4, 0.3, 0.3, 2, 0.0, 0.0, 0);
    public static final Collider PILLAR_HEAVY2 = new MultiOBBCollider(4, 0.3, 0.3, 3, 0.0, 0.0, 0);
    public static final Collider PILLAR_HEAVY3 = new MultiOBBCollider(4, 0.3, 0.3, 4, 0.0, 0.0, 0);
    public static final Collider PILLAR_HEAVY4 = new MultiOBBCollider(4, 0.3, 0.3, 4.5, 0.0, 0.0, 0);
    public static final Collider PILLAR_HEAVY3_SAGE =  new MultiOBBCollider(4, 1, 1, 10, 0.0, 0.0, -1.6);
    public static final Collider PILLAR_HEAVY_RIVERSEAFLIP = new MultiOBBCollider(4, 0.3, 0.3, 2.5, 0.0, 0.0, -0.8);
    public static final Collider THRUST_JUESICK_START = new MultiOBBCollider(4, 0.3, 0.3, 2.5, 0.0, 0.0, -0.8);

    public static final Collider HENSHIN = new MultiOBBCollider(4, 0.3, 0.3, 7.0, 0.0, 0.0, 6.0);
    public static final Collider GREATSAGE_DERIVATIVE = new MultiOBBCollider(1, 1.0, 1.0, 1.0, 0.0, 0.0, 6.0);
    public static final Collider GREATSAGE_XULI4 = new MultiOBBCollider(4, 3.0, 3.0, 7.0, 0.0, 0.0, 6.0);
    public static final Collider GREATSAGE_CHOPSTICK = new MultiOBBCollider(4, 0.6, 0.6, 3.0, 0.0, 0.0, 0.0);




}
