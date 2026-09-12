package com.p1nero.wukong.item;

import com.p1nero.wukong.WukongMoveset;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// 悟空模组物品注册表: 注册棍类武器/大圣套装护甲以及创造模式标签页
public class WukongItems {
    // 物品注册器
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, WukongMoveset.MOD_ID);
    // 棍(测试用武器, 高耐久)
    public static final RegistryObject<Item> STAFF =
            ITEMS.register(
                    "staff",
                    () ->
                            new TestStaff(
                                    Tiers.NETHERITE,
                                    -3,
                                    -3,
                                    (new Item.Properties())
                                            .defaultDurability(114514)
                                            .rarity(Rarity.COMMON)));
    // 亢金(备用棍武器, uncommon品质)
    public static final RegistryObject<Item> KANG_JIN =
            ITEMS.register(
                    "kang_jin",
                    () ->
                            new TestStaff(
                                    Tiers.NETHERITE,
                                    1,
                                    -3,
                                    (new Item.Properties())
                                            .defaultDurability(2777)
                                            .rarity(Rarity.UNCOMMON)));
    // 金箍棒(史诗品质武器, 不可堆叠不可修复)
    public static final RegistryObject<Item> JIN_GU_BANG =
            ITEMS.register(
                    "jingubang",
                    () ->
                            new JinGuBang(
                                    Tiers.NETHERITE,
                                    3,
                                    -3,
                                    (new Item.Properties())
                                            .stacksTo(1)
                                            .setNoRepair()
                                            .rarity(Rarity.EPIC)));
    // 大圣头盔
    public static final RegistryObject<Item> DASHENG_H =
            ITEMS.register(
                    "dasheng_h",
                    () ->
                            new DaShengArmorItem(
                                    WukongArmorMaterials.DA_SHENG,
                                    ArmorItem.Type.HELMET,
                                    (new Item.Properties())
                                            .defaultDurability(2777)
                                            .rarity(Rarity.EPIC)));
    // 大圣胸甲
    public static final RegistryObject<Item> DASHENG_C =
            ITEMS.register(
                    "dasheng_c",
                    () ->
                            new DaShengArmorItem(
                                    WukongArmorMaterials.DA_SHENG,
                                    ArmorItem.Type.CHESTPLATE,
                                    (new Item.Properties())
                                            .defaultDurability(2777)
                                            .rarity(Rarity.EPIC)));
    // 大圣护腿
    public static final RegistryObject<Item> DASHENG_L =
            ITEMS.register(
                    "dasheng_l",
                    () ->
                            new DaShengArmorItem(
                                    WukongArmorMaterials.DA_SHENG,
                                    ArmorItem.Type.LEGGINGS,
                                    (new Item.Properties())
                                            .defaultDurability(2777)
                                            .rarity(Rarity.EPIC)));
    // 大圣靴子
    public static final RegistryObject<Item> DASHENG_F =
            ITEMS.register(
                    "dasheng_f",
                    () ->
                            new DaShengArmorItem(
                                    WukongArmorMaterials.DA_SHENG,
                                    ArmorItem.Type.BOOTS,
                                    (new Item.Properties())
                                            .defaultDurability(2777)
                                            .rarity(Rarity.EPIC)));

    // 创造模式标签页注册器
    public static final DeferredRegister<CreativeModeTab> ITEM_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WukongMoveset.MOD_ID);
    // 悟空模组创造模式标签页, 图标为金箍棒, 收录本模组全部物品
    public static final RegistryObject<CreativeModeTab> WK =
            ITEM_TAB.register(
                    "wukong_items",
                    () ->
                            CreativeModeTab.builder()
                                    .title(Component.translatable("itemGroup.wukong.items"))
                                    .icon(() -> new ItemStack(JIN_GU_BANG.get()))
                                    .displayItems(
                                            (parameters, tabData) -> {
                                                tabData.accept(STAFF.get());
                                                tabData.accept(KANG_JIN.get());
                                                tabData.accept(JIN_GU_BANG.get());

                                                tabData.accept(DASHENG_H.get());
                                                tabData.accept(DASHENG_C.get());
                                                tabData.accept(DASHENG_L.get());
                                                tabData.accept(DASHENG_F.get());
                                            })
                                    .build());
}
