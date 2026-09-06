# 大圣套装 Epic Fight 战斗模式"显示碎片"问题:原因分析与修复文档

## 1. 问题现象

穿上大圣套装(头盔 dasheng_h / 胸甲 dasheng_c / 护腿 dasheng_l / 靴子 dasheng_f)后:

- 原版(非战斗)模式:盔甲由 GeckoLib 直接渲染 Blockbench 导出的 geo 模型;
- Epic Fight 战斗模式:盔甲由 Epic Fight 接管渲染,显示成散落的"碎片",而不是完整盔甲。

## 2. 背景知识:Epic Fight 战斗模式下盔甲是怎么画出来的

战斗模式下玩家被替换成 Epic Fight 自己的动画骨架,盔甲不再由 GeckoLib 绘制,而是由 Epic Fight 的 WearableItemLayer 绘制。它获取"蒙皮网格"的优先级如下(反编译 EpicFight 20.14.17 证实):

1. 优先加载资源包里的 `assets/<命名空间>/animmodels/armor/<物品id>.json`,这是 Epic Fight 专用网格格式(顶点 + UV + 法线 + 每个顶点绑定的骨骼和权重);
2. 如果该文件不存在,就把盔甲"现场烘焙"成这种网格(HumanoidModelBaker.bakeArmor)。由于本盔甲是 GeckoLib 模型(GeoArmorRenderer),烘焙走 Epic Fight 为 GeckoLib 盔甲准备的 GeoModelTransformer。

GeoModelTransformer 的烘焙规则(反编译其静态初始化证实):按**原版人体各部位的比例范围**建包围盒(如手臂区域为 0.5x0.85x0.5 块、中心在 ±0.375, 1.125),把顶点裁剪进包围盒,并按高度阈值分配关节(如手臂在 y=1.125 处从 Hand 切换到 Arm)。

## 3. 为什么旧版本没问题,移植到 Epic Fight 20.14.17 就碎了

### 3.1 根因一:这套盔甲是"夸张造型",超出原版人体比例,新管线会把它碾碎

把模型每个部件的实际高度,与 Epic Fight 官方盔甲网格(反编译内置的 helmet/chestplate/leggins/boots.json,即官方认可的"正确位置")逐一对标:

| 部件 | 本模型位置(块) | Epic Fight 官方区间(块) | 结论 |
|---|---|---|---|
| 头盔 | 2.00 ~ 2.69 | 1.49 ~ 2.07 | **整体高出约 0.5 块(8 像素)** |
| 身体(含长袍) | 0.19 ~ 1.59 | 躯干 0.71 ~ 1.54 | 长袍下垂属造型设计,区间衔接正常 |
| 手臂 | 0.98 ~ 1.61 | 0.67 ~ 1.58 | 顶部对齐 |
| 腿 | -0.09 ~ 0.59 | 约 0 ~ 0.75 | 对齐 |
| 靴 | -0.03 ~ 0.71 | -0.06 ~ 0.81 | 对齐 |

原版 jar(wukong-forge1.20.1-20.2.0.jar)声明依赖 `epicfight [20.8.0,)`,且**不携带任何** animmodels/armor 网格资源(已核实 jar 内容)——即原版完全依赖当时 Epic Fight 版本对 GeckoLib 盔甲的处理方式。而 20.14.17 的烘焙会:头盔原样偏高 0.5 块(悬空);长袍、高冠等超出官方包围盒的几何被裁剪拉回,关节按高度错配。多部件各自错位/破碎,视觉上就是"碎片"。

(旧版本 20.8~20.13 的具体行为无法在本机逐字节复核,因为本地没有旧版 jar;但这不影响修复——修复不依赖对旧版的猜测,而是给 20.14.17 提供它优先级最高、格式完全可控的自定义网格资源,让烘焙彻底不参与。)

### 3.2 根因二(移植期间引入):geo 模型被重新导出过,UV 与贴图失配

当前项目里的 dasheng.geo.json 与原版 jar 里的不一致(移植时被改动):贴图基准从 128x128 改成 64x64,方块数量变化(头盔 24->20 块、身体 76->82 块等)。但两张 jar 里的贴图 `textures/item/armor/dasheng.png` 是同一个文件(16988 字节,实际尺寸 256x256)。

用离线渲染器对两个 geo 逐面采样统计:

| geo 版本 | 声明贴图基准 | UV 采样不透明率 |
|---|---|---|
| 原版 | 128x128 | 75.1%(自洽,128 基准对 256 贴图 = 2 倍超采样) |
| 当前 | 64x64 | **36.5%(一多半采样点落在透明区,贴图错乱)** |

因此即使烘焙成功,贴图也会错乱,加剧"碎片"观感。

### 3.3 根因三(第一版标定网格失败的原因):JSON 坐标系是 Blender 风格,Z 轴需取负

反编译 JsonAssetLoader.loadSkinnedMesh 证实:加载网格时会对 positions 和 normals 统一施加固定矩阵 `BLENDER_TO_MINECRAFT_COORD = Rx(-90度)`。即 JSON 里存储的三元组 (a, b, c) 在游戏中会变成 MC 坐标 (a, c, -b):

- 正确存储格式 = (mc_x, -mc_z, mc_y),第二个分量必须取负;
- 第一版标定网格存储了 (mc_x, mc_z, mc_y),导致整个网格前后镜像、法线全部反向,表现为战斗模式下模型"混乱";
- 该问题无法通过坐标区间校验发现(镜像不改变 min/max),是通过反编译加载器矩阵才定位的。

### 3.4 根因四(第二版网格仍混乱的原因):parts 数组必须预三角化

Epic Fight 渲染网格时把渲染模式改为 TRIANGLES(EpicFightRenderTypes.makeTriangulated),DrawingFunction(NEW_ENTITY)只是逐顶点写缓冲——即 parts 数组中**每 3 个连续顶点构成一个三角形**,网格文件必须预先三角化。

佐证:官方网格头盔的 head/hat 部件各 30 顶点 = 10 个三角形(原版头盔 12 个面去掉 2 个不可见的底面);官方 chestplate torso 240 顶点 = 80 个三角形,全部可被 3 整除。

此前的网格按"每 4 顶点一个四边形"输出,渲染器按 3 个一组切割,从第 4 个顶点起全部错位,网格被撕成错乱的三角形——这就是第二版"混乱"的直接形态。

### 3.5 之前的修复尝试为什么失败

| 尝试 | 失败原因 |
|---|---|
| 第一次:手工转换生成 animmodels JSON(旧脚本) | 坐标未标定(头盔原样偏高 0.5 块),且 UV 用了坏掉的 64 基准 |
| 第二次:删除 JSON 走 Epic Fight 原生烘焙 | 烘焙按原版比例裁剪+错配关节,对夸张造型必然碾碎(日志 `jsonVisible=false, positions=480` 证实走了烘焙且仍碎片) |

## 4. 修复方案

结论:**保留 GeckoLib**,双管线各用各的正确资源:

- 原版模式:GeckoLib 继续渲染 geo 模型(恢复原版 geo 后贴图/造型恢复发布版状态);
- Epic Fight 模式:提供"标定版"自定义网格 JSON(优先级高于烘焙),由重写的转换脚本从原版 geo 生成:

1. **恢复原版 geo 模型**(从原版 jar 提取覆盖),修复 UV 与贴图的自洽关系;
2. **重写 scripts/convert_dasheng_armor.py**,生成 4 个 `animmodels/armor/dasheng_{h,c,l,f}.json`:
   - 几何换算:逐字节移植 GeckoLib 4.8.3 BakedModelFactory/VertexSet/RenderUtils 的坐标与旋转规则(含 153 个带旋转方块的枢轴旋转);
   - 存储:`(x, -z, y)`(见 3.3),不做任何平移/缩放——EF 空间即原版模型空间(px/16, 脚底 0),glb 坐标天然对齐。依据:官方头盔顶 2.067 = 33px/16(原版头顶 32px + 1px 膨胀),官方头盔底 1.49 约等于 24px/16;本模型头盔冠底 glb 1.99 恰好落在 EF 头顶 2.0 上,腿部 0.18~0.71、靴 -0.09~0.18 也与 EF 对应区域吻合;
   - 骨骼绑定:照抄 Epic Fight 官方混合曲线(头盔纯 Head;躯干 Torso 到 Chest 在 0.85~1.40 线性过渡;手臂 y=1.125 处 Arm/Hand 切换;腿/靴按 Leg(0.365 以下) / Knee(0.365~0.735) / Thigh(以上) 分段);
   - UV:按原版 geo 的 128 基准归一化;
3. **重新打包**。资源存在后 Epic Fight 直接加载,烘焙路径不再触发。

> 历史备注:中间版本曾给 armorHead 加 -0.5 块平移,那是把"冠底"错锚到官方网格的"头盔底"所致——本模型是"戴在头顶的冠"设计,冠底应锚定 EF 头顶(2.0)而非头盔底(1.49),故最终版本平移量为 0。

## 5. 变动清单

| 功能点 | 是什么 | 为什么 | 作用 |
|---|---|---|---|
| geo 模型恢复 | 用原版 jar 的 dasheng.geo.json 覆盖当前文件 | 当前文件 UV 基准(64)与 256x256 贴图失配,骨骼结构也与发布版不一致 | 原版模式恢复发布版贴图与造型;为转换脚本提供自洽 UV 输入 |
| 转换脚本 v4 | scripts/convert_dasheng_armor.py(Blender 坐标存储 + 无平移 + parts 预三角化 + 官方混合曲线) | 旧脚本 Z 轴未取负、UV 基准错误;-0.5 平移属错误锚定;parts 未按 TRIANGLES 模式预三角化 | 生成坐标/蒙皮/UV/拓扑都正确的 Epic Fight 网格 |
| 4 个 EF 网格 JSON | animmodels/armor/dasheng_{h,c,l,f}.json(重新生成) | 该资源优先级最高,可完全绕开会碾碎造型的烘焙管线 | Epic Fight 战斗模式显示完整盔甲 |

## 6. 修改前后对比

- 修改前:Epic Fight 战斗模式碎片(烘焙裁剪 + 未标定坐标);原版模式贴图错乱(geo UV 基准错误);
- 修改后:Epic Fight 战斗模式加载标定网格(完整、贴图正确);原版模式恢复发布版外观;烘焙路径不再参与。

## 7. 风险与规避

1. 头盔 -0.5 块为标定值:若游戏内头盔整体仍有小偏移(注意:是整体偏移,不是碎片),调整脚本顶部 DELTA_Y 中 armorHead 的值(每 0.0625 约合 1 像素)重跑脚本即可;
2. 长袍下摆绑定 Torso(骨盆)关节,官方没有长袍先例:个别剧烈动作下长袍可能有轻微穿插,属外观层面;
3. 恢复原版 geo 改变原版模式当前外观:相对错误重导出属于修复(恢复发布版);若需改造型应回 Blockbench(注意贴图基准保持 128,否则需同步重导贴图);
4. 诊断日志 [DaShengDiag] 暂时保留(每次每件盔甲打一行),测试通过后可移除;
5. 回滚:geo 用 git 还原;删除 4 个 JSON 即回到烘焙路径;本次之前的状态可由 git 与 scripts/dasheng_animmodels_backup 完整恢复。

## 8. 具体出处

- 加载优先级/烘焙流程:EpicFight-20.14.17 反编译,`yesman/epicfight/client/renderer/patched/layer/WearableItemLayer.class`;
- 烘焙包围盒与高度阈值:同 jar `yesman/epicfight/api/client/model/transformer/GeoModelTransformer*.class`;
- GeckoLib 兼容注册:同 jar `yesman/epicfight/compat/GeckolibCompat.class`;
- 官方网格与混合曲线:同 jar `assets/epicfight/animmodels/armor/{helmet,chestplate,leggins,boots}.json`;
- 骨骼索引表:同 jar `assets/epicfight/animmodels/entity/biped.json`;
- 原版资产:`libs/wukong-forge1.20.1-20.2.0.jar`(geo 87048 字节/128 基准;贴图 16988 字节/256x256;无 animmodels;mods.toml 依赖 `epicfight [20.8.0,)`);
- UV 采样统计与部件区间标定:本地面渲染器/分析脚本逐面采样与逐顶点统计;
- GeckoLib 版本无关性:geckolib-4.8.3(项目依赖)与 geckolib-forge-1.20.1-4.8.4(实例运行)的 GeoArmorRenderer.class 字节码完全一致;
- 旋转方块处理:geckolib-4.8.3 `RenderUtils`(translateToPivotPoint/rotateMatrixAroundCube/translateAwayFromPivotPoint)与 `BakedModelFactory$Builtin` 字节码;
- JSON 坐标系(Blender 风格,Z 取负):同 jar `yesman/epicfight/api/asset/JsonAssetLoader.class` 静态初始化(BLENDER_TO_MINECRAFT_COORD = Rx(-90))与 loadSkinnedMesh 对 positions/normals 的矩阵变换;
- parts 预三角化:同 jar `Mesh$DrawingFunction`(NEW_ENTITY 逐顶点写缓冲)、`EpicFightRenderTypes.makeTriangulated`(渲染模式改为 TRIANGLES)、官方网格各部件顶点数均可被 3 整除;
- EF 空间 = 原版模型空间(px/16):官方网格头盔顶 2.067 约等于 33px/16、头盔底 1.49 约等于 24px/16;烘焙路径(GeoModelTransformer + prepMatrixForBone,零旋转零位移时矩阵为恒等)输出原始 glb 坐标亦可佐证。

## 9. 遗留情况

- 原版模式下头盔垂直位置本次不处理(geo 造型未改动,仅恢复文件);若确认原版模式头盔也悬空,需在 Blockbench 中将 armorHead 下移 8 像素后重新导出(保持 128 基准);
- ResourceLocation 构造器弃用警告(约 55 处)为独立问题,未包含在本次修改;
- 预览图:scripts/old_geo_render.png(原版模型)与 scripts/new_geo_render.png(当前模型),可目视对比。
