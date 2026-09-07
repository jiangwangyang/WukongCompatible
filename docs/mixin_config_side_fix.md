# Mixin 副作用修复: PatchedLivingEntityRendererMixin 移入 client 列表

## 1. 概述

本 mod(黑风山悟空动作组, 后文简称"本 mod")的 mixin 配置文件 wukong.mixins.json 中, 针对客户端专属渲染类 PatchedLivingEntityRenderer 的注入器 PatchedLivingEntityRendererMixin 被登记在了"双端都加载"的 mixins 列表里。配合配置顶部的 "required": true 与注入器默认要求 "defaultRequire": 1, 在没有该目标类的专用服务器上会导致 mixin 加载失败并中止启动。本次修复将这一条目移入仅客户端加载的 client 列表, 服务端不再处理它。

| 项目 | 内容 |
|---|---|
| 功能点名称 | PatchedLivingEntityRendererMixin 的加载端别(从双端改为仅客户端) |
| 是什么 | 调整 wukong.mixins.json 中一条 mixin 条目的登记位置, 不改动任何 Java 代码 |
| 为什么 | 该 mixin 的目标类是纯客户端渲染类, 专用服务器上不存在, 双端加载必然失败 |
| 作用 | 修复客户端 mixin 被服务端处理导致的启动报错/加载失败, 客户端行为不变 |

## 2. 背景知识

### 2.1 mixin 配置文件里的两个列表有什么区别

mixin 是一种"字节码手术"机制: 把写好的增强代码(标了 @Mixin 的类)合并进游戏或其它 mod 的既有类里, 不需要直接改那些类的源码。每个 mod 的 mixin 配置文件(本 mod 为 src/main/resources/wukong.mixins.json)必须告诉 mixin 框架: 哪些增强代码在哪种环境下生效。

- "mixins" 列表: 登记在这里的 mixin 会在"两端"都尝试生效。Minecraft 的一个存档可以既跑客户端也跑专用服务器, mod 的同一个 jar 会被两处使用, 所以双端逻辑(技能、网络包、伤害处理等)的 mixin 登记在这里;
- "client" 列表: 登记在这里的 mixin 只在客户端(游戏画面那端)处理, 专用服务器会直接跳过。凡是目标类只在客户端存在的 mixin, 必须登记在这里, 因为专用服务器上根本没有那些类可注入。

Mixin 官方配置说明明确要求: 目标为 client 专属类的 mixin 必须放进 client 列表, 否则在专用服务器上该 mixin 找不到目标类而失败。

### 2.2 "required": true 与 "defaultRequire": 1 意味着失败就是致命错误

- 配置顶部的 "required": true 表示本配置文件是 mod 正常运行所必需的, 配置本身读取失败视为致命;
- "injectors" 段的 "defaultRequire": 1 表示每条注入(本 mod 的 @ModifyArg/@Inject 等)默认要求至少命中一次, 一次都没命中(比如目标类不存在、方法签名对不上)按"注入失败"处理, mixin 框架会抛出 InvalidMixinException/MixinApplyError 一类错误, 该 mixin 配置整体判定为加载失败, 游戏启动中止。

换句话说, 在这套设置下, "某个 mixin 在服务端找不到目标类"不是警告, 而是直接崩溃/加载失败。这正是双端列表里混入客户端 mixin 会出问题的原因。

### 2.3 PatchedLivingEntityRenderer 是 Epic Fight 的纯客户端渲染类

目标类 yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer 来自依赖 mod Epic Fight 20.14.17(本仓库 libs/EpicFight-20.14.17.jar)。反编译核实:

- 它位于 Epic Fight 的 client 渲染包下, 与 PCreeperRenderer/PHumanoidRenderer 等同包;
- 它的字节码直接引用 net/minecraft/client/Minecraft、net/minecraft/client/player/LocalPlayer、net/minecraft/client/renderer/entity/LivingEntityRenderer、net/minecraft/client/renderer/entity/EntityRenderDispatcher 等类, 这些类只在客户端的运行环境里存在(jar 包里虽有 class 文件, 但专用服务器上不会被加载, 一旦触碰即触发 Forge 的 dist 检查报 "Attempted to load class ... for invalid dist DEDICATED_SERVER" 或类找不到错误)。

所以"专用服务器上这个类不存在"是事实层面成立的, 不是推测。

### 2.4 该 mixin 注入的是什么

PatchedLivingEntityRendererMixin 对上述渲染类的 render 方法做 @ModifyArg, 修改其调用 SkinnedMesh.draw 时传入的透明度(alpha)参数, 用来实现"聚形散气"技能中大圣残影随计时渐隐/渐显的视觉效果(见同文件 modifyAlpha 方法, 读取 SHENFA_SKILL_SLOT 技能槽的 JXSQ_YINGSHEN_TIMER/JXSQ_YINGSHEN_ZT 数据)。渲染透明度是纯画面效果, 只在客户端有意义, 也不需要服务端任何配合。

## 3. 问题分析: 原配置为什么有问题

修复前 wukong.mixins.json 的登记(节选):

```json
{
  "required": true,
  "client": [
    "ItemRendererMixin"
  ],
  "mixins": [
    "BasicAttackMixin",
    "EntityStateMixin",
    "PatchedLivingEntityRendererMixin",
    "TechnicianSkillMixin",
    ...
  ],
  "injectors": { "defaultRequire": 1 },
  ...
}
```

- PatchedLivingEntityRendererMixin 登记在双端列表 "mixins" 里;
- 该 mixin 的 @Mixin 注解指向 remap = false 的客户端类(见 src/main/java/com/p1nero/wukong/mixin/PatchedLivingEntityRendererMixin.java 第 24 行), 类体还 import 了 Minecraft 与 LocalPlayer 等客户端类;
- 配置为 "required": true 且 "defaultRequire": 1。

于是专用服务器启动时, mixin 框架按双端列表处理该条目: 目标类不存在, 注入无法完成, 在 required + defaultRequire 设置下升级为致命错误, mixin 配置加载失败, mod 启动中止。即便某些 mixin 版本对"目标从未加载"的场景延迟处理, 该条目也始终是服务端的一颗雷: 任何使其被处理/加载的路径都会因引用客户端类而报错。

本项目所有 mixin 的目标类逐一核对结果(依据各 @Mixin 注解与 Epic Fight 反编译):

| mixin | 目标类 | 所在包 | 端别判定 | 原登记 | 是否需要处理 |
|---|---|---|---|---|---|
| ItemRendererMixin | ItemRenderer | net.minecraft.client.renderer.entity | 仅客户端 | client | 已正确, 无需处理 |
| PatchedLivingEntityRendererMixin | PatchedLivingEntityRenderer | yesman.epicfight.client.renderer.patched.entity | 仅客户端 | mixins | 需移入 client |
| BasicAttackMixin | BasicAttack | yesman.epicfight.skill | 双端 | mixins | 正确 |
| EntityStateMixin | EntityState | yesman.epicfight.api.animation.types | 双端 | mixins | 正确 |
| TechnicianSkillMixin | TechnicianSkill | yesman.epicfight.skill.passive | 双端 | mixins | 正确 |
| animation_types_mixin.AnimationPlayerMixin | AnimationPlayer | yesman.epicfight.api.animation | 双端 | mixins | 正确 |
| animation_types_mixin.AttackAnimation | AttackAnimation | yesman.epicfight.api.animation.types | 双端 | mixins | 正确 |
| animation_types_mixin.ConcurrentLinkAnimationMixin | AttackAnimation | 同上 | 双端 | mixins | 正确 |
| animation_types_mixin.DynamicAnimationMixin | DynamicAnimation | yesman.epicfight.api.animation | 双端 | mixins | 正确 |
| animation_types_mixin.MovementAnimationMixin | MovementAnimation | yesman.epicfight.api.animation.types | 双端 | mixins | 正确 |

即: 全项目只有 PatchedLivingEntityRendererMixin 一处登记错误, 与外部反馈的问题定位一致。

## 4. 修改内容

仅改动 src/main/resources/wukong.mixins.json 一处, 把 "PatchedLivingEntityRendererMixin" 从 "mixins" 列表移入 "client" 列表(与 ItemRendererMixin 并列)。Java 代码零改动。

修改前:

```json
"client": [
  "ItemRendererMixin"
],
"mixins": [
  "BasicAttackMixin",
  "EntityStateMixin",
  "PatchedLivingEntityRendererMixin",
  "TechnicianSkillMixin",
  "animation_types_mixin.AnimationPlayerMixin",
  "animation_types_mixin.AttackAnimation",
  "animation_types_mixin.ConcurrentLinkAnimationMixin",
  "animation_types_mixin.DynamicAnimationMixin",
  "animation_types_mixin.MovementAnimationMixin"
]
```

修改后:

```json
"client": [
  "ItemRendererMixin",
  "PatchedLivingEntityRendererMixin"
],
"mixins": [
  "BasicAttackMixin",
  "EntityStateMixin",
  "TechnicianSkillMixin",
  "animation_types_mixin.AnimationPlayerMixin",
  "animation_types_mixin.AttackAnimation",
  "animation_types_mixin.ConcurrentLinkAnimationMixin",
  "animation_types_mixin.DynamicAnimationMixin",
  "animation_types_mixin.MovementAnimationMixin"
]
```

## 5. 修改前后区别

- 项目当前情况(修改前): 客户端一切正常(客户端两份列表都会处理), 但专用服务器加载本 mod 时, mixin 框架会尝试处理一条目标类不存在的必需 mixin, 报错并导致加载失败;
- 修改后: 客户端上 client 列表与原 mixins 列表对该条目的处理结果完全一致, 该 mixin 照常注入, 聚形散气残影渐隐效果不受任何影响; 专用服务器上 client 列表条目被整体跳过, 不再触碰不存在的目标类, 启动不再报错;
- 修改前后区别总结: 只改变"这一条 mixin 在服务端是否被处理", 客户端行为零变化, 双端共用逻辑(其它全部 mixin)零变化。

## 6. 风险与规避

- 风险一: 若未来有人在服务端逻辑里依赖该 mixin 的效果。规避: 该 mixin 只改渲染透明度, 天然只属于客户端, 服务端不存在依赖场景; 若日后要加"服务端同步残影状态"之类的需求, 应走网络包(项目已有 network/packet/client 包的先例), 而不是把渲染 mixin 挪回双端列表;
- 风险二: 条目移动后拼写/包名前缀写错导致客户端也不再生效。规避: client 列表内的条目与包名 com.p1nero.wukong.mixin 下的类名一一对应, 本次仅做整行搬移, 已核对类名 PatchedLivingEntityRendererMixin 与编译产物中的类文件一致, 并已重新打包验证;
- 风险三: 旧配置在部分 mixin 版本上"侥幸不崩"的场合, 移动后会不会反向引入新问题。规避: client 列表是 mixin 官方为"目标仅客户端存在的 mixin"准备的标准登记位置, 不会改变注入优先级或顺序语义, 客户端注入结果与之前一致。

## 7. 具体出处

- 问题条目原文: 修复前 src/main/resources/wukong.mixins.json 的 "mixins" 数组第三项 "PatchedLivingEntityRendererMixin", 配置顶部 "required": true 与 "injectors" 内 "defaultRequire": 1;
- mixin 源码与目标: src/main/java/com/p1nero/wukong/mixin/PatchedLivingEntityRendererMixin.java 第 24 行 @Mixin(value = PatchedLivingEntityRenderer.class, remap = false), 以及同文件 import 的 net.minecraft.client.Minecraft、net.minecraft.client.player.LocalPlayer;
- 目标类的客户端属性: 依赖库 libs/EpicFight-20.14.17.jar 内 yesman/epicfight/client/renderer/patched/entity/PatchedLivingEntityRenderer.class, 经 javap 反编译核实其引用 net/minecraft/client/Minecraft、net/minecraft/client/player/LocalPlayer、net/minecraft/client/renderer/entity/LivingEntityRenderer、net/minecraft/client/renderer/entity/EntityRenderDispatcher 等仅客户端存在的类;
- 规范依据: SpongePowered Mixin 官方配置说明中对 client 列表的定义(目标类仅客户端存在的 mixin 必须登记于 client, 专用服务器跳过该列表);
- 修复产物: build/libs/wukong_compact-forge-1.20.1-1.0.0.jar(已重新编译打包, 包内 wukong.mixins.json 与本文第 4 节修改后内容一致)。

## 8. 遗漏情况说明

- BattleModeGuiMixin(src/main/java/com/p1nero/wukong/mixin/BattleModeGuiMixin.java): 目标类 yesman.epicfight.client.gui.BattleModeGui 同样是 Epic Fight 的客户端类, 但它目前并没有登记在 wukong.mixins.json 的任何列表里, 处于"编译进 jar 但从未被 mixin 框架加载"的搁置状态, 因此不构成服务端崩溃风险; 项目内多个技能类对它的 import 与 {@link} 引用仅存在于注释和未使用的导入, 类内亦无静态成员被调用(已核实)。本次未改动它: 若要启用应登记进 client 列表(属于功能变更), 若确认废弃应删除文件, 均超出本次修复范围, 仅在此说明现状;
- refmap 现象说明, 避免误判: 打包产物 wukong.mixin-refmap.json 中没有 PatchedLivingEntityRendererMixin 的条目, 这是正常的。refmap 只记录需要重映射的原版类引用, 本 mod 除 ItemRendererMixin(目标为原版 net.minecraft.client.renderer.entity.ItemRenderer, 默认 remap = true)外全部 @Mixin(remap = false), 不产生 refmap 条目;
- 兼容性核对: 本修复只影响本 mod 自身的 mixin 登记, 不依赖 Epic Fight 或 Forge 版本变化; 对同存的其它 mod 无任何影响。
