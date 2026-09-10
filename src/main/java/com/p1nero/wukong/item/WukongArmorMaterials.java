package com.p1nero.wukong.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.util.Lazy;

import java.util.function.Supplier;

// 悟空模组护甲材质枚举: 定义套装的耐久/护甲值/附魔亲和度/装备音效/韧性与修复材料
public enum WukongArmorMaterials implements ArmorMaterial {
    // 大圣套装材质(耐久倍率237, 四槽位护甲值6/10/10/5, 附魔等级15, 韧性3.0, 下界合金锭修复)
    DA_SHENG(
            "da_sheng",
            237,
            new int[] {6, 10, 10, 5},
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0F,
            0.1F,
            () -> Ingredient.of(Items.NETHERITE_INGOT));

    // 原版各槽位基础耐久值(头/胸/腿/靴)
    private static final int[] HEALTH_PER_SLOT = new int[] {13, 15, 16, 11};
    // 材质参数: 名称/耐久倍率/各槽位护甲值/附魔亲和度/装备音效/韧性/击退抗性/修复材料
    private final String name;
    private final int durabilityMultiplier;
    private final int[] slotProtections;
    private final int enchantmentValue;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;
    // 弃用API迁移: LazyLoadedValue已被原版标记废弃, 等价替换为Forge的Lazy(同为懒加载Supplier, get()用法不变)
    private final Lazy<Ingredient> repairIngredient;

    // 构造方法, 保存材质参数
    WukongArmorMaterials(
            String p_40474_,
            int p_40475_,
            int[] p_40476_,
            int p_40477_,
            SoundEvent p_40478_,
            float p_40479_,
            float p_40480_,
            Supplier p_40481_) {
        this.name = p_40474_;
        this.durabilityMultiplier = p_40475_;
        this.slotProtections = p_40476_;
        this.enchantmentValue = p_40477_;
        this.sound = p_40478_;
        this.toughness = p_40479_;
        this.knockbackResistance = p_40480_;
        this.repairIngredient = Lazy.of(p_40481_);
    }

    // 返回指定槽位的护甲耐久
    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return HEALTH_PER_SLOT[type.ordinal()] * this.durabilityMultiplier;
    }

    // 返回指定槽位的护甲值
    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.slotProtections[type.ordinal()];
    }

    // 返回附魔亲和度
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    // 返回装备音效
    public SoundEvent getEquipSound() {
        return this.sound;
    }

    // 返回修复材料
    public Ingredient getRepairIngredient() {
        return (Ingredient) this.repairIngredient.get();
    }

    // 返回材质名称
    public String getName() {
        return this.name;
    }

    // 返回护甲韧性
    public float getToughness() {
        return this.toughness;
    }

    // 返回击退抗性
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
