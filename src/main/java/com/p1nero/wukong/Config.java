package com.p1nero.wukong;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = WukongMoveset.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
// 模组配置文件类, 定义蓄力/格挡等可调参数, 并注册 wukong 管理命令
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    // 切手技判定时间(tick)
    public static final ForgeConfigSpec.DoubleValue DERIVE_CHECK_TIME;
    // 四蓄衰减速度基数(每tick流失该值/5的棍势)
    public static final ForgeConfigSpec.DoubleValue CHARGING_SPEED;
    // 四蓄(满星)窗口时长(tick), 窗口内命中会刷新计时
    public static final ForgeConfigSpec.DoubleValue CHARGED4_WINDOW_TICKS;
    // 普攻间隔判定时间(tick)
    public static final ForgeConfigSpec.DoubleValue BASIC_ATTACK_INTERVAL_TICKS;
    // 是否给首个进入游戏的玩家发放指南书
    public static final ForgeConfigSpec.BooleanValue GET_GUILD_BOOK;
    // 可被棍花格挡的实体类型列表(字符串形式的资源位置)
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>>
            ENTITIES_CAN_BE_BLOCKED_BY_STAFF_FLOWER;
    // 配置规范对象
    public static final ForgeConfigSpec SPEC;

    // 可被棍花格挡的实体类型集合(运行时缓存, 由配置填充)
    public static Set<? extends EntityType<?>> entities_can_be_blocked = new HashSet<>();

    static {
        GET_GUILD_BOOK = createBool("给第一个进游戏的玩家玩法说明。", "get_guild_book", true);
        BASIC_ATTACK_INTERVAL_TICKS =
                createDouble("自定义普攻间隔判定时间（原版太长了！）", "basic_attack_time_interval", 16.0);
        DERIVE_CHECK_TIME = createDouble("切手技判定时间", "derive_check_time", 30.0);
        CHARGING_SPEED = createDouble("四蓄衰减速度基数（3星以上且窗口结束时每tick流失该值/5的棍势）", "charging_speed", 1.0);
        CHARGED4_WINDOW_TICKS =
                createDouble(
                        "四蓄(4星)窗口时长tick数，窗口内命中敌人会刷新计时，窗口结束时满星降回3星(20 ticks = 1秒)，四种棍式统一",
                        "charged4_window_ticks",
                        300.0);
        ENTITIES_CAN_BE_BLOCKED_BY_STAFF_FLOWER =
                BUILDER.comment("可被棍花格挡的实体")
                        .defineListAllowEmpty(
                                List.of("可被棍花格挡的实体"),
                                () -> List.of("minecraft:arrow"),
                                Config::validateEntityName);
        SPEC = BUILDER.build();
    }

    private static ForgeConfigSpec.BooleanValue createBool(String key, boolean defaultValue) {
        return BUILDER.translation("config." + WukongMoveset.MOD_ID + "." + key)
                .define(key, defaultValue);
    }

    private static ForgeConfigSpec.BooleanValue createBool(
            String comment, String key, boolean defaultValue) {
        return BUILDER.comment(comment)
                .translation("config." + WukongMoveset.MOD_ID + "." + key)
                .define(key, defaultValue);
    }

    private static ForgeConfigSpec.DoubleValue createDouble(
            String comment, String key, double defaultValue) {
        return BUILDER.comment(comment)
                .translation("config." + WukongMoveset.MOD_ID + "." + key)
                .defineInRange(key, defaultValue, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    // 校验配置值是否为合法的实体类型资源位置字符串
    private static boolean validateEntityName(final Object obj) {
        return obj instanceof final String itemName
                && ForgeRegistries.ENTITY_TYPES.containsKey(ResourceLocation.parse(itemName));
    }

    // 注册 wukong 命令(需2级权限)
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("wukong")
                        .requires((commandSourceStack) -> commandSourceStack.hasPermission(2)));
    }
}
