# 弃用 API 清理: 分析与替换文档

## 1. 这份文档讲什么

本项目(黑神话: 悟空连招 Epic Fight 附属, MC 1.20.1 + Forge 47.4.10 + Epic Fight 20.14.17)的代码中存在大量调用了"已被官方标记为弃用(Deprecated)的方法"的情况。弃用意味着: 官方(原版 Minecraft/Forge/Epic Fight)已经不推荐使用这些方法, 并在未来的版本中会删除它们, 继续积累会导致将来升级时大面积崩坏。

本次改动使用 javac 编译器自带的弃用警告分析(`-Xlint:deprecation`)对全部 93 个源文件做了扫描, 并把"当前依赖版本中存在官方替代 API"的弃用调用全部替换为等价的新 API; 对于"当前依赖版本中不存在替代 API"的少数弃用调用, 保留原样并添加说明注释。

改动结论: 编译弃用警告从 308 条降至 69 条, 剩余 69 条全部是依赖版本内无替代 API 的 Epic Fight 武器构建器方法(见第 5 节), 全部替换均为行为等价替换, 不改变任何游戏内表现。

## 2. 分析工具与方法

- 工具: JDK 17 javac 的 `-Xlint:deprecation` 警告(在 build.gradle 的 JavaCompile 任务中开启), 并调高 `-Xmaxwarns` 上限。
- 重要发现: javac 默认只报告前 100 条警告, 本项目实际警告 308 条, 首轮扫描曾被默认上限截断导致漏报(例如 FashuAnshenfaSkill 后半部分), 提高 `-Xmaxwarns` 后才获得完整清单。
- 每一种弃用 API 的替代方案, 都通过反编译依赖 jar(javap 查看字节码与注解)逐一核实后才动手替换, 替代依据在下一节逐条列出。

## 3. 变动功能点清单(是什么/为什么/有什么作用)

### 3.1 技能数据同步 setDataSync(改动最多, 共 171 处, 25 个文件)

- 是什么: `SkillDataManager.setDataSync(key, value, player)` 三参数重载已被 Epic Fight 标记弃用(forRemoval)。
- 替换为: 两参数的 `setDataSync(key, value)`。
- 为什么等价: 反编译比对证实, 弃用的三参版本内部就是 `setData(key, value)` + `syncServerPlayerData(key, player)`; 两参版本是 `setData(key, value)` 后按容器执行者自动分端: 服务端用容器自己的 ServerPlayer 同步, 客户端用容器自己的 LocalPlayer 同步。本项目中所有调用点传入的 player 本来就是该数据容器所属的同一个玩家, 因此两者完全等价, 两参版本还能自动区分客户端/服务端, 更不易误用。
- 涉及文件: WukongAnimations, WukongGreatSageAnimations, BattleUnit, SmashHeavyAttack, ThrustHeavyAttack, PillarHeavyAttack, GreatSageHeavyAttack, StaffPassive, WukongDodgeSkill, ShenfaJuxingsanqiSkill, ShenfaTongtoutiebiSkill, FashuAnshenfaSkill, FashuDingshenfaSkill, ShenWaiShenFaSkill, WukongKeyMappings 等。

### 3.2 资源路径 ResourceLocation 构造器(共 51 处, 21 个文件)

- 是什么: `new ResourceLocation(namespace, path)`(48 处)与 `new ResourceLocation(String)`(3 处)两个构造器已被原版标记弃用且 forRemoval。
- 替换为: 官方静态工厂方法 `ResourceLocation.fromNamespaceAndPath(ns, path)` 与 `ResourceLocation.parse(str)`。
- 为什么等价: 工厂方法在 1.20.1 官方 jar 中已存在(反编译核实), 内部校验逻辑与构造器一致; 三处单参调用(Config.java 实体名校验, FakeWukongRenderer 的 steve 贴图, StaffPassive 的实体类型解析)解析的字符串语义不变。
- 涉及文件: JinGuBangRenderer(7), ThrustHeavyAttack(7), SmashHeavyAttack(7), PillarHeavyAttack(7), GreatSageHeavyAttack(7), WukongWeaponCapabilityPresets, WukongSkillDataKeys, WuKongSounds, EpicFightDamageType, DaShengArmorTextureHandler, DaShengArmorRenderer, FakeWukongRenderer, Config, PacketHandler, WKCapabilityProvider, StaffPassive, 各 fashu 技能等。

### 3.3 主模组类构造方式(WukongMoveset.java)

- 是什么: 构造器里使用的 `FMLJavaModLoadingContext.get()` 与 `ModLoadingContext.get()` 已被 Forge 标记弃用, Forge 的 javadoc 明确写明替代方案是"在模组构造器中接收 context 参数"。
- 替换为: 构造器签名改为 `public WukongMoveset(FMLJavaModLoadingContext context)`。反编译 Forge 47.4.10 的 FMLModContainer 证实, 模组加载时会优先尝试 `getDeclaredConstructor(FMLJavaModLoadingContext.class)` 进行注入, 注入的实例与 `get()` 返回的是同一个对象, 因此 `context.getModEventBus()` 与 `context.registerConfig(...)` 行为完全一致。
- 作用: 消除加载路径上的弃用调用, 这是 Forge 1.21.1+ 的强制写法, 提前迁移。

### 3.4 客户端玩家补丁获取(CameraAnim.java, 2 处)

- 是什么: `ClientEngine.getInstance().getPlayerPatch()` 已被 Epic Fight 标记弃用。
- 替换为: `EpicFightCapabilities.getEntityPatch(Minecraft.getInstance().player, LocalPlayerPatch.class)`。反编译证实弃用方法的内部实现正是这一行(且 getEntityPatch 对 null 玩家返回 null, 与原方法一致, 原有的判空逻辑保留)。
- 作用: 镜头拉近功能(蓄力时第三人称视角偏移)改用官方推荐的通用补丁获取入口。

### 3.5 粒子注册(WuKongParticles.java, 2 处)

- 是什么: `ParticleEngine.register(type, provider)` 已被 Forge 标记弃用。
- 替换为: 同一个 `RegisterParticleProvidersEvent` 事件自带的 `event.registerSpriteSet(type, provider)`(该类本来就在监听此事件)。两者接收同一个函数式接口 `SpriteParticleRegistration`, 方法引用 `DingParticle.DangerParticleProvider::new` 原样保留。
- 作用: 粒子提供者注册改为 Forge 推荐的事件 API。

### 3.6 玩家移动输入拦截(SmashHeavyAttack, PillarHeavyAttack, StaffPassive, 共 4 处)

- 是什么: Epic Fight 事件 `MovementInputEvent.getMovementInput()`(返回可变的原版 Input 对象)已弃用, 新 API 为不可变的 `PlayerInputState` 记录类。
- 替换为: `InputManager.setInputState(event.getInputState().withXxx(...))` 链式写法。等价性依据: 反编译 `InputManager.setInputState` 证实其内部调用 `PlayerInputState.applyToVanillaInput(state, Minecraft.getInstance().player.input)`, 会把全部 8 个字段(leftImpulse/forwardImpulse/up/down/left/right/jumping/sneaking)写回原版 Input, 其中 sneaking 对应原 Input.shiftKeyDown; Epic Fight 自己的新版技能(SteelWhirlwindSkill, GuardSkill)正是这种写法。
- 作用: 蓄力期间禁止跳跃, 棍花期间禁止移动/潜行等输入压制逻辑迁移到新输入 API。

### 3.7 按键状态设置(StaffPassive, PillarHeavyAttack, 共 2 处)

- 是什么: `ControlEngine.setKeyBind(keyMapping, value)` 已被 Epic Fight 标记弃用且类内无其他重载。
- 替换为: 直接调用 `keyMapping.setDown(value)`。反编译证实弃用方法的方法体就只有这一行委托调用, 行为逐字节一致。作用: 棍花期间强制关闭疾跑。

### 3.8 盔甲材料修复原料懒加载(WukongArmorMaterials.java)

- 是什么: 原版工具类 `net.minecraft.util.LazyLoadedValue` 整个类已被标记弃用。
- 替换为: Forge 的 `net.minecraftforge.common.util.Lazy`(通过 `Lazy.of(supplier)` 创建)。两者都是"首次 get() 时才执行 supplier"的懒加载包装, `get()` 调用点代码不变; 修复原料 `Ingredient.of(Items.NETHERITE_INGOT)` 的注册时机不变。

### 3.9 粒子贴图集常量(ParticleRenderTypeN.java, 1 处)

- 是什么: `TextureAtlas.LOCATION_PARTICLES` 已被原版标记弃用, 1.20.1 中不存在替代常量(替代常量 AtlasIds 是 1.20.5 才加入的)。
- 替换为: 按反编译核实的常量值 `minecraft:textures/atlas/particles.png` 等价内联为 `ResourceLocation.withDefaultNamespace("textures/atlas/particles.png")`。
- 作用: 自定义粒子渲染类型(立棍式技能特效)的贴图集引用改用非弃用 API, 值完全相同。

### 3.10 分析能力固化(build.gradle)

- 在 JavaCompile 任务中新增 `options.compilerArgs << '-Xlint:deprecation'` 与 `-Xmaxwarns 100000`。
- 作用: 以后每次编译都会把所有弃用 API 的使用位置完整打印出来, 不再受 javac 默认 100 条上限的截断, 可持续监控技术债。

## 4. 修改前后对比

- 修改前: 编译产生 308 条弃用警告(其中因 javac 默认上限, 常规编译只显示 100 条), 涉及 33 个源文件, 18 种弃用 API; 模组功能不受影响但弃用面持续扩大。
- 修改后: 编译产生 69 条弃用警告, 全部集中在 WukongWeaponCapabilityPresets.java 一个文件的 6 种 Epic Fight 武器构建器方法上(见第 5 节); 其余 17 种弃用 API 清零。
- 行为变化: 无。所有替换都经过反编译比对, 是调用形态的等价改写, 游戏内表现(技能, 输入, 渲染, 注册)不变。

## 5. 保留未替换的 69 条警告(依赖版本内无替代 API)

WukongWeaponCapabilityPresets.java 中 `WeaponCapability$Builder` 的 6 个方法: livingMotionModifier(49 处), innateSkill(7 处), newStyleCombo(7 处), styleProvider(2 处), comboCancel(2 处), passiveSkill(2 处)。

保留原因(均已反编译 Epic Fight 20.14.17 官方 jar 核实):

1. 这 6 个方法在当前依赖版本中各自只有一个重载, 不存在任何非弃用的替代方法;
2. 弃用注解标注 `since = "1.21.1", forRemoval = true`, 即替代 API 只存在于 Epic Fight 的 1.21.1+ 代码库, 1.20.1 版本根本没有;
3. Epic Fight 官方自带的 WeaponCapabilityPresets 与 JSON 加载器 WeaponTypeReloadListener 同样在调用这些方法(仅官方预设中 livingMotionModifier 就调用了 89 次), 它们就是 1.20.1 下的标准用法;
4. 唯一的替换途径是升级 Epic Fight 到 1.21.1 版本, 意味着放弃 MC 1.20.1 支持, 属于独立的依赖升级工程, 不在本项目当前目标内。

处理方式: 在 WukongWeaponCapabilityPresets.java 类头添加了说明注释, 保留编译警告作为将来升级时的迁移检查清单, 未添加 @SuppressWarnings(按约定)。

## 6. 风险与规避

1. 替换引入行为差异的风险: 低。每类替换都通过反编译核实了内部实现等价( setDataSync 的同步目标, getEntityPatch 的空值保护, setKeyBind 的委托实现, applyToVanillaInput 的字段映射, Lazy 的懒加载语义, fromNamespaceAndPath/parse 的校验逻辑)。规避方式: 建议实际运行游戏验证核心路径: 蓄力/棍花/闪避/法术的技能数据同步(服务端与客户端数值应保持一致), 蓄力镜头, 棍花禁移动与禁疾跑, 粒子与贴图显示, 盔甲修复配方。
2. WukongMoveset 构造器签名变更的风险: 低。模组类仅由 FML 反射构造, FMLModContainer 会自动尝试带 FMLJavaModLoadingContext 参数的构造器(反编译核实); 项目内无其他 new WukongMoveset() 调用点。规避方式: 启动游戏确认模组正常加载, 配置文件正常生成。
3. 批量脚本误伤的风险: 替换由脚本辅助完成, 曾出现注释结构与行尾重复两类脚本缺陷, 均已通过 git 还原并修复脚本后重做; 最终以 `git show HEAD` 比对确认所有被改文件的注释标记数量不变, 并以 javac 全量编译通过(0 错误)兜底。
4. 遗漏风险: 弃用警告清单已提高 -Xmaxwarns 后完整收集(308 条全覆盖), 不存在因上限截断的漏报; 剩余 69 条为有意保留。后续新代码若引入弃用 API, 编译时会被 -Xlint:deprecation 直接暴露。

## 7. 依据来源

- 弃用清单: javac -Xlint:deprecation 对本项目 93 个源文件的编译输出(修改前 308 条, 修改后 69 条)。
- Epic Fight 侧结论: 反编译 build/fg_cache 与 forge_gradle 缓存中的 EpicFight-20.14.17_mapped_official_1.20.1.jar, 涉及 SkillDataManager, WeaponCapability$Builder, ClientEngine, MovementInputEvent, PlayerInputState, InputManager, ControlEngine, EpicFightCapabilities, WeaponCapabilityPresets, WeaponTypeReloadListener 等类的字节码与 @Deprecated 注解。
- Forge/Minecraft 侧结论: 反编译 forge-1.20.1-47.4.10_mapped_official_1.20.1-recomp.jar(ResourceLocation, TextureAtlas, LazyLoadedValue, RegisterParticleProvidersEvent, Lazy)与 javafmllanguage/fmlcore 1.20.1-47.4.10 的官方源码 jar(FMLJavaModLoadingContext, FMLModContainer, ModLoadingContext, 其中含弃用 javadoc 指明的替代方式)。
- 注: 本项目自带 build/fg_cache 编译缓存, 上述 jar 均为项目编译实际使用的 classpath 产物, 无需额外下载。

## 8. 遗漏情况说明

- 代码中被注释掉的死代码块(如各技能 onRemoved 中 /* */ 包裹的旧 setDataSync 调用)未做替换: 它们不参与编译, 保留原样以最小化改动; 若将来启用这些代码块, 需按第 3.1 节同步改为两参 setDataSync。
- GeckoLib 依赖(4.8.3)未扫描出本项目代码对它的弃用调用, 无需处理。
- 非代码文件(资源 json, mixins 配置)不涉及弃用 API。
- staff_stack 相关 GUI 贴图路径等字符串拼接调用, 替换后字符串内容未改动, 已由编译与常量值核对覆盖。
