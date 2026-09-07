# Epic Fight 20.14.17 移植适配修复集: 原因分析与修复文档

## 1. 概述

本 mod(黑风山悟空动作组, 后文简称"本 mod")从旧版 Epic Fight(约 20.8, 对应旧成品 jar wukong-forge1.20.1-20.2.0.jar)移植到 Epic Fight 20.14.17(移植提交 e8e2d22)后, 出现了一批与"API 变更"和"资源路径变更"直接相关的缺陷。本文档汇总记录其中 4 个提交修复的 7 个问题, 统一归档原因分析、修改内容与风险说明:

| 问题 | 对应提交 | 类型 |
|---|---|---|
| 问题一: 切手技(派生攻击)无法释放 | d50e438 | Java API 变更导致的比较失效 |
| 问题二: 技能图标与立势图标缺失 | b315a26 | 资源路径规则变更导致图标加载不到 |
| 问题三: 金箍棒渲染崩溃(NullPointerException) | b315a26 | 未判空调用链 |
| 问题四: 孤立数据文件 ryjgb.json | b315a26 | 移植照搬的冗余数据清理 |
| 问题五: 首次向后闪避失败 | f2fd90f | 动画 JSON 关键帧数据损坏 |
| 问题六: 完美闪避不保留棍势 | f2fd90f | 布尔标志从未置位 + 读取器取反 |
| 问题七: 大圣模式立棍后永久卡死 | b2a6810 | 移植新增的取消蓄力逻辑与新版事件触发点冲突 |

除上述 7 个问题外, 同一批移植修复中的"大圣套装碎片"与"聚形散气瞬移/定字"两类问题因内容独立且篇幅较长, 已分别归档于 docs/dasheng_armor_fix.md 与 docs/jxsq_dash_and_ding_particle_fix.md, 本文不再重复。

## 2. 背景知识

### 2.1 动画句柄(AnimationAccessor)是什么

Epic Fight 里每个动画资源(StaticAnimation)在注册表里有一个"句柄"(AnimationManager.AnimationAccessor)。可以把句柄理解为"存取票": 拿着票可以随时用 get() 换出动画本体。票和动画本体是两种不同的对象, 两者比较(instanceof、equals)结果永远是"否"。

在 Epic Fight 20.8(旧版)中, 从移植前代码的写法可反推出 API 语义: 玩家动作事件返回的是动画本体(旧代码直接写 event.getAnimation() instanceof WukongDodgeAnimation), 武器连段列表取值时需先 get() 换出本体(旧代码写 autoAnimations.get(i).get())。本地没有旧版 Epic Fight 的 jar, 上述为依据旧代码用法的反推; 修复以 20.14.17 的反编译事实为准, 不依赖该反推。

在 Epic Fight 20.14.17(新版)中两者对调并统一(反编译核实):

- ActionEvent.getAnimation() 返回 AnimationAccessor 句柄(其字段声明为 AnimationManager.AnimationAccessor<? extends MainFrameAnimation>);
- CapabilityItem.getAutoAttackMotion(PlayerPatch) 返回 List<AnimationManager.AnimationAccessor<? extends AttackAnimation>>, 即整个列表都改存句柄;
- 句柄接口 AssetAccessor<O> 继承 Supplier<O>, 用 get() 取本体。

### 2.2 技能图标的加载路径规则

Epic Fight 渲染战斗模式技能栏(HUD)与技能学习界面图标时, 调用 Skill.getSkillTexture()。反编译 20.14.17 证实其路径格式为:

    textures/gui/skills/<类别名小写>/<技能注册路径>.png

- 类别名 = 技能所属类别(SkillCategory)的字符串转小写, 如本 mod 自定义的 STAFF_STYLE/HAO_MAO/SHENFA_STYLE/FASHU_STYLE 四类(见 WukongSkillCategories.java), 以及 Epic Fight 自带的 DODGE 类(闪避技能 createDodgeBuilder 中 setCategory(SkillCategories.DODGE));
- 技能注册路径 = 技能注册名(wukong:dodge 之类)冒号后的部分;
- 命名空间沿用技能注册名的命名空间(本 mod 为 wukong)。

旧版 20.8 的规则是扁平的 textures/gui/skills/<技能id>.png(旧成品 jar 内图标全部位于 skills/ 根目录, 已核实)。因此移植后按旧规则存放的图标在新版下一律查不到。

### 2.3 金箍棒物品是怎么画出来的, 什么时候会崩

金箍棒(jingubang)物品模型由 GeckoLib 的 GeoItemRenderer(JinGuBangRenderer)渲染。getTextureLocation()/actuallyRender() 在物品被绘制的所有场合都会执行, 不只在世界里拿在手上, 也包括背包、创造模式物品栏等 GUI 里画物品图标。

渲染时要判断玩家当前棍势来换贴图/调色, 于是需要拿"玩家的 Epic Fight 能力补丁": EpicFightCapabilities.getEntityPatch(mc.player, LocalPlayerPatch.class)。反编译核实该方法的行为: 实体为 null(如主菜单、世界加载阶段)或实体没有 Epic Fight 能力(LazyOptional 为空)时, 返回 null。

### 2.4 武器能力数据文件是怎么被加载的

Epic Fight 20.14.17 用 ItemCapabilityReloadListener 在资源重载时扫描 data/<命名空间>/capabilities/ 下的 armors 与 weapons 子目录(跳过 types 与 item_keyword 两个特殊子目录), 把文件名当作物品注册路径去物品注册表(ForgeRegistries.ITEMS)查: 文件名没有对应物品时记录一条 warn 日志并跳过该文件(反编译核实)。即 data/wukong/capabilities/weapons/jingubang.json 描述的是物品 wukong:jingubang 的战斗属性。

### 2.5 闪避动画 JSON 与关键帧数量校验

本 mod 的闪避动画存放在 src/main/resources/assets/wukong/animmodels/animations/biped/dodge/ 下, 每个文件描述若干骨骼(bone), 每根骨骼有两组等长的数组: time(关键帧时间点)与 transform(每个时间点对应的 16 元素变换矩阵行)。反编译 Epic Fight 20.14.17 的 JsonAssetLoader.getTransformSheet 证实: 解析每根骨骼时先取 time 与 transform 两个数组并比较长度, 不相等直接抛出 AssetLoadingException 拒绝加载。动画数据是"首次使用时才解析"的(StaticAnimation 的 animationClip 字段按需填充), 所以损坏的文件要等到玩家第一次用到该动作时才会暴露。

### 2.6 棍势与完美闪避机制

- 棍势: 重击类技能的资源(SkillContainer 的 stack 层数), 蓄力(IS_CHARGING / Thrust_IS_CHARGING)期间积累, 释放重击消耗;
- 蓄力中闪避: 原设计是"蓄力中做闪避动作时清空棍势", 但完美闪避例外;
- 完美闪避: 在敌人攻击即将命中的瞬间闪避成功。Epic Fight 会在这种闪避上触发 DODGE_SUCCESS_EVENT(闪避成功事件);
- 判定链路(修复后的完整流程): 闪避动画开始(WukongDodgeAnimation.begin)时服务端把玩家能力数据 WKPlayer 的 perfectDodge 标志重置为 false → 若这次闪避触发了 DODGE_SUCCESS_EVENT(WukongDodgeSkill.onInitiate 监听), 把 perfectDodge 置为 true → 闪避动画播放到 delayTime 时刻(WukongDodgeAnimation 内注册的 InTimeEvent, 服务端执行)时, 若玩家正在蓄力且 perfectDodge 为 false, 才清空棍势。

### 2.7 立棍蓄力流程与 ACTION_EVENT 的触发时机

大圣(greatsage)与立棍(pillar)两套重击技能共用的"站棍蓄力"动画链, 动画定义均在 WukongAnimations.java, 由 WukongSkills.java 的两个技能 build 分别引用:

- 起手 PILLAR_START0..4: 播放结束时(ON_END_EVENTS, 服务端执行)reserve 下一动画, 并给 WEAPON_INNATE 技能的数据管理器置 IS_CHARGING=true;
- 循环 PILLAR_LOOP0(起手 0..3 衔接)与 PILLAR_CHARGED_LOOP4(起手 4 衔接): 均为 ActionAnimation, ON_END 里 reserve 自身形成无限自循环, 是"站棍蓄力"的常态载体(NO_GRAVITY_TIME/MOVE_VERTICAL 把人物固定在棍顶, 实测每次自循环回卷约 4 秒);
- 升星 PILLAR_UP: 蓄力中星级提升时插入播放, 结束后同样 reserve 回 PILLAR_LOOP0;
- 出口: 技能侧 updateContainer 每 tick 检查 KEY_PRESSING(客户端每 tick 同步的技能键按下状态), 松键时播放 PILLAR_HEAVY0..4 下棍重击。

反编译 20.14.17 核实的事件触发点: MainFrameAnimation.begin(LivingEntityPatch) 在每个 MainFrame 动画(含其子类 ActionAnimation)开始播放时触发 ACTION_EVENT_CLIENT(逻辑客户端的本地玩家)或 ACTION_EVENT_SERVER(其余场合); ServerAnimator.tick() 在当前动画结束、切换到 reserve 动画的时机调用 DynamicAnimation.begin()。因此"reserve 自动衔接"的动画开始播放同样会触发 ACTION_EVENT_SERVER, 与玩家主动发起的动作在事件层面不可区分。

对照: 劈棍蓄力的常态动画 SMASH_CHARGING_LOOP_STAND 是普通 StaticAnimation(非 MainFrameAnimation), 其 begin() 不触发 ACTION_EVENT。这是"劈棍蓄力 + 蓄力中做其它动作就取消"的监听器能共存, 而"立棍(复用 ActionAnimation 自循环) + 同款取消监听器"出问题的结构性原因。

## 3. 问题现象

1. 切手技无法释放: 拿金箍棒普攻后按右键, 无法触发切手/派生重击(四套重击: 劈棍 smash / 戳棍 thrust / 立棍 pillar / 大圣 greatsage 全部失效);
2. 技能图标缺失: 战斗模式 HUD 与技能界面里, 本 mod 全部技能图标显示为紫黑棋盘格(资源缺失占位图); 另外 HUD 左下方的"立势图标"在特定状态下同样缺失;
3. 金箍棒渲染崩溃: 打开背包等 GUI 场景时偶发崩溃, 报 NullPointerException, 位置在 JinGuBangRenderer;
4. 每次资源重载(进存档/按 F3+T)日志出现 ryjgb 物品不存在的 warn;
5. 首次向后闪避失败: 玩家第一次使用向后闪避时动作无法播放;
6. 完美闪避不保留棍势: 完美闪避本应保留已蓄的棍势, 实际同样被清空;
7. 大圣模式立棍后永久卡死: 大圣势下普攻 4 段接特殊技(武器 innate 技能键), 棍子立地、人物爬上棍顶后永久无法操作(攻击/闪避/移动均无响应), 只能退出重进。

## 4. 原因分析

### 4.1 问题一: 切手技无法释放 —— 句柄与本体比较方向弄反

四套重击技能(GreatSageHeavyAttack/PillarHeavyAttack/SmashHeavyAttack/ThrustHeavyAttack)依赖 ACTION_EVENT_SERVER(玩家动作事件)做两件事(代码位于各自文件的事件监听内):

1. 派生检测: 事件携带的动画若出现在武器连段列表(getAutoAttackMotion)中, 说明玩家刚完成普攻动作, 此时设置派生窗口计时器(DERIVE_TIMER/CAN_FIRST_DERIVE 等), 此后短时间按右键即可切手;
2. 蓄力保护: 若玩家正在蓄力(IS_CHARGING), 而当前动作既不是蓄力起手动画(chargePre)也不是闪避动画(WukongDodgeAnimation), 则取消蓄力。

按 2.1 节的 API 变更核对移植前的代码, 两处比较方向都停留在旧版语义, 在 20.14.17 下永远为"否":

- 连段比较 autoAnimations.get(i).get().equals(event.getAnimation()): 左边是"从票解包出的动画本体", 右边是"票"。本体和票 equals 永远 false → 派生检测永不命中 → 切手技无法释放(核心症状);
- 闪避豁免 event.getAnimation() instanceof WukongDodgeAnimation: 票永远不是动画本体的实例 → 豁免永不生效 → 蓄力中闪避会被误判为"其它动作"而取消蓄力(次要症状)。

### 4.2 问题二: 技能图标缺失 —— 扁平路径不满足新版分类路径规则

移植时资源文件按旧版 20.8 的扁平规则存放在 textures/gui/skills/<技能id>.png(与旧 jar 一致), 而 20.14.17 按 2.2 节的"类别/技能id"两级规则查找, 全部落空, 图标显示为紫黑棋盘格。

需要补的图标与技能类别对照(类别名已对照 WukongSkillCategories.java 与 createDodgeBuilder 核实):

| 新增图标文件 | 技能(注册路径) | 类别(路径第一段) |
|---|---|---|
| skills/dodge/dodge.png | dodge(闪避) | dodge(Epic Fight 自带) |
| skills/fashu_style/spell_asf.png | spell_asf(安身法) | fashu_style(奇术) |
| skills/fashu_style/spell_dsf.png | spell_dsf(定身法) | fashu_style(奇术) |
| skills/hao_mao/spell_swsf.png | spell_swsf(身外身法) | hao_mao(毫毛) |
| skills/shenfa_style/spell_jxsq.png | spell_jxsq(聚形散气) | shenfa_style(身法) |
| skills/shenfa_style/spell_tttb.png | spell_tttb(铜头铁臂) | shenfa_style(身法) |
| skills/staff_style/smash_style.png | smash_style(劈棍势) | staff_style(棍势) |
| skills/staff_style/thrust_style.png | thrust_style(戳棍势) | staff_style(棍势) |
| skills/staff_style/pillar_style.png | pillar_style(立棍势) | staff_style(棍势) |
| skills/staff_style/greatsage_style.png | greatsage_style(大圣势) | staff_style(棍势) |

立势图标(staff_stack/stance)的情况类似但成因不同: 四套重击技能的 drawOnGui(画 HUD)中, 劈/戳/立三套用动态路径 textures/gui/staff_stack/stance/<势编号>_0.png, 其中势编号 = 当前势的 id 减去 SMASH 的 id(WukongStyles 枚举声明顺序为 SMASH/THRUST/PILLAR/GREATSAGE, 编号依次 0/1/2/3, 见 WukongStyles.java, 故大圣势编号为 3); 旧成品 jar 中大圣势的 HUD 图标不走该动态路径, 而是旧版 GreatSageHeavyAttack 字节码中硬编码的 textures/gui/staff_stack/stance/greatsage_style.png(反编译核实), 因此旧 jar 的 stance 目录只有 0_0/0_1/1_0/1_1/2_0/2_1 而没有 3_0。移植后凡动态路径计算出 3 的场合都会找不到文件, 本次修复补入 3_0.png。

### 4.3 问题三: 金箍棒渲染 NPE —— 对可能为 null 的玩家补丁直接链式调用

修复前 JinGuBangRenderer 有两处(约 64 行 getTextureLocation 与约 89 行 actuallyRender)写成:

    SkillContainer containe = EpicFightCapabilities.getEntityPatch(mc.player, LocalPlayerPatch.class).getSkill(WukongSkillSlots.STAFF_STYLE);

按 2.3 节, getEntityPatch 在 mc.player 为 null(主菜单/加载界面渲染物品图标)或玩家没有 Epic Fight 能力补丁时返回 null, 对 null 直接调 getSkill 即抛 NullPointerException。

### 4.4 问题四: 孤立数据文件 ryjgb.json —— 旧命名遗留, 新版加载器逐文件告警

旧成品 jar 的 data/wukong/capabilities/weapons/ 下同时存在 jingubang.json 与 ryjgb.json, 两者内容完全相同(attributes: armor_negation 20 / impact 0.6 / max_strikes 2, type: wukong:wk_staff, 逐字节比对核实)。ryjgb 是"如意金箍棒"的旧拼音缩写命名, 该命名下并无对应物品: 当前项目内对 ryjgb 零引用(grep 核实), 物品注册名与语言文件均为 wukong:jingubang(item.wukong.jingubang = 如意金箍棒)。移植时把这份孤儿文件原样带入, 20.14.17 的加载器(见 2.4 节)在每次资源重载时都会因"文件名无对应物品"记一条 warn, 属于纯噪音。

### 4.5 问题五: 首次向后闪避失败 —— dodge_b1.json 关键帧数组数量不匹配

dodge_b1.json 是向后闪避动画。用脚本逐骨骼统计修复前该文件的数组(结果核实):

    Root: time 31 个值, transform 32 行  <== 不匹配
    其余 19 根骨骼(Thigh_R/Leg_R/.../Tool_L, 合计 20 根): 全部匹配

Root 骨骼 transform 数组末尾连续两行完全相同(重复数据):

    [1.0, -0.0, -0.0, -0.499933, -0.0, -0.0, -1.0, -4.052178, 0.0, 1.0, -0.0, 0.60816, 0.0, 0.0, 0.0, 1.0]

按 2.5 节, time 与 transform 数量不相等时 Epic Fight 在解析 Root 骨骼时抛 AssetLoadingException, 动画加载失败, 玩家第一次后闪(即首次触发该动画解析)时表现为闪避失败。

与旧成品 jar 内同名文件逐字节比对的结果是: 修复前文件与旧版资源完全一致 —— 该损坏并非移植引入, 而是上游旧版资源本身携带(旧 jar 内同样是 time 31/transform 32, 末尾同样是内容相同的两行)。之所以旧版没有暴露, 合理解释是旧版(约 20.8)解析行为宽松, 但本地没有旧版 Epic Fight 的 jar, 这一推断无法像 20.14.17 一样反编译核实; 好在修复不依赖该推断: 删除重复行后数据在任意版本下都是自洽的(见 5.5 节)。

### 4.6 问题六: 完美闪避不保留棍势 —— 标志从未置位叠加读取器取反

按 2.6 节的判定链路核对修复前的代码, 发现两个叠加缺陷:

1. 标志从未置位: git 全量检索修复前代码, setPerfectDodge 只有两处 —— 定义处与 WukongDodgeAnimation.begin() 里的 setPerfectDodge(false)(每次闪避开始重置)。DODGE_SUCCESS_EVENT 监听里没有任何代码把标志置为 true, 即 perfectDodge 恒为 false;
2. 读取器取反: WKPlayer.isPerfectDodge() 写成了 return !perfectDodge; 。

两者叠加后: 修复前清空棍势的条件写作 (正在蓄力) && wkPlayer.isPerfectDodge() && !isPerfect, 由于"恒 false 的标志 + 取反读取器", isPerfectDodge() 恒为 true, 条件退化为"只要蓄力中闪避就清空棍势"。其中"取反读取器 + 正向判断"的写法对普通闪避恰好像是对的(双重否定抵消), 这也是该缺陷不易从单次测试中发现的原因; 但因标志从未置 true, 完美闪避同样被清空, 与设计(完美闪避保留棍势)不符。

另: WukongDodgeAnimation 有带 isPerfect 参数的五参构造器(4 参构造默认 false), 是动画层面的第二道豁免; 当前 WukongAnimations.java 中注册的全部闪避动画(DODGE_F1~L2, DODGE_F3/B3 等)都用四参构造, isPerfect 恒 false, 因此实际判定完全依赖 perfectDodge 标志。

### 4.7 问题七: 大圣模式立棍后永久卡死 —— 自动衔接的循环动画触发了"其它动作"取消蓄力

按 2.7 节的动画链与事件触发点, 复盘卡死过程(编号对应时间顺序):

1. 普攻 4 段后按特殊技, GreatSageHeavyAttack.executeOnServer 命中"combo==3"分支, 置 GREATSAGE_PILLAR=true 并播放 PILLAR_START0..3(按当前星级选档), 玩家看到棍子立地、人物爬棍;
2. PILLAR_START 播放结束, ON_END 里 reserve PILLAR_LOOP0 并置 IS_CHARGING=true;
3. 下一帧 ServerAnimator.tick 切换到 PILLAR_LOOP0 时调用其 begin(), 触发 ACTION_EVENT_SERVER。移植时在 GreatSageHeavyAttack 的该事件监听器里新加了一段取消逻辑(与 SmashHeavyAttack 同款; 反编译旧成品 jar 证实原版 GreatSageHeavyAttack 的监听器没有这段): "IS_CHARGING 为 true 且当前动画不是 chargePre 且不是闪避动画, 则 cancelCharge()"。PILLAR_LOOP0 三条都不满足豁免, 于是 IS_CHARGING 被置回 false, 棍势被清空;
4. updateContainer 中"松开技能键 → 播放 PILLAR_HEAVY 下棍"的唯一出口以 IS_CHARGING 为前提, 从此永不可达; 而 PILLAR_LOOP0 无限自循环把人物锁在棍顶, 即为卡死。

为什么劈棍蓄力、普通立棍模式没有此问题: 劈棍的常态蓄力动画 SMASH_CHARGING_LOOP_STAND 是普通 StaticAnimation, begin() 不触发 ACTION_EVENT(见 2.7 节对照); PillarHeavyAttack 的 ACTION_EVENT 监听器没有取消逻辑。大圣 = 复用立棍的 ActionAnimation 自循环 + 套用劈棍式的取消监听器, 两者组合才触发。

第一次修复的回归(诊断日志抓到): 第一次修复在取消条件中排除立棍动画, 但对 PILLAR_LOOP0 写成了 WukongAnimations.PILLAR_LOOP0.get().equals(animation)。WukongAnimations 中 PILLAR_LOOP0/PILLAR_CHARGED_LOOP4 字段本身就是 AnimationAccessor 句柄(与 4.1/5.1 节的 StaticAnimationProvider 不同, 它们不是"返回句柄的提供者"), 在句柄上调 get() 返回的是动画本体, "本体 vs 句柄"的比较永远 false。诊断日志显示 pillar_start0 的 pillarFlow=true(经由 pillarStartAttacks 数组命中)而 pillar_loop0 的 pillarFlow=false, 取消逻辑照旧触发。该比较语义正是 2.1 节"票和本体比较永远为否"的又一次踩坑, 只是这次反在了提供者一侧。

现场证据(临时诊断日志, 已随修复移除): ACTION_EVENT anim=pillar_loop0 charging=true 后紧跟 cancelCharge fired by ACTION_EVENT; 蓄力期间无任何 charging tick 日志(IS_CHARGING 在置位同帧即被清空); 约 4 秒后循环回卷再次触发同一事件(charging 已为 false)。

## 5. 修改内容

共 4 个提交, 涉及 8 个 Java 文件(4 个重击技能 + JinGuBangRenderer + WKPlayer + WukongDodgeSkill + WukongDodgeAnimation)、11 张贴图与 1 个被删除的数据文件。以上均为已合入 git 历史的既成提交, 本次任务仅补充文档, 未改动其中任何内容。

### 5.1 比较方向改为新版句柄语义(问题一)

文件: GreatSageHeavyAttack.java(2 处) / PillarHeavyAttack.java(1 处) / SmashHeavyAttack.java(2 处) / ThrustHeavyAttack.java(1 处)

- 是什么: 把"从句柄解包后与句柄比较"改为"需要本体时先 .get() 解包, 句柄间直接比较"。即 event.getAnimation() instanceof WukongDodgeAnimation 改为 event.getAnimation().get() instanceof WukongDodgeAnimation; autoAnimations.get(i).get().equals(event.getAnimation()) 改为 autoAnimations.get(i).equals(event.getAnimation());
- 为什么: 20.14.17 中事件返回句柄、连段列表存句柄(见 2.1 节), 旧写法两边类型不匹配, 比较恒为否;
- 作用: 恢复派生窗口的设置, 切手技可以正常释放; 同时恢复蓄力中的闪避豁免, 蓄力中闪避不再被误取消。

### 5.2 按新版规则补齐图标资源(问题二)

- 是什么: 新增 4.2 节表格中的 10 张技能图标(从 wukong/textures 与旧成品 jar 的同内容贴图按新路径规则放置), 以及 textures/gui/staff_stack/stance/3_0.png(与 skills/staff_style/greatsage_style.png 为同内容副本, MD5 0565a7ab44df8e3e28f30ee5632c23f1 一致);
- 为什么: 新版 getSkillTexture() 按类别两级路径查找(见 2.2 节); 立势图标动态路径在大圣势(编号 3)下缺图;
- 作用: 战斗模式 HUD 与技能界面图标恢复正常; 大圣势立势图标不再缺失;
- 说明: 旧的扁平图标(textures/gui/skills/spell_asf.png 等)有意保留 —— 它们仍被技能内部代码按硬编码路径引用(如 ShenfaJuxingsanqiSkill.java 约第 229 行的 styleTexture 取 textures/gui/skills/spell_jxsq.png, 五处法术/身法技能同理), 删除会引入新的缺失。

### 5.3 玩家补丁判空(问题三)

文件: JinGuBangRenderer.java(2 处)

- 是什么: 两处改为 SkillContainer containe = lpp == null ? null : lpp.getSkill(WukongSkillSlots.STAFF_STYLE); 并配注释"GUI 渲染时玩家 patch 可能不存在, 需要判空";
- 为什么: getEntityPatch 在 GUI 场景/无能力补丁时返回 null(见 2.3 节), 后续已有 containe != null 分支, 补 null 后自然走默认贴图/默认颜色;
- 作用: 消除 NullPointerException 崩溃。

### 5.4 删除孤立数据文件(问题四)

- 是什么: 删除 src/main/resources/data/wukong/capabilities/weapons/ryjgb.json;
- 为什么: 无对应物品的孤儿文件(见 4.4 节), jingubang.json 已完整承担同一物品的属性声明;
- 作用: 消除每次资源重载的 warn 噪音; 数据目录中 weapons 下仅剩 jingubang.json/kang_jin.json/staff.json 三个有效文件。

### 5.5 修复动画关键帧数据(问题五)

文件: src/main/resources/assets/wukong/animmodels/animations/biped/dodge/dodge_b1.json

- 是什么: 删除 Root 骨骼 transform 数组末尾重复的一行(4.5 节所列内容), 使 time 31 个值与 transform 31 行恢复一一对应;
- 为什么: 数量不匹配会被 20.14.17 的解析器直接拒绝(见 2.5 节);
- 数据核对: 修复后保留的 31 帧与旧成品 jar 同名文件 32 帧中的前 31 帧逐行完全一致, 时间轴(0.0 ~ 1.2916)不变, 即本次修复只移除了末尾的重复帧, 未改动任何有效关键帧数值;
- 作用: 向后闪避动画可正常加载与播放。

### 5.6 完美闪避标志修复(问题六)

文件: WKPlayer.java / WukongDodgeSkill.java / WukongDodgeAnimation.java

- 是什么: 1) isPerfectDodge() 改回 return perfectDodge; 2) WukongDodgeSkill.onInitiate 的 DODGE_SUCCESS_EVENT 监听中, 在播完美闪避音效前增加 player.getCapability(WKCapabilityProvider.WK_PLAYER).ifPresent(wkPlayer -> wkPlayer.setPerfectDodge(true)); 3) WukongDodgeAnimation 清棍势条件改为 (正在蓄力) && !wkPlayer.isPerfectDodge() && !isPerfect, 并配注释"非完美闪避才清空棍势, 完美闪避保留棍势";
- 为什么: 见 4.6 节, 三处改动分别对应"读取器取反""标志从未置位""条件语义反转"三个缺陷;
- 作用: 完美闪避(触发 DODGE_SUCCESS_EVENT)时不清棍势; 普通闪避照旧清空; begin() 每次闪避开始重置标志的既有逻辑不变, 保证标志只在本次闪避窗口内有效。

### 5.7 立棍流程动画豁免取消蓄力(问题七)

文件: GreatSageHeavyAttack.java(提交 b2a6810)

- 是什么: ACTION_EVENT_SERVER 监听器的取消条件增加 !isPillarFlowAnimation(event.getAnimation()); 新增私有方法 isPillarFlowAnimation(AnimationManager.AnimationAccessor<? extends MainFrameAnimation>): 逐项比较 pillarStartAttacks(PILLAR_START0..4)与 pillarUp 的句柄, 并与 WukongAnimations.PILLAR_LOOP0/PILLAR_CHARGED_LOOP4 两个句柄字段直接 equals; 相应新增 3 个 import;
- 为什么: 见 4.7 节。立棍起手/升星/循环动画由流程自动衔接, 不是玩家主动动作, 不应触发"其它动作取消蓄力";
- 作用: 大圣模式立棍蓄力(含升星)不再被误取消, 松键可正常下棍重击; 立棍蓄力中玩家主动普攻/非闪避技能仍会取消蓄力(原设计保留), 闪避豁免不变; 劈棍/戳棍/普通立棍均不经过该监听器, 不受影响。

## 6. 修改前后区别

| 场景 | 修改前 | 修改后 |
|---|---|---|
| 普攻后右键切手 | 派生检测永不命中, 无法切手 | 派生窗口正常开启, 切手技可释放 |
| 蓄力中闪避 | 闪避豁免失效, 蓄力被误取消 | 闪避豁免生效, 蓄力保留 |
| 战斗模式 HUD/技能界面图标 | 全部紫黑棋盘格 | 全部正常显示 |
| 大圣势立势图标 | 动态路径缺图, 紫黑棋盘格 | 正常显示(与旧版同图) |
| 背包等 GUI 场景渲染金箍棒 | 可能 NullPointerException 崩溃 | 判空后按默认贴图/颜色渲染 |
| 资源重载日志 | 每次出现 ryjgb 物品不存在的 warn | 无该 warn |
| 首次向后闪避 | 动画解析抛异常, 闪避失败 | 动画正常播放 |
| 蓄力中完美闪避 | 棍势被清空 | 棍势保留 |
| 蓄力中普通闪避 | 棍势被清空 | 棍势被清空(设计如此, 不变) |
| 大圣普攻 4 段接特殊技立棍 | 蓄力被自动衔接的循环动画误取消, 人物永久卡在棍上 | 蓄力正常进行, 松键正常下棍重击 |
| 大圣立棍蓄力中升星 | PILLAR_UP 同样触发误取消, 蓄力中断 | 正常升星, 蓄力不中断 |
| 大圣立棍蓄力中普攻/非闪避技能打断 | 取消蓄力(设计) | 不变 |
| 劈棍/戳棍/普通立棍模式蓄力 | 正常 | 不变(不经过该监听器) |
| 旧扁平图标(被技能内部代码引用) | 存在 | 保留不动 |
| 立棍衔接等与本次无关的逻辑 | - | 未触碰 |

## 7. 风险与规避

1. 句柄比较语义风险: autoAnimations.get(i).equals(event.getAnimation()) 依赖 AnimationAccessor 的 equals 实现。AnimationAccessorImpl 的 equals 语义未逐字节核实, 但修复提交后切手技恢复(已由玩家实测确认, 见问题记录), 说明该比较在 20.14.17 下成立; 若未来升级 Epic Fight 后切手再次失灵, 优先复查该 equals 语义是否变化;
2. 图标双份维护成本: 同一张图同时存在扁平路径(技能内部硬编码引用)与类别路径(新版 getSkillTexture 引用)两份。风险是后续改图只改一份导致不一致。规避: 更换贴图时两处同步替换, 或后续统一改造技能内部引用为 getSkillTexture();
3. 3_0.png 为副本文件: 与 greatsage_style.png 内容相同。风险同上, 更换大圣势图标时两处同步;
4. 判空的静默降级: lpp 为 null 时金箍棒按默认贴图渲染, 不再崩溃但也"看不到势特效", 这与旧版行为一致(旧版在这些场景直接崩溃), 属可接受降级;
5. 删除 ryjgb.json 的兼容性: 若有旧存档/外部数据包显式引用 wukong:ryjgb 能力(未发现此类引用, 项目内零引用), 将回退为默认武器能力。规避: 若日后发现第三方资源包引用, 按其需求补回对应物品的真实命名文件即可;
6. 完美闪避窗口边界: perfectDodge 标志在 begin() 重置、DODGE_SUCCESS_EVENT 置位、InTimeEvent 判定, 三者都在同一次闪避动画生命周期内, 理论上不存在跨次闪避的标志残留; 若实测出现"上一次完美闪避影响下一次", 应检查 DODGE_SUCCESS_EVENT 与动画 begin 的先后顺序(以服务端时序为准);
7. dodge_b1.json 为手工删行修复: 已逐行核实修复后 31 帧与旧版资源前 31 帧完全一致(只移除重复帧, 无数值改动), 并以脚本核对全部 20 根骨骼 time/transform 数量匹配; 风险仅剩未做游戏内实测(见第 9 节第 6 条);
8. 立棍豁免列表的维护成本: isPillarFlowAnimation 依赖 pillarStartAttacks/pillarUp 两个技能字段与 WukongAnimations.PILLAR_LOOP0/PILLAR_CHARGED_LOOP4 两个静态句柄。若日后给大圣换用新的立棍循环/起手动画, 必须同步把新句柄加入该方法, 否则卡死复发; 引入新立棍衔接动画时同理;
9. 句柄 equals 语义(与风险 1 同源): AnimationAccessorImpl 的 equals 实现未逐字节核实, 但修复已由玩家实测(立棍蓄力/升星/松键下棍正常)确认成立; 若未来升级 Epic Fight 后立棍再次卡死, 优先复查该 equals 语义与 MainFrameAnimation.begin 的事件触发范围;
10. PILLAR_HEAVY 有意未加入豁免列表: 松键下棍时服务端先把 IS_CHARGING 置 false 再播放下棍动画, 事件触发时蓄力标志已清除, 无需豁免; 若日后把释放顺序改为"先播放后清标志", 需重新评估此处;
11. 立棍起手约 1 秒的窗口期内重复按特殊技, executeOnServer 会因 IS_CHARGING 尚未置 true 而走 chargePre 分支, 把立棍起手替换为站桩蓄力并清掉 GREATSAGE_PILLAR。该行为与旧版一致, 本次未改动; 若要"起手期间忽略重复按键", 可后续单独处理。

## 8. 内容出处

- Epic Fight 20.14.17(反编译 libs/EpicFight-20.14.17.jar):
  - yesman/epicfight/world/entity/eventlistener/ActionEvent: getAnimation() 返回 AnimationManager.AnimationAccessor<? extends MainFrameAnimation>;
  - yesman/epicfight/api/asset/AssetAccessor: 继承 Supplier<O>, 定义 get() 解包;
  - yesman/epicfight/world/capabilities/item/CapabilityItem: getAutoAttackMotion(PlayerPatch) 返回 List<AnimationAccessor<? extends AttackAnimation>>;
  - yesman/epicfight/skill/Skill.getSkillTexture(): 路径格式 textures/gui/skills/%s/%s.png, 两参数为类别名小写与技能注册路径;
  - yesman/epicfight/api/data/reloader/ItemCapabilityReloadListener: 扫描 capabilities/ 下 armors 与 weapons 子目录, 文件名映射物品注册名, 无对应物品时 warn 并跳过;
  - yesman/epicfight/api/asset/JsonAssetLoader.getTransformSheet: time 与 transform 数组长度校验, 不等抛 AssetLoadingException;
  - yesman/epicfight/api/animation/AnimationManager.loadAnimationClip 与 StaticAnimation: 动画 clip 按需加载, 失败包装后重新抛出;
  - yesman/epicfight/world/capabilities/EpicFightCapabilities.getEntityPatch: 实体为 null/无能力/类型不符时返回 null;
  - yesman/epicfight/api/animation/types/MainFrameAnimation.begin: 每个 MainFrame 动画(含 ActionAnimation 子类)开始播放时触发 ACTION_EVENT_CLIENT(逻辑客户端本地玩家)或 ACTION_EVENT_SERVER(其余);
  - yesman/epicfight/api/animation/ServerAnimator.tick: 当前动画结束切换到 reserve 动画时调用 DynamicAnimation.begin(), 即 reserve 衔接的动画同样触发上述事件;
  - yesman/epicfight/api/animation/types/StaticAnimation.end: 触发 ON_END_EVENTS 属性中注册的事件;
  - yesman/epicfight/skill/SkillDataKey.createSkillDataKey: 第三参数为 syncronizeTrackingPlayers(服务端向旁观者自动同步), 与客户端→服务端的键位同步无关;
  - yesman/epicfight/network/client/CPModifySkillData.handle: 客户端 KEY_PRESSING 同步包在服务端 setDataRawtype 无校验直写(已核实同步链路完好, 排除该嫌疑);
  - yesman/epicfight/world/capabilities/entitypatch/player/PlayerPatch.tick 与 SkillContainer.update: 服务端与本地客户端每 tick 均调用 updateContainer(已核实, 排除"客户端不更新"嫌疑)。
- 旧成品 jar(反编译与内容列举 libs/wukong-forge1.20.1-20.2.0.jar): 图标全部位于 skills/ 根目录(扁平规则); GreatSageHeavyAttack 字节码硬编码 textures/gui/staff_stack/stance/greatsage_style.png; stance 目录无 3_0.png; capabilities/weapons/ 同时含 jingubang.json 与 ryjgb.json 且内容相同; 原版 GreatSageHeavyAttack 的 ACTION_EVENT_SERVER 监听器只有派生检测, 没有取消蓄力逻辑(证明该取消逻辑为移植期新增)。
- 本项目源码(修复前后的 git 提交 d50e438/b315a26/f2fd90f/b2a6810 及当前工作区): WukongSkillCategories.java(四类别定义), WukongStyles.java(枚举顺序即势编号), WukongDodgeSkill.java(createDodgeBuilder 设定 DODGE 类别, DODGE_SUCCESS_EVENT 监听), WukongAnimations.java(闪避动画注册, 816~825 行均为四参构造; PILLAR_START0..4/PILLAR_LOOP0/PILLAR_UP/PILLAR_CHARGED_LOOP4 的 ON_END 衔接与 IS_CHARGING 置位, SMASH_CHARGING_LOOP_STAND 为普通 StaticAnimation 的对照), WKPlayer.java(perfectDodge 字段与读写器), WukongDodgeAnimation.java(begin 重置与 InTimeEvent 清棍势判定), JinGuBangRenderer.java(两处判空), ThrustHeavyAttack/SmashHeavyAttack/PillarHeavyAttack/GreatSageHeavyAttack.java(HUD 立势图标路径与派生检测), ShenfaJuxingsanqiSkill.java 等五处 styleTexture 硬编码引用(扁平图标需保留的依据), src/main/resources/assets/wukong/animmodels/animations/biped/dodge/dodge_b1.json(重复行位置), src/main/resources/data/wukong/capabilities/weapons/(现存三个数据文件), assets/wukong/lang/*.json(item.wukong.jingubang = 如意金箍棒), WukongSkills.java(大圣与立棍两个技能 build 共用 PILLAR 系动画), GreatSageHeavyAttack.java(提交 b2a6810 的豁免方法与取消条件), PillarHeavyAttack.java/SmashHeavyAttack.java(取消逻辑有无的对照组)。
- 校验手段: 3_0.png 与 greatsage_style.png 的 MD5(0565a7ab44df8e3e28f30ee5632c23f1)一致; dodge_b1.json 修复前后逐骨骼数组计数脚本比对(修复前 Root 31/32, 其余 19 根匹配; 修复后 20 根全部匹配), 修复后 31 帧与旧成品 jar 前帧逐行一致(仅去重复帧), 并全库扫描 117 个动画 JSON 确认无其它不匹配; 修复前 dodge_b1.json 与旧成品 jar 同名文件逐字节一致(证明损坏源自上游旧版资源); 问题七以临时诊断日志([GS-DIAG], 已随修复移除)抓取现场: pillar_start0 begin 时 pillarFlow=true 而 pillar_loop0 begin 时 pillarFlow=false 并触发 cancelCharge, 蓄力期间无 charging tick/RELEASE 日志, 循环每约 4 秒回卷重复触发; 修复后玩家实测立棍蓄力/升星/松键下棍正常。

## 9. 遗漏情况说明

1. 旧成品 jar 内的 textures/gui/skills/dzsf_style.png 在当前项目技能列表中无对应技能, 未迁移, 也不影响现有功能; 若日后恢复该技能, 需按新规则同时补 skills/<类别>/dzsf_style.png;
2. 派生检测的修复只恢复了"事件比较"这一层; 派生窗口时长、连段上限(i < 4 / i < 6)等数值逻辑未做任何改动, 与旧版一致;
3. 金箍棒判空仅覆盖 JinGuBangRenderer 两处直接链式调用; 同文件其余 lpp 使用处(getTextureLocation 与 actuallyRender 中的动画/蓄力判断)本就带有 lpp != null 前置判断, 未改动;
4. 立势图标动态路径中还有 "_1" 系列贴图(0_1/1_1/2_1), 现有代码只拼接 "_0"; "_1" 贴图为旧版遗留, 本次未清理, 不影响运行;
5. 动画按需加载意味着 dodge_b1.json 的损坏在首次后闪时才暴露; 已对全库 117 个动画 JSON 逐骨骼扫描 time/transform 数量, 无其它文件存在同类不匹配;
6. 本次任务仅新增本份文档, 未改动任何代码与资源; 6 个问题的修复效果均已由玩家游戏内实测确认(见问题记录); 建议回归测试场景: 普攻后切手/蓄力中闪避/完美闪避保留棍势/背包打开不崩溃/首次后闪/大圣势 HUD 图标;
7. 立棍起手约 1 秒窗口期内重复按特殊技会把起手替换为站桩蓄力(见第 7 节第 11 条), 该行为与旧版一致, 本次未改动, 未视为缺陷修复;
8. 问题七修复过程中加入的临时诊断日志已全部删除(提交 b2a6810 的 diff 仅含修复本体), 日志证据摘要记录于 4.7 节与第 8 节校验手段;
9. 问题七的回归测试场景: 大圣普攻 4 段接特殊技立棍蓄力/蓄力中升星/松键下棍(0~3 星各档)/蓄力中普攻与闪避打断/普通模式立棍与劈棍蓄力。
