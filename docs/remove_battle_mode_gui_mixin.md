# 删除从未生效的死代码 BattleModeGuiMixin 并清理其引用

## 1. 概述

本 mod(黑风山悟空动作组, 后文简称"本 mod")里有一个名为 BattleModeGuiMixin 的 mixin 类(位于 src/main/java/com/p1nero/wukong/mixin/BattleModeGuiMixin.java), 它的意图是: 玩家使用四套棍势重击技能(劈/戳/立/大圣)时, 取消 Epic Fight 战斗界面(HUD)默认的"武器固有技能图标"绘制, 只显示本 mod 自行绘制的一套画面(棍势图标/棍势层数/冷却进度环/金白红光点特效)。

核实发现两个事实:

1. 它从未被登记进 mixin 配置文件(src/main/resources/wukong.mixins.json 的 client 与 mixins 列表都没有它), 而 mixin 只有登记后才会被加载生效, 因此它是"编译进了 jar 但从未运行过"的死代码;
2. 更重要的是, 在本 mod 当前依赖的 Epic Fight 20.14.17 版本下, 即使把它登记回去也是错的: 它会连本 mod 自定义 HUD 的绘制一起取消掉, 玩家将看不到棍势层数/进度环/光效。本 mod 的自定义画面是通过重写技能的 drawOnGui 方法实现的, 而新版 Epic Fight 里"默认图标绘制"和"调用 drawOnGui"已经是同一个方法内的同一条路径, 无法只取消前者保留后者(详见 2.3 节)。

因此本次选择删除: 删掉 BattleModeGuiMixin.java 本体, 并清理其余 8 个文件中引用它的 10 处 import/注释, 相关注释改写为与实际代码一致的描述。这是一次纯代码清理, 玩家在游戏里看到的界面和操作完全不变。

| 项目 | 内容 |
|---|---|
| 功能点名称 | 删除死代码 BattleModeGuiMixin(未生效的 HUD 图标取消逻辑)及其全部引用 |
| 是什么 | 删除 1 个 mixin 类文件; 删除 3 个文件中的无用 import; 将 7 处方法注释中对 BattleModeGuiMixin 的引用改写为如实描述。不改动任何游戏逻辑代码, 不改动 wukong.mixins.json |
| 为什么 | 该类从未登记进 mixin 配置, 从未生效; 且经核实在新版 Epic Fight(20.14.17)下即便登记也会误伤本 mod 自定义 HUD 的绘制, 属于无用且有害的旧版遗留 |
| 作用 | 消除"代码写着会生效但实际不生效"的表里不一, 避免后人误把它登记回去而弄坏战斗 HUD; 注释与实际行为一致, 降低维护误读 |

## 2. 背景知识

### 2.1 mixin 怎样才会生效

mixin 是一种"字节码手术"机制: 把标了 @Mixin 的增强代码合并进游戏或其它 mod 的既有类里, 不必直接修改那些类的源码。每个 mod 必须在 mixin 配置文件(本 mod 为 src/main/resources/wukong.mixins.json)里逐个登记这些增强类, 没登记的类会被完全忽略——编译不报错, 运行也不加载, 就像不存在一样。这种"在代码里存在、在运行里不存在"的状态, 就是俗称的死代码。

### 2.2 BattleModeGuiMixin 原本想干什么

Epic Fight 在战斗模式下会在屏幕上绘制"武器固有技能"图标。本 mod 给四套棍势重击技能配了一套自制画面(立势图标/棍势层数/冷却进度环/光点特效), 绘制代码写在每个技能类的 drawOnGui 方法里。原版 mod(libs/wukong-forge1.20.1-20.2.0.jar, 针对旧版 Epic Fight)里有一个 BattleModeGuiMixin, 意图是: 遇到这四套重击技能时, 取消 Epic Fight 默认图标的绘制, 让画面只剩自制 HUD。原版 mod 的 mixin 配置里它登记在 client 列表, 是正常生效的。

移植到 Epic Fight 20.14.17 时(提交 e8e2d22), 这份代码被改写成注入新版方法 renderWeaponInnateSkill, 但它的登记条目没有保留下来, 于是变成了死代码。docs/mixin_config_side_fix.md 第 154 行记录过这一"搁置状态"。

### 2.3 为什么在新版下登记回去反而是错的

反编译当前依赖的 Epic Fight 20.14.17(libs/EpicFight-20.14.17.jar)可以核实: 新版的 BattleModeGui.renderWeaponInnateSkill 方法内部没有任何独立的"默认图标绘制"步骤, 它做的事就是检查技能可否绘制, 然后调用技能自己的 drawOnGui 方法。也就是说, "默认图标"本身就是由 drawOnGui 的默认实现画的; 本 mod 的四套重击技能已经把 drawOnGui 完全重写成自制 HUD, 默认实现根本不会被调用。

推论:

- 现状(mixin 未生效)下, 玩家看到的恰好就是正确的自制 HUD, 并不存在"默认图标和自制画面叠加"的界面问题;
- 若把现版 BattleModeGuiMixin 登记进 client 列表, 它会在方法开头(cancel)把整个 renderWeaponInnateSkill 取消掉, drawOnGui 也随之不再被调用——四套重击的棍势层数/进度环/光效会全部消失, 属于功能回退;
- 该类里还有一个"注册名叫 common 的技能"的判断分支, 本 mod 与 Epic Fight 20.14.17 中都不存在叫这个名字的固有技能, 是旧版遗留的无效逻辑。

结论: 这个类在新版架构下没有可用的存在价值, 正确处理是删除, 而不是补登记。

## 3. 修改内容明细

共改动 9 个文件(行号为改动前编号):

| 文件 | 改动前 | 改动后 |
|---|---|---|
| src/main/java/com/p1nero/wukong/mixin/BattleModeGuiMixin.java | 整个文件(44 行, 从未生效) | 删除 |
| ThrustHeavyAttack.java(第 490 行注释) | 一行内挤着两句话: 描述 + "取消原本的绘制在 {@link ...BattleModeGuiMixin}"(见 3.1 节) | 拆成两行: "根据棍式和星级画图" + "本方法完全重写 Epic Fight 默认的技能图标绘制, 战斗模式 HUD 仅显示此自定义画面" |
| SmashHeavyAttack.java(第 418 行注释) | 同上 | 同上 |
| PillarHeavyAttack.java(第 422 行注释) | 同上 | 同上 |
| FashuAnshenfaSkill.java(第 17 行 import, 第 219 行注释) | import + 同样挤在一行、内容却写着"根据棍式和星级画图"的复制错误注释 | 删除 import; 注释改为"根据技能状态绘制自定义技能图标与冷却显示" + 重写说明 |
| FashuDingshenfaSkill.java(第 14 行 import, 第 229 行注释) | 同上 | 同上 |
| ShenfaJuxingsanqiSkill.java(第 219 行注释) | 同 FashuAnshenfa 注释(无 import) | 注释改写同上 |
| ShenfaTongtoutiebiSkill.java(第 200 行注释) | 同上 | 注释改写同上 |
| ShenWaiShenFaSkill.java(第 21 行 import) | 只有 import, 无注释引用 | 删除 import |

注: GreatSageHeavyAttack.java(大圣势)本来就没有引用该类, 未改动。改动统计: 14 行新增, 54 行删除。

### 3.1 为什么注释改动会牵扯到"拆行"

这些文件的历史中文注释经历过一次错误的编码转换, 原本两行的注释被压成了同一个物理行, 行内还残留一个损坏产生的问号, 例如(按乱码显示):

     * 鏍规嵁妫嶅紡鍜屾槦绾х敾鍥?     * 鍙栨秷鍘熸湰鐨勭粯鍒跺湪 {@link com.p1nero.wukong.mixin.BattleModeGuiMixin}

(第一段乱码解码后是"根据棍式和星级画图", 第二段是"取消原本的绘制在")。既然要改写这行注释, 就顺带把它恢复成正常的两行、并用可读的 UTF-8 中文重写, 其余行的乱码注释一律不动(见第 7 节)。

## 4. 修改前后对比

| 项目 | 修改前 | 修改后 |
|---|---|---|
| 玩家可见行为(战斗 HUD/技能图标) | 自定义 HUD 正常显示 | 完全不变 |
| wukong.mixins.json | 未登记 BattleModeGuiMixin(正确) | 不变, 仍是未登记 |
| mixin 包目录 | 含 1 个死代码类 | 只含全部生效的 mixin |
| 技能类注释 | 引用一个不存在的生效点, 与实际行为矛盾 | 与实际行为一致(重写 drawOnGui 即替代默认图标) |
| 编译 | 通过(100 个警告, 全为项目原有的 Epic Fight 过时 API 弃用警告) | 同样通过, 警告集合不变 |
| build 编译产物 | 编译后旧 class 已自动清除 | 无残留 |

## 5. 风险与规避

1. 误以为删错了功能: 该类从未生效, 删除不改变任何运行行为; 已通过编译验证与引用清零检查(grep src/main 无 BattleModeGuiMixin 残留)。规避: 若未来确有"隐藏默认图标"的新需求, 应针对新版 API 重新设计(例如只改 shouldDraw 或重写 drawOnGui), 不要恢复本类。
2. 把它重新登记回 client 列表: 这会让四套重击技能的自定义 HUD 整个消失(原因见 2.3 节)。规避: 本文档已明确说明; wukong.mixins.json 保持不变。
3. 注释改写引入编码问题: 新注释为标准 UTF-8 中文, 与构建配置(options.encoding = UTF-8, build.gradle 第 144 行)一致; 文件原有其它乱码注释原样保留, 未扩大改动面。
4. 引用清理不彻底导致编译失败: 已全量 grep 确认 src/main 下零残留, 且 compileJava 通过。

## 6. 出处

- 死代码判定: src/main/resources/wukong.mixins.json(本仓库当前内容)与 git 历史(自引入本仓库的首个提交 e8e2d22 起即无此条目); docs/mixin_config_side_fix.md 第 154 行当时现状记录;
- 原版登记与原注入目标: libs/wukong-forge1.20.1-20.2.0.jar 内 wukong.mixins.json(client 列表含 BattleModeGuiMixin)及 com/p1nero/wukong/mixin/BattleModeGuiMixin.class 字节码(注入处理器签名 (LocalPlayerPatch, SkillContainer, GuiGraphics, float), 该方法在新版已不存在);
- 新版 drawOnGui 调用链: libs/EpicFight-20.14.17.jar 内 yesman/epicfight/client/gui/BattleModeGui.class 字节码(renderWeaponInnateSkill 末尾 invoke Skill.drawOnGui, 方法内无独立默认图标绘制; renderNormalSkills 亦调用 drawOnGui);
- 自定义 HUD 绘制: src/main/java/com/p1nero/wukong/epicfight/skill/custom/wukong/ 下 ThrustHeavyAttack/SmashHeavyAttack/PillarHeavyAttack/GreatSageHeavyAttack 的 drawOnGui 重写, 以及 src/main/java/com/p1nero/wukong/epicfight/skill/custom/fashu/ 下 5 个法术/身法技能的 drawOnGui 重写;
- 引用位置: 删除前 grep 结果, 即第 3 节表格所列 8 个文件 10 处。

## 7. 遗留事项

1. 上述 8 个文件中还存在其它历史乱码注释(如"娓呯┖鑰愬姏..."等), 与本次改动无关, 为控制改动面未处理; 此前已有先例: 提交 d9edd5e 单独修复过 WukongAnimations.java 的乱码注释, 后续可照此逐文件清理;
2. 4 个法术/身法技能旧注释第一句"根据棍式和星级画图"是从棍势技能复制来的错误描述, 本次已随改写一并更正; 五个法术/身法技能的 shouldDraw/drawOnGui 逻辑值得日后统一梳理;
3. docs/mixin_config_side_fix.md 第 154 行是对当时现状的历史记录, 保留原文未改, 以本文档为最新结论;
4. 本 mod 的 mixin 配置(wukong.mixins.json)本身无需任何改动。
