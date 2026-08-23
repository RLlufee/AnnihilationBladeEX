# AnnihilationBladeEX 2.8.0-1.21.1-neoforge

湮灭之刃的 1.21.1 NeoForge 移植版，公开命名空间为 `annihilationbladeex`，目标版本固定为 `2.8.0-1.21.1-neoforge`。

## 环境

- Minecraft `1.21.1`
- NeoForge `21.1.228`
- Java `21`
- SlashBlade Resharped `2.0.3-1.21.1`
- Jupiter `2.3.7-1.21.1-neoforge`
- 本地依赖：`libs/SlashBladeResharped-2.0.3-1.21.1.jar`

## 内容

- 主刀：`annihilationbladeex:annihilation_blade`
- 血狱：`annihilationbladeex:blood_prison`
- 湮灭核心：右键生成同一把 canonical 湮灭之刃
- Slash Art：`annihilationbladeex:spatial_fracture`
- Special Effects：
  - `annihilationbladeex:dankong`
  - `annihilationbladeex:world_rift`
  - `annihilationbladeex:terminus_echo`
  - `annihilationbladeex:void_dominion`
  - `annihilationbladeex:causality_collapse`
  - `annihilationbladeex:starless_judgement`
  - `annihilationbladeex:phantom_judgement`
  - `annihilationbladeex:abyssal_decree`

`Phantom Judgement` 使用 40 格索敌范围，触发后生成受限幻影剑雨；剑雨击杀目标后会在地面短暂保留，用于提示击杀来源，同时降低服务器实体与粒子峰值。

湮灭之刃位于背包、主手或副手时，客户端会以光照贴图提供无药水图标的夜视级照明；该效果不写入玩家的药水状态。

所有普攻、SA 与 SE 的目标判定统一遵循 SlashBlade 的 `pvp_enable` 与 `friendly_enable` 配置，默认不会伤及玩家、宠物和非敌对单位。

裂界会在湮灭之刃伤害生效 5 tick 后，以受击位置为中心处决周围所有合法目标，并继续连锁。连锁次数和以初始攻击者为中心的最大连锁范围可在 common config 的 `annihilation_blade.world_rift.chain_count` 与 `chain_range` 调整。

## 构建

PowerShell 下使用 UTF-8 与 Java 21：

```powershell
$OutputEncoding=[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)
$env:JAVA_HOME='C:\Program Files\Zulu\zulu-21'
$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8'
.\gradlew.bat --no-daemon build --console=plain
```

产物位于 `build/libs/`，文件名应包含版本 `2.8.0-1.21.1-neoforge`。

## Changelog
### 2026-08-23 魔龙夜陨第三态 SA 与 Jupiter 配置修复
- 修正 `nightfall_dragon.json` 默认命名刀定义：新刀回到封印态 `nightfall_judgement_cut` 与封印态 SE，避免未写入形态标签时直接携带终焉态 SA/SE 混合配置。
- 补回第三态压制链路：NeoForge 版在 `LivingIncomingDamageEvent` 中恢复“被压制实体不能造成伤害”的分支，保持 1.20.1 终焉态世界撕裂/结界压制行为。
- 将配置系统从 NeoForge `ModConfigSpec` 迁回 Jupiter `AutoInitConfigContainer`，接入 common/server 与 client JSON 配置、Jupiter 配置界面以及 Tooltip 渲染开关。
- 新增 Jupiter Modrinth 依赖与 `neoforge.mods.toml` 必需前置声明，配置读取统一改为 `getValue()`，并补齐四个语言文件的配置标题与字段名。

### 2026-08-23 魔龙刀旋转光环同步
- 将 1.20.1 Forge 版魔龙刀物品视角的 `item_dragon_halo_luminous` 旋转光环渲染逻辑同步到 1.21.1 NeoForge 事件链，保持 `nightfall_dragon_halo.png` 发光光环按源版持续旋转与脉冲缩放。
- 收敛 NeoForge 目标工程此前对 `halo` 渲染节点的额外取消逻辑，改回源版“所有 `item_` 节点叠加旋转光环”的行为，避免打乱 SlashBlade 的默认模型节点处理顺序。
- 修正光环可见但不旋转的问题：对 `item_dragon_halo_luminous` 分组单独替换默认静态渲染 pass，并按 OBJ 中心点执行旋转与脉冲缩放，避免固定光环覆盖动态层。

### 2026-08-23 魔龙刀模型修复
- 补齐 `assets/annihilationbladeex/models/item/nightfall_dragon.json`，让 `annihilationbladeex:nightfall_dragon` 走 SlashBlade 的 `builtin/entity` 物品渲染路径。
- 为 `annihilationbladeex:nightfall_dragon` 注册 SlashBlade 客户端物品属性、`SlashBladeTEISR` 自定义渲染器与 `BladeModel` 烘焙包装，并将客户端注册类显式挂到 NeoForge MOD 事件总线，修复进游戏后魔龙刀物品图标显示紫黑缺失纹理、手持时被渲染成巨大紫黑面片的问题。

### 2026-08-23 迁移收尾
- 清理目标工程残留的 1.20.1 Forge `META-INF/mods.toml`，打包时仅保留 NeoForge 模板生成的 `META-INF/neoforge.mods.toml`。
- 将资源包描述统一为 `annihilationbladeex resources`，避免继续暴露旧命名空间描述。
- 补齐 `zh_hk` 与 `zh_tw` 的 `cosmic_nightfall_descent` Slash Art 本地化，并修正四个语言文件中魔龙夜陨 JEI 描述仍写 `slashblade:none` 的旧内容。
- 明确 `sa_susanoo` / `sasukes-susanoo-advanced` 属于已废弃资源，本次迁移不会恢复。

### 2026-08-14 崩溃修复
- 修复加载旧存档中的 `slashblade:spiral_swords` 等拔刀剑投射物时，`inGround=true` 但 `inBlockState` 缺失所导致的服务端实体 tick 空指针崩溃。
- 在 SlashBlade 读取落地方块状态前按实体当前位置补全缺失状态，已有“进存档即崩”的问题实体无需手动删除，其他正常投射物逻辑不受影响。
- 追加 `inBlockState.equals(...)` 调用前的兜底修复，覆盖同一 tick 内先写入 `inGround`、尚未写入 `inBlockState` 就进入方块检测分支的 2.0.4 崩溃路径。
- 将上述兜底从调用前注入改为直接重定向 `Object.equals` 调用，避免 SlashBlade 2.0.4 在空 receiver 上先抛出 NPE。
- 为湮灭之刃持有者的 SlashBlade 召唤剑命中追加“幻影回响”机制：不修改命名刀纸面伤害和召唤剑基础伤害，命中后对主目标施加终末标记并在下一 tick 兜底执行，同时牵引附近少量合法目标并播放归墟连线视觉。
- 将 1.20.1 Forge 的无尽星空合成机制迁移到 1.21.1：保留血狱作为核心材料以及击杀数、耀魂值和锻造等级要求，并将自定义刀材料字段改为 SlashBlade Resharped 2.0.3 的 `items` 数组格式，修复配方数据加载失败。

### v2.7.2
- **无尽星空 (Infinity Stellaris) 移植适配**：
  - 迁移了 `infinity_stellaris.obj` 模型贴图以及 GUI 资源。
  - 对 1.21.1 目标工程的 `zh_cn.json`、`zh_tw.json`、`zh_hk.json` 及 `en_us.json` 进行了非破坏性追加，将无尽星空的全部翻译改写为 `annihilationbladeex` 命名空间并合并。
  - 对 `NamedBladeStacks` 数据配置进行适配，新增 `creativeGroup` 和 `item` 组件定义，契合 1.21.1 命名刀规范。
  - 新建 `ModEntities.java` 注册实体，在主类注册其总线。
  - 在 `ModSlashArts`、`ModSpecialEffects`、`ModComboStates`、`ModConfig` 中，合入了无尽星空对应的 `vacuum_decay_collapse` SA、四个特效、绝对湮灭圈状态及对应 Config 配置。
  - 重构了 `InfinityStellarisDefinitions.java`，完全采用 1.21.1 的 `BladeStateData` 组件和 `Enchantment` Holder 模式对无尽星空基础属性和附魔进行运行时绑定。
  - 重写了 `InfinityStellarisTooltipRenderer.java`，完成了 Tesselator begin/buildOrThrow 及 VertexBuilder 格式在 1.21 渲染系统中的重构。
  - 修复并适配了 `CurvatureRuptureLogic.java` 与 `InfinityStellarisLogic.java` 中的 `PlayerTickEvent.Post` 等事件总线签名。
  - 在 `EntropyDissolutionLogic.java` 与 `GammaThunderburstLogic.java` 中，将事件监听更改为兼容 1.21 取消机制的 `LivingIncomingDamageEvent`。

### 1.20.1 Forge 到 1.21.1 NeoForge 完整移植 Changlog
- **网络层重构 (Network API)**：
  - 将 1.20.1 Forge 的 `InfinityStellarisAiErasurePacket` 转换为 1.21.1 NeoForge `CustomPacketPayload` 协议标准，通过 `StreamCodec.composite` 和 `PayloadRegistrar` 完成注册与多线程安全调度。
- **客户端按键与交互 (Client Event & KeyBinding)**：
  - 移植 `InfinityStellarisAiErasureKeyHandler`，适配 1.21.1 的 `ClientTickEvent.Post` 与 `RegisterKeyMappingsEvent` 客户端按键绑定。
- **逻辑与机制完善 (Logic & Restorations)**：
  - 恢复并升级 `CurvatureRuptureLogic` 中的 AI 擦除开关与生物 AI 恢复/还原机制 (`releasePlayer` 与 `restoreMob`)，修复了周围 Mob 无条件永久卡 AI 的缺陷。
- **资源与本地化合并 (Resources & Localization)**：
  - 将 1.20.1 的全套语言文件（`zh_cn`, `en_us`, `zh_hk`, `zh_tw`）及无尽星空拔刀剑仪式配方 JSON (`annihilationbladeex.infinity_stellaris.json`) 完整合并并转换为 `annihilationbladeex` 命名空间规范。
