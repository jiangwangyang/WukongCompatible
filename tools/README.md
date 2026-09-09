# tools - 代码工具

## google-java-format-1.28.0-all-deps.jar

项目 Java 源码统一使用 google-java-format 1.28.0、AOSP 风格（4 空格缩进）格式化，与提交 134e545 "Format all Java sources with google-java-format (AOSP style, 4-space indent)" 确立的约定一致。

格式化单个或多个文件：

```bash
java -jar tools/google-java-format-1.28.0-all-deps.jar --aosp --replace <文件.java> ...
```

提交前检查文件是否已符合格式（退出码 0 = 已格式化）：

```bash
java -jar tools/google-java-format-1.28.0-all-deps.jar --aosp --dry-run --set-exit-if-changed <文件.java> ...
```

## convert_dasheng_armor.py

大圣盔甲模型转换脚本：将 GeckoLib 盔甲模型 (`dasheng.geo.json`, 原版 128 基准) 转换为 EpicFight 20.14.17 盔甲网格 JSON，输出到 `src/main/resources/assets/wukong/animmodels/armor/dasheng_{h,c,l,f}.json`。

```bash
python3 tools/convert_dasheng_armor.py
```

原理与历史问题记录见 `docs/大圣套装修复.md`。
