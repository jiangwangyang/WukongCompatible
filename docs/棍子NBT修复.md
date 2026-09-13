# Boss 循环释放第一招(开场招)问题: 原因分析与修复文档

## 1. 问题现象

测试 Boss(以尸壳为壳实体, 手持金箍棒的自定义生物, 数据包见 boss-epicfight/Wukong)时观察到:

1. 用 `/function wukong:summon` 召唤 Boss 后, Boss 不停重复释放第一招 `wukong:biped/greatsage/xuli4`(大圣·四段蓄力重击), 不进行其它招式;
2. 退出存档重进后似乎恢复正常: 开场放一次 xuli4, 然后开始随机正常出招;
3. 但战斗中有时又会开始重复释放 xuli4 循环, 需要再次退出重进。

## 2. 背景知识

### 2.1 Boss 的出招机制(EpicFight mobpatch)

Boss 的招式由 EpicFight 数据包补丁(husk.json)驱动。每个生物的战斗 AI 由一个名为 `CombatBehaviors` 的状态机管理, 内部包含若干招式系列(BehaviorSeries), 每个系列有权重(weight)与冷却(cooldown)。开场招系列的配置是: 权重 999999(首次抽招必然命中) + 冷却 999999 刻(放一次即实质永久禁用), 以此实现"开场只放一次"。

关键点: 冷却只存在于内存中的 `CombatBehaviors` 对象里, 不会写入实体 NBT。每当 EpicFight 执行 `initAI()`(重建 AI), 都会用数据包里的构造器新建一个 `CombatBehaviors`, 所有冷却清零, 开场招立刻又可被抽中。

`initAI()` 的触发时机有三个: 实体加入世界(召唤/重进/区块卸载后重载)、骑乘变化、以及 **手部装备变化**(原版每 tick 检测生物装备栏, 变化时触发 Forge 的 `LivingEquipmentChangeEvent`, EpicFight 收到后调用 `HumanoidMobPatch.updateHeldItem` → `initAI()`)。

### 2.2 原版的装备变化检测

每个生物每 tick 会比较"当前手持物品"与上一 tick 保存的"快照副本"(ItemStack.matches: 比较物品种类/数量/NBT 标签)。不一致则触发装备变化事件, 然后把当前物品深拷贝一份存为新快照。

### 2.3 金箍棒的变大特效是怎么做的

悟空模组的部分招式(如蓄力重击)需要让棍子模型在动画期间变大/位移。旧实现的做法是: 动画播放期间由动画事件把缩放/位移参数(`WK_shouldScaleItem`、`WK_XScale` 等 8 个键)**写进手持物品栈的 NBT**, 渲染时由 `ItemRendererMixin` 读取这些 NBT 键来变换模型, 动画结束时再复位。

## 3. 排查过程与证据

### 3.1 第一次诊断: 确认冷却被反复重置

通过源码通读 EpicFight 20.14.17(并与运行 jar 字节码核对)确认: 单个 `CombatBehaviors` 实例内开场招冷却为 999999 刻, 绝对不可能重复抽中。因此"重复释放"必然来自 `CombatBehaviors` 被反复重建, 即装备变化事件被反复触发。

### 3.2 第二次诊断: 日志实锤装备变化每 tick 触发

在悟空模组临时加入诊断日志(监听装备变化事件并打印变化前后的物品 NBT)后, 日志显示:

- Boss 召唤后, 装备变化事件**每 tick 触发一次**(每秒 20 次), 且 `fromTag={} toTag=null`, 随后快照里出现 `{WK_shouldScaleItem:1b, WK_XScale:1.0f, ...}` 等键;
- 对照组(一只同样手持金箍棒但使用原版补丁的溺尸)只触发了一次装备变化, 不循环。

`WK_*` 系列键只有悟空模组会写, 且全部是**客户端动画事件/渲染器**写入的——但它们却出现在了**服务端**的装备检测快照里。

### 3.3 根因: 单人游戏局域连接下物品栈对象跨端共享

单人游戏(含对局域网开放)使用内置服务器, 服务端向客户端发送数据包时**不经过序列化**, 直接把对象引用递过去。原版装备变化检测在触发事件后会把当前物品**深拷贝**存入快照并广播给客户端; 由于不序列化, 客户端实体的手持物品栈与服务端的快照**是同一个 Java 对象**。

于是形成恶性循环:

1. 客户端渲染/动画事件把 `WK_*` 缩放参数写进自己看到的金箍棒(= 服务端的快照对象);
2. 服务端下一 tick 检测装备: 快照(被客户端写过, 带 WK 键) ≠ 当前手持(服务端没人写过, 无 NBT) → 判定装备变化;
3. EpicFight 收到事件 → `updateHeldItem` → `initAI()` → 战斗 AI 重建 → 开场招冷却清零;
4. 当前招式动画结束后重新抽招, 权重 999999 的开场招必然命中 → Boss 再次释放 xuli4;
5. 广播新快照 → 客户端继续写 → 回到第 2 步, 每 tick 循环。

这解释了全部现象: 召唤即循环; 退出重进后客户端 NBT 状态被重置, 暂时表现正常; 战斗中再次触发客户端写入后复发。

## 4. 修复内容

核心思路: **棍子的缩放/位移状态不再写入物品 NBT, 改为存放在独立的客户端缓存中**, 客户端写入行为与物品栈彻底脱钩, 服务端装备检测不再被污染。

### 4.1 变动功能点一览

| 功能点 | 是什么 | 为什么改 | 有什么作用 |
|---|---|---|---|
| 新增 `client/StaffScaleState.java` | 客户端缓存类: 以物品栈实例为键(WeakHashMap)记录缩放/位移 6 个参数, 提供 set/reset/apply 三个静态方法 | 物品 NBT 在单人局域连接下跨端共享, 不能再用作渲染参数的载体 | 渲染参数有独立的存放处, 客户端写入不再触碰物品栈, 服务端装备检测恢复正常 |
| 修改 `WukongAnimations.getScaleEvents` | 生成缩放动画事件的工厂方法 | 它原先每帧往物品 NBT 写 8 个键, 是污染服务端快照的源头 | 改为写入 `StaffScaleState` 缓存, 首尾事件改为清除缓存 |
| 修改 `WukongScaleStaffAttackAnimation.end()` | 变长棍攻击动画的结束回调 | 它原先在服务端也会复位物品 NBT, 同样会触发装备变化事件 | 改为仅在客户端清除 `StaffScaleState` 缓存; 服务端不再触碰物品 |
| 修改 `mixin/ItemRendererMixin.java` | 物品渲染器的注入点, 负责按参数变换棍子模型 | 原先从物品 NBT 读取缩放/位移参数 | 改为从 `StaffScaleState` 缓存读取并应用变换 |

### 4.2 修改前后对比

| 方面 | 修改前 | 修改后 |
|---|---|---|
| 缩放/位移参数载体 | 手持物品栈的 NBT (`WK_*` 8 个键) | 客户端 WeakHashMap 缓存(按物品栈实例) |
| 服务端是否写物品 NBT | 是(end() 双端执行) | 否(仅客户端清缓存) |
| Boss 战斗 AI | 每 tick 被重建, 冷却失效, 循环释放开场招 | 正常: 开场放一次 xuli4 后随机出招 |
| 棍子变大/位移渲染 | NBT 驱动 | 缓存驱动, 视觉效果一致 |

## 5. 风险与规避

| 风险 | 说明 | 规避/结论 |
|---|---|---|
| 渲染特效失效 | 换了参数载体, 理论上变换逻辑完全等价 | 已按原有语义一一对应迁移(缩放+位移, 首/中/尾事件), 需游戏内目测确认棍子变大/位移特效照常 |
| 缓存残留 | 动画被极端情况打断且客户端未走到复位 | WeakHashMap 键随物品栈对象失效自动回收, 无内存泄漏; `end()` 在打断时也会被调用, 风险很低 |
| 多人游戏兼容性 | 缓存为纯客户端, 不参与存档与网络同步 | 专用服务器上客户端/服务端物品栈本就相互独立, 本次修复消除了唯一的服务端 NBT 写入, 多人行为不变且更干净; 局域网联机与单人同为内置服务器, 修复同样生效 |
| 多持棍实体 | Boss 与玩家可同时持棍 | 缓存按物品栈实例区分, 互不干扰 |

## 6. 机制固有边界(未修复, 不属于本次问题)

EpicFight 的招式冷却不持久化、不耐重建是其机制设计。以下情况开场招仍会再放一次, 属预期边界: Boss 死亡重招、退出重进存档、所在区块卸载后重新加载、真实的手部装备更换。若希望彻底消除, 需修改 EpicFight 本体(冷却写入实体 NBT 或 `initAI` 时复用已有状态机), 超出本次修复范围。

## 7. 出处

| 结论 | 出处 |
|---|---|
| 抽招/冷却机制与冷却在抽中时计时 | EpicFight 20.14.17 源码 `CombatBehaviors.java` / `BehaviorSeries.canBeSelected` / `resetCooldown`(已对运行 jar `EpicFight-20.14.17.jar` 字节码核对一致) |
| 装备变化触发 AI 重建 | EpicFight 源码 `EntityEvents.equipChangeEvent` → `HumanoidMobPatch.updateHeldItem` → `initAI` / `CustomHumanoidMobPatch.setAIAsInfantry` |
| 装备检测与快照深拷贝逻辑 | Forge 47.4.10 补丁后源码 `LivingEntity.detectEquipmentUpdates` / `collectEquipmentChanges` / `handleEquipmentChanges`(build/fg_cache 内 mapped_official 源码) |
| ItemStack.matches 比较 NBT 与 capability 序列化 | Forge 补丁后 `ItemStack.isSameItemSameTags` / `CapabilityDispatcher.areCompatible` |
| 单人局域连接不序列化数据包导致物品栈对象共享 | 日志证据: 服务端装备事件的快照 NBT 中含仅客户端会写的 `WK_*` 键, 且服务端全程无 setTag 调用(诊断 Mixin 证实) |
| `WK_*` 键的原先写入/读取位置 | 本模组 `WukongAnimations.getScaleEvents` / `WukongScaleStaffAttackAnimation.end` / `ItemRendererMixin`(修改前版本) |

## 8. 已知遗漏与后续事项

1. 诊断期间使用的临时日志监听器与诊断 Mixin 已全部移除, 未残留;
2. `WukongChargedAttackAnimation` 中另有物品 NBT 写入(`playing_wk_charged`), 经核实该类从未被实例化(死代码), 不在本次修复范围, 若将来启用需同样改为非 NBT 载体;
3. 修复尚未覆盖 EpicFight 冷却持久化问题(见第 6 节), 如需可另行立项。
