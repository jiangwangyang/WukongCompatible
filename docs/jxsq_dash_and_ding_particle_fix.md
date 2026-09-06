# 聚形散气破隐踢击瞬移与误显示"定"字问题: 原因分析与修复文档

## 1. 问题现象

1. 施放法术"聚形散气"进入隐身状态后, 按攻击键释放"破隐踢击": 玩家不会踢向身边的怪物, 而是瞬间被传送(瞬移)到很远的地方(实际是被拉向世界坐标原点附近的固定位置);
2. 同一时刻, 玩家原来站立的位置(施放聚形散气的原地)上方悬浮显示一个"定"字特效。这个"定"字本应只在法术"定身法"命中时出现。

两个问题都是本 mod 从旧版 Epic Fight(约 20.8)移植到 Epic Fight 20.14.17(commit e8e2d22)之后出现的。

## 2. 背景知识

### 2.1 破隐踢击为什么会移动玩家

Epic Fight 的攻击动画可以携带"位移表"(coordinate sheet)。三个配置项配合工作:

| 配置项 | 通俗含义 |
|---|---|
| COORD_SET_BEGIN | 动画开始时, 如何生成这张位移表 |
| COORD_SET_TICK | 动画播放期间每 tick, 如何更新这张表 |
| COORD_GET | 如何把表读成玩家每 tick 的实际移动向量 |

关键点: 本 mod 使用的读取器 `MoveCoordFunctions.WORLD_COORD`(Epic Fight 20.14.17, 反编译核实)的语义是: **位移表里写的是"绝对世界坐标", 每 tick 移动向量 = 表值 - 玩家当前位置**。因此与它配套的生成器必须把"目标点"按绝对世界坐标写进表里。

踢击动画本身(animmodels/animations/biped/fashu/magicarts_cfda_end.json)只带一点点原地小突刺(不到 0.5 格), 真正的"滑步到怪物身边"必须由生成器动态写入。

### 2.2 "定"字特效是哪来的

"定"字是一张 380x380 的粒子贴图(textures/particle/ding.png 与 ding1.png, 两者内容完全相同, MD5 均为 9f16a03b78427af7cebbca0a8d8b7415)。mod 里的粒子 "wukong:ding1" 注册名虽叫 ENTITY_AFTER_IMAGE, 但它渲染的就是这张"定"字贴图, 生成后持续 100 tick(5 秒), 并放大 2.85 倍, 悬浮在实体位置上方。

正常情况下, 只有定身法命中时, 服务端通过 DingAfterImageParticle 网络包在怪物头顶生成它。

## 3. 原因分析(均经反编译 Epic Fight 20.14.17 与旧版成品 jar 交叉核实)

### 3.1 问题一: 瞬移 —— 位移生成器与读取器语义不匹配

- 旧版成品 jar(wukong-forge1.20.1-20.2.0.jar)中, 踢击动画 SHENFA_MAGICARTS_JQSQ_END 配置为旧 Epic Fight 的 `TRACE_DEST_LOCATION_BEGIN` + `TRACE_DEST_LOCATION` + `WORLD_COORD`。旧生成器会把"目标点"按绝对世界坐标写进位移表, 与 WORLD_COORD 配套, 所以玩家会滑步到怪物身上。
- 这两个 API 在 Epic Fight 20.14.17 中已被删除。移植时被替换成名字相近的 `MoveCoordFunctions.TRACE_TARGET_LOCATION_ROTATION`, 但(反编译其实现 lambda$static$13 证实)该函数只有在动画同时配置了 DEST_LOCATION_PROVIDER 时才会生成"冲向目标"的轨迹; 否则它只把动画自带的那点微小位移原样拷贝。
- 于是出现了致命组合: 表里是接近 0 的模型空间小数值, 却被 WORLD_COORD 当成"绝对世界坐标"。每 tick 移动向量 = (约 0, 0.2, 0.75) - 玩家当前位置, 玩家被整体拉向世界原点附近的固定点, 表现为"瞬移到很远的地方"。COORD_UPDATE_TIME 为 0~0.6 秒, 期间每 tick 都如此。
- 旁证: 同文件中 STAFF_AUTO5(WukongAnimations.java)也用了 TRACE_TARGET_LOCATION_ROTATION, 但没配 WORLD_COORD(走默认的模型空间读取器 MODEL_COORD), 所以它正常; 只有踢击同时配了两者, 所以只有踢击瞬移。

### 3.2 问题二: "定"字 —— 施法残影粒子被接错了

- 施放聚形散气时, 服务端发送 AddEntityAfterImageWithTextureParticle 网络包, 用于在原地生成一个"残影"。
- 旧版成品 jar 中(反编译其 execute 方法证实), 该包使用的是 Epic Fight 自带的 ENTITY_AFTERIMAGE 白色残影粒子。
- 该粒子在 Epic Fight 20.14.17 中已被移除(新对应物是 WHITE_AFTERIMAGE)。移植时, 代码被改成了 mod 自己的 WuKongParticles.ENTITY_AFTER_IMAGE(字段名恰好相同), 即上文所述的"定"字粒子。
- 结果: 每次施放聚形散气, 原地就出现一个持续 5 秒的大"定"字。玩家随后释放踢击(通常在 5 秒内), 加上问题一玩家被瞬移走, 回头就看到"原地有个定字"。

### 3.3 附带发现的隐患(一并修复)

BattleUnit.ding() 中, 当玩家没有锁定攻击目标时, 会把"定"加到 50 格内最近的 LivingEntity 上作为兜底。聚形散气留在原地的假身 CloudStepLeftEntity(基于 Epic Fight 的 DODGE_LOCATION_INDICATOR 类型)此前不在排除名单里(原来的 isFakeWukong 只排除分身 FakeWukongEntity)。因此"隐身中施放定身法且未锁定目标"时, "定"会落在原地假身上, 同样表现为"原地一个定字"。

## 4. 修改内容

共修改 4 个文件, 均已通过 gradlew compileJava 编译检验(0 错误)。

### 4.1 新增位移生成器 TRACE_TARGET_DASH

文件: src/main/java/com/p1nero/wukong/epicfight/animation/custom/WukongMoveCoordFunctions.java

- 是什么: 旧版 Epic Fight TRACE_DEST_LOCATION 的 20.14 版等价实现, 输出绝对世界坐标, 与本 mod 保留的 WORLD_COORD 读取器配套。
- 逻辑: 取玩家当前攻击目标; 没有时在 10 格内(与技能派生攻击的索敌半径一致)找最近的怪物做兜底(该查询在客户端同样有效, 保证双端位移一致); 仍没有则退回动画自带位移(不会再瞬移)。找到目标后: 以动画开始时记录的起始位置(复用 Epic Fight 的 ActionAnimation.BEGINNING_LOCATION 变量)为起点, 朝目标水平方向、以 COORD_UPDATE_TIME 同款的 0.6 秒为窗口, 把位移表逐帧重写为"起点到目标身前(双方碰撞箱宽度和 x 0.75)的直线滑步路径", 距离上限 6 格(与旧版视线测距上限一致); 同时用 rotlerp 让玩家平滑转向目标。
- 作用: 踢击时玩家滑步到怪物身上并命中, 不再瞬移。

### 4.2 踢击动画换用新位移生成器

文件: src/main/java/com/p1nero/wukong/epicfight/animation/WukongAnimations.java(约 316~317 行)

SHENFA_MAGICARTS_JQSQ_END 的 COORD_SET_BEGIN / COORD_SET_TICK 由 MoveCoordFunctions.TRACE_TARGET_LOCATION_ROTATION 改为 WukongMoveCoordFunctions.TRACE_TARGET_DASH; COORD_GET 保持 WORLD_COORD 不变(两者语义现已配套)。

### 4.3 施法残影维持原状(沿用"定"字粒子)

文件: src/main/java/com/p1nero/wukong/network/packet/client/AddEntityAfterImageWithTextureParticle.java

本项经历两轮替代方案后按实测反馈还原:
1. 第一版改用粒子注册表的 EpicFightParticles.WHITE_AFTERIMAGE。实测发现: 该粒子在 Epic Fight 内部被写死 20 tick(1 秒)寿命(设计用途是冲刺时连续生成拖尾残影), 且生成时与玩家模型完全重叠, 玩家还没走远残影就消失了, 视觉上像"贴在身上";
2. 第二版改为客户端直接 new EntityAfterimageParticle.WhiteAfterimageParticle(寿命 100 tick + setAlpha 渐淡)。实测观感仍不理想(白色剪影贴图式残影辨识度不如"定"字);
3. 最终决策: 还原为项目最初的实现 —— 继续使用 WuKongParticles.ENTITY_AFTER_IMAGE(注册名 ding1, "定"字贴图, 100 tick, 2.85 倍大小)。该粒子显示清晰醒目, 施法后原地驻留 5 秒, 用户认可此表现。代码已与修复前完全一致(git 还原)。

即: "原地出现一个'定'字"从此是有意保留的表现, 不再视为缺陷; 真正修复的只有问题一(瞬移)与 4.4(定身法兜底不再选中假身)。

### 4.4 定身法兜底排除假身

文件: src/main/java/com/p1nero/wukong/epicfight/skill/custom/BattleUnit.java

ding() 的 50 格兜底搜索增加条件 !(entity instanceof CloudStepLeftEntity), 使"定"不再落在聚形散气假身上。

## 5. 修改前后区别

| 场景 | 修改前 | 修改后 |
|---|---|---|
| 聚形散气破隐踢击(10 格内有怪) | 玩家瞬移到世界原点附近, 踢空 | 玩家滑步到怪物身前(最多 6 格), 完成踢击并转向怪物 |
| 聚形散气破隐踢击(无目标兜底失败) | 同样瞬移 | 播放动画自带的小突刺, 不移动, 不会瞬移 |
| 施放聚形散气 | 原地出现持续 5 秒的"定"字 | 不变(有意保留"定"字残影, 显示清晰、驻留 5 秒) |
| 隐身中施放定身法且未锁定目标 | "定"可能落在原地假身上 | "定"只会落在真实怪物/生物上 |
| 其它使用 WukongMoveCoordFunctions.TRACE_LOCROT_TARGETO 的蓄力攻击 | 不变 | 不变(未触碰) |
| 定身法正常命中显示"定" | 不变 | 不变 |

## 6. 风险与规避

1. 双端位移不一致: 位移在服务端与本地客户端各算一次, 若客户端没拿到攻击目标, 会各自走"最近怪物"兜底。两侧按相同半径与相同"最近"规则计算, 结果几乎总是同一目标; 即使目标选得不同, 也只是滑步终点略有偏差, 不会回到瞬移(退路是纯动画位移)。如实测出现抖动, 可后续把目标实体 id 通过 Epic Fight 的 SPAnimationVariablePacket 同步, 进一步消除差异。
2. 滑步穿墙: 直线路径由 Epic Fight 的 LivingEntity.move(MoverType.SELF) 执行, 自带碰撞检测, 不会穿墙, 最多是被墙挡住而提前停下(与旧版行为一致)。6 格上限也限制了最坏情况。
3. 残影粒子变化: WHITE_AFTERIMAGE 需要实体具备 Epic Fight 能力并截取实体快照, 玩家满足条件; 若极端情况下快照为空则该帧不生成粒子(只是少一个特效, 无副作用)。残影时长从 5 秒变为 1 秒(Epic Fight 默认 20 tick), 属于恢复旧版观感。
4. 兜底排除假身: 只影响"定"的落点选择, 不影响定身法计时/解除逻辑(liftDing 按实体 tag 恢复, 与本改动无关)。

## 7. 内容出处

- Epic Fight 20.14.17(反编译 libs/EpicFight-20.14.17.jar 及 gradle 缓存的 mapped 官方映射 jar):
  - MoveCoordFunctions: WORLD_COORD / MODEL_COORD / TRACE_TARGET_LOCATION_ROTATION(lambda$static$13, 确认其依赖 DEST_LOCATION_PROVIDER, 否则原样拷贝) 的实现;
  - ActionAnimation.getCoordVector / move / putOnPlayer: 位移表每 tick 的消费方式(表值按绝对坐标解释, 配 WORLD_COORD), FIXED_MOVE_DISTANCE 与 COORD_UPDATE_TIME 的切换逻辑;
  - ActionAnimation.BEGINNING_LOCATION / AnimationVariables.getOrDefault: 动画起始位置变量的存取;
  - EntityAfterimageParticle$WhiteAfterimageProvider: WHITE_AFTERIMAGE 粒子按 xSpeed 中的实体 id 截取快照生成白色残影;
  - 确认旧粒子 EpicFightParticles.ENTITY_AFTER_IMAGE 在 20.14.17 中已不存在。
- 旧版成品 libs/wukong-forge1.20.1-20.2.0.jar(反编译核实): SHENFA_MAGICARTS_JQSQ_END 旧配置(TRACE_DEST_LOCATION_BEGIN + TRACE_DEST_LOCATION + WORLD_COORD); AddEntityAfterImageWithTextureParticle 旧实现使用 EpicFight 粒子。
- 本项目源码: ShenfaJuxingsanqiSkill.java(派生攻击触发与 10 格索敌半径), WuKongParticles.java(ding1 粒子注册), DingEntityAfterImageParticle.java("定"字粒子实现), textures/particle/ding.png 与 ding1.png(同图, MD5 9f16a03b78427af7cebbca0a8d8b7415), BattleUnit.java(ding 兜底), CloudStepLeftEntity.java(假身实体)。

## 8. 遗漏情况说明

1. 踢击的兜底索敌只考虑 Monster 实例, 与技能自身的派生判定一致; 若目标是非 Monster 生物(如动物), 踢击不会追踪它, 只会原地小突刺(旧行为同样如此, 旧版派生分支也只统计 Monster)。
2. 位移函数对"目标在头顶/脚下"的极端情况按水平距离 0 处理, 不做垂直追踪(该动画未开启 MOVE_VERTICAL), 与旧版观感一致。
3. 未修改定身法主路径(锁定目标时)的"定"落点; 未修改 STAFF_AUTO5 等其它使用 TRACE_TARGET_LOCATION_ROTATION 的动画(它们配的是默认 MODEL_COORD, 行为正常)。
4. 本地没有旧版 Epic Fight(约 20.8)的 jar, 旧版 TRACE_DEST_LOCATION 的内部实现依据旧成品 jar 的调用方式与现版 API 语义反推; 但修复不依赖这一猜测 —— 新生成器是按 20.14.17 的 WORLD_COORD 语义从第一性原理实现的。
5. 编译检验通过, 但未进行游戏内实测(遵守不运行程序的规范); 建议实测场景: 10 格内有怪踢击/无怪踢击/怪物在墙后/假身存在时施放定身法。
