# AnnihilationBladeEX 2.9.4-hotfix1

湮灭之刃的 1.21.1 NeoForge 移植版，公开命名空间为 `annihilationbladeex`，当前版本为 `2.9.4-hotfix1`。

## 环境

- Minecraft `1.21.1`
- NeoForge `21.1.228`
- Java `21`
- SlashBlade Resharped `2.1.9-1.21.1`
- Jupiter `2.3.7-1.21.1-neoforge`
- 本地依赖：`libs/SlashBlade_Resharped-1.21.1/SlashBladeResharped-2.1.9-1.21.1.jar`

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

产物位于 `build/libs/`，文件名应包含版本 `2.9.4-hotfix1`。

## Changelog
### 2.9.4-hotfix1 — 修复 SlashBlade Resharped 2.1.9 运行时崩溃（NoSuchMethodError）
- **崩溃现象**：进入世界后按 E 打开创造模式物品栏即崩溃，`java.lang.NoSuchMethodError: mods.flammpfeil.slashblade.capability.slashblade.BladeStateData.<init>(...)`（18 参描述符），抛出点位于 `AnnihilationBladeDefinitions.applyRuntimeStats`，调用链为 `ModCreativeTabs` → `createFromLookup` → `applyRuntimeStats`。报告来自 FCL（Android）启动器玩家，其运行环境为 SlashBlade Resharped `2.1.9-1.21.1`。
- **根因**：工程编译依赖仍为 `libs/SlashBladeResharped-2.0.7-1.21.1.jar`（18 参构造），而 `neoforge.mods.toml` 中 `slashblade` 依赖 `versionRange="[2.0.7-1.21.1,)"` 未设上界，放行了 2.1.9。2.1.9 在 `BladeStateData` record 第 10 个分量位置新增 `Optional<List<SwordType>> defaultSwordTypes`（`SwordType` 为 2.1.x 全新枚举，2.0.7 中不存在该类），构造函数由 18 参变为 19 参，按 2.0.7 编译出的 18 参 `invokespecial` 在 2.1.9 运行时无法解析。
- **修复**：
  - `build.gradle` 的 `implementation` 与 `localRuntime` 切换为官方 Jar `libs/SlashBlade_Resharped-1.21.1/SlashBladeResharped-2.1.9-1.21.1.jar`（2.0.7 旧 Jar 予以保留，未删除，不再参与构建）。
  - 在 `AnnihilationBladeDefinitions`、`BloodPrisonDefinitions`、`NightfallDragonDefinitions`、`InfinityStellarisDefinitions`、`LoliBladeDefinitions` 五处 `new BladeStateData(...)` 中，于 `defaultBewitched` 与 `destructable` 之间补入 `Optional.empty()`，与 2.1.9 字段顺序对齐。取空是为保持 2.0.7 时期的既有行为——彼时尚无 `defaultSwordTypes` 字段，五把刀均不声明默认剑类型，`SwordType.from()` 的自动判定不受影响。
  - 删除 `assets/annihilationbladeex/models/item/annihilation_blade.json` 与 `nightfall_dragon.json` 中无效的 `overrides` 块：谓词 `"slashblade:user": "exist"` 的值类型为字符串，触发 `JsonSyntaxException: Expected slashblade:user to be a Float`，导致两把刀模型加载失败（物品显示缺失模型）；且该谓词在 SlashBlade 2.1.9（`ClientHandler`）与本模组客户端注册（`AnnihilationBladeEXClient`）中均恒返回 0，仅用于 `BladeModel.user` 渲染副作用，引用的 `slashblade:eproudsoul` 模型在 2.1.9 中亦不存在，整块为历史遗留死代码。
- **依赖下限收紧**：`neoforge.mods.toml` 中 `slashblade` 的 `versionRange` 由 `[2.0.7-1.21.1,)` 提升为 `[2.1.9-1.21.1,)`，避免低版本 SlashBlade 环境下加载后复现同一 `NoSuchMethodError`。
- **跨版本兼容性核查**：已逐一核实以下 API 在 2.1.9 中均兼容，无需额外改动——`BladeStateAccess` 全系方法（`of`/`getData`/`getDataOrDefault`/`setData`/`updateData`/`ensureRuntimeComponent`）、`SlashBladeDataComponents.BLADE_STATE_DATA`、`CarryType.NINJA`、`SlashBlade.getSlashBladeDefinitionRegistry` 的 `Level` 与 `HolderLookup.Provider` 两种重载、`SlashBladeDefinition.REGISTRY_KEY` 与 `getBlade` 两种重载、`SpecialEffect(int, boolean, boolean)`、`TargetSelector.test`、`SlashBladeTEISR` 与 `BladeModel` 构造器及 `BladeModel.user` 字段、`ComboState.Builder`/`motionLoc`/`TimeLineTickAction.getBuilder()`/`DefaultResources.ExMotionLocation`、`RegistryEvents` 实体常量；`EntityAbstractSummonedSwordMixin` 依赖的 `inGround`/`inBlockState` 字段与 `tick` 在 2.1.9 中均在（崩溃日志显示游戏已进入 gameplay，`required=true` 的 Mixin 全部应用成功）。
- **验证方式**：`./gradlew.bat --no-daemon build --offline --console=plain` 构建通过（仅 2 处与本问题无关的 `EventBusSubscriber#bus` 弃用警告）；反汇编产物确认 5 处 `BladeStateData.<init>` 描述符均为 19 参 `(Ljava/lang/String;FIIIZZLnet/minecraft/resources/ResourceLocation;ZLjava/util/Optional;ZLnet/minecraft/resources/ResourceLocation;Lmods/flammpfeil/slashblade/client/renderer/CarryType;IZLnet/minecraft/world/phys/Vec3;Ljava/util/Optional;Ljava/util/Optional;Ljava/util/List;)V`，与官方 2.1.9 Jar 的规范构造器逐一比对一致；Jar 内 `neoforge.mods.toml` 的 `version="2.9.4-hotfix1"` 与 `slashblade` 下限 `[2.1.9-1.21.1,)` 均已生效。
- **部署提醒**：玩家需将 SlashBlade Resharped 同步升级至 `2.1.9-1.21.1` 或更高；旧版 Jar 与本产物 `mod_id` 相同，**不可同时放入 mods 目录**。

### 2.9.4-1.21.1-neoforge — 附魔问题修复
- 用户已审查附魔修复，版本更新为 `2.9.4-1.21.1-neoforge`。
- 默认附魔仅在创建时赋予；终焉、血狱、无尽星空、萝莉之刃和魔龙夜陨的 Tick、攻击、SA、绑定巡检及形态切换保留玩家当前附魔。
- 新刀创建兜底使用注册表初始化默认附魔；无尽星空创造栏使用注册表创建。
- 目标分支为 `1.21.1-neoforge`。由于 `2.9.4` Tag 已用于 Forge 版，NeoForge 版使用独立 Tag `2.9.4-1.21.1-neoforge`；README 本地维护，不加入自动推送。
- 本次按用户确认完整同步工程：包含此前未上传的萝莉之刃与魔龙 Tooltip，清理旧移植重复文件，升级并纳入构建必需的 SlashBlade Resharped `2.0.7-1.21.1` 依赖，修复 `BladeStateData` 构造兼容性。
- 发布工作区基于远程 `1.21.1-neoforge` 创建，Java 21 完整构建通过；Jar 内版本、完整读取和 SHA-256 校验通过。保留原始工作区已有暂存内容，仅在独立工作区提交发布代码。
- 发布完成：提交 `95e28773e6427fd990d1278f1baa5d571a987838` 已推送到 `1.21.1-neoforge`，Tag 为 `2.9.4-1.21.1-neoforge`，正式 Release 地址为 https://github.com/RLlufee/AnnihilationBladeEX/releases/tag/2.9.4-1.21.1-neoforge ，附件为 `slashblade-finale-blade-sanctum-2.9.4-1.21.1-neoforge.jar`。GitHub 附件 SHA-256 与本地一致：`b35721394c17c924beca4817fece65e4235920f2858f3808ffd4237da4466ad1`；分支 Ahead/Behind 为 0/0，README 未提交。

### 2026-10-10 修复运行时默认附魔被补回
- NeoForge 1.21.1 未整份覆盖命名刀模板，但终焉、血狱、无尽星空、魔龙夜陨和萝莉之刃的维护流程反复调用 `stack.enchant(..., 10)`，导致删除或降低的默认附魔在 20 tick 刷新、攻击、SA 准备、绑定巡检或形态切换时被补回。
- 拆分新武器创建与已有武器维护：默认附魔由命名刀数据包初始化，代码兜底仅在有注册表上下文的新刀创建时赋予；维护、攻击与形态切换保留玩家当前附魔组件。
- 修复终焉、血狱、魔龙夜陨在数据包定义缺失时未使用世界注册表的创建兜底；无尽星空创造栏改为使用注册表创建，避免移除运行时补齐后新刀缺少默认附魔。
- 保留身份、模型、SA/SE、形态、成长数据和防折断维护；修复已通过用户审查，发布版本见上方 2.9.4 记录。
- 验证：Java 21 下 `./gradlew.bat --no-daemon build --console=plain` 构建成功；反汇编确认五把刀的维护入口、萝莉刀绑定及魔龙形态切换调用链不再调用附魔初始化，最终 Jar 中五份命名刀默认附魔定义完整。未进行游戏内实测。
- 构建环境排错：备份并重新生成 NeoForm 中引用旧 `C:/Users/VOS_User/.gradle` 路径的 `listLibraries` 缓存，下载缺失的 Jupiter 依赖；未改动项目构建配置。审查差异与验证记录位于 `.codex_backup/enchantment-review-20261010-231012/`。

### 2026-09-12 修复 SlashBlade Resharped 2.0.7 运行时崩溃（NoSuchMethodError）
- **崩溃现象**：进入世界后按 E 打开创造模式物品栏即崩溃，根因行为 `java.lang.NoSuchMethodError: mods.flammpfeil.slashblade.capability.slashblade.BladeStateData.<init>(...)`，抛出点位于 `AnnihilationBladeDefinitions.applyStats`，调用链为 `ModCreativeTabs` → `createFromLookup` → `applyStats`。
- **根因**：工程编译依赖是 `libs/SlashBladeResharped-2.0.3-1.21.1.jar`（17 参构造），而实际运行环境加载的是 `SlashBladeResharped-2.0.7-1.21.1.jar`。2.0.7 在 `BladeStateData` record 的第 10 个分量位置新增了 `boolean destructable`，构造函数由 17 参变为 18 参，导致按 2.0.3 编译出的 17 参 `invokespecial` 在运行时无法解析。
- **修复**：
  - `build.gradle` 与 `libs/` 的编译依赖同步升级为 `SlashBladeResharped-2.0.7-1.21.1.jar`（2.0.3 旧 Jar 予以保留，未删除）。
  - 在 `AnnihilationBladeDefinitions`、`BloodPrisonDefinitions`、`InfinityStellarisDefinitions`、`LoliBladeDefinitions`、`NightfallDragonDefinitions` 五处 `new BladeStateData(...)` 中，于 `defaultBewitched` 与 `comboRoot` 之间补入 `destructable = false`，与 2.0.7 字段顺序对齐。取 `false` 是为保持 2.0.3 时期的既有行为——这些刀均为 `UNBREAKABLE` 且 `broken = false`，不应在断裂时销毁物品（2.0.7 的 `ItemSlashBlade#damageItem` 仅在 `isBroken() && isDestructable()` 时才 `stack.shrink(1)`）。
- **依赖下限收紧**：`neoforge.mods.toml` 中 `slashblade` 的 `versionRange` 由 `[2.0.3-1.21.1,)` 提升为 `[2.0.7-1.21.1,)`，避免在 2.0.3 环境下加载后复现同一 `NoSuchMethodError`。
- **跨版本兼容性核查**：对 2.0.3 与 2.0.7 两个 Jar 做了全量公共 API 逐类比对（各 383 个类）。除大量新增成员外，2.0.7 仅移除/变更了 5 项：`BladeStateData` 17 参构造、`AttackManager.getEnchantmentBonus(LivingEntity, Entity)`、`ItemSlashBlade.getName(ItemStack)`、JEI 分类的 `getBooleanText(boolean)`、`BladestandEvents.proudSoulEnchantmentProbabilityCheck(ProudSoulEnchantmentEvent)`，以及 `MoveCommandMessage#command()` 返回类型由 `int` 改为 `EnumSet<InputCommand>`。以上均未被本工程引用，故无需额外改动。`EntityAbstractSummonedSwordMixin` 所依赖的 `inGround` / `inBlockState` 字段及 `tick` 内的 `equals` 调用在 2.0.7 中位置未变，Mixin 依然有效。
- **验证方式**：`./gradlew.bat build --offline` 构建通过（仅 2 处与本问题无关的 `EventBusSubscriber#bus` 弃用警告）；并反汇编新产物逐一确认 5 处 `BladeStateData.<init>` 描述符均为 18 参，与 2.0.7 运行时 Jar 完全一致。
- **部署提醒**：测试实例 `1.21.1-NeoForge_21.1.250拔刀剑测试/mods/` 中目前仍放着旧命名的 `annihilationbladeex-2.9.2-1.21.1-neoforge.jar`；本次构建产物名为 `slashblade：finale-blade-sanctum-2.9.2-1.21.1-neoforge.jar`。两者 `mod_id` 相同，**不可同时放入 mods 目录**，否则会因重复 mod ID 启动失败，需先移除旧 Jar 再放入新产物。

### 2026-09-06 产出构建产物命名对齐
- 修复 `build.gradle` 中冗余的 `base { archivesName = mod_id }` 覆盖配置，使其正常生效 `gradle.properties` 中的 `mod_archive_name`。
- 构建产物 Jar 包名称与 1.20.1 Forge 原版对齐，由 `annihilationbladeex-2.9.2-1.21.1-neoforge.jar` 统一为 `slashblade：finale-blade-sanctum-2.9.2-1.21.1-neoforge.jar`。

### 2026-09-06 v2.9.2 魔龙夜陨 Tooltip 视觉强化与萝莉刀机制同步
- **魔龙夜陨旗舰级 Tooltip 渲染器移植**：将 1.20.1 旗舰级 Tooltip 视觉渲染完整移植至 1.21.1 NeoForge，包含动态 3x3 龙威权能全效果芯片组（9 核心龙术芯片：龙威领域、魔血寄生、逆鳞捕杀、外神伤痕、龙神躯、逆鳞守、万龙刃阵、破界斩、绝对领域）、交互式鼠标聚光灯追踪、全息激光扫描线、蜂窝科技矩阵背景、龙瞳能量核心、神陨形态色差故障错位（Chromatic Aberration Glitch Shift）以及双重逆向旋转龙威魔法阵。
- **NeoForge 1.21.1 渲染管线适配**：全面适配 `Tesselator.getInstance().begin(Mode.QUADS, ...)` / `BufferUploader.drawWithShader(builder.buildOrThrow())` 渲染流、`DataComponents.ENCHANTMENTS` 附魔映射与 `BladeStateAccess` 数据读取。
- **Tooltip 事件与配置接入**：新增 `NightfallDragonTooltipEvents` 响应客户端 `RenderTooltipEvent.Pre`，并在 `ModConfig.ClientTooltips` 中新增 `enableNightfallDragonRenderer` 开关配置。
- **萝莉之刃音效触发与处决链路重构**：将原先仅在 SA 空放时的音效，重构为当玩家手持萝莉之刃时，其释放的任何攻击（普通近战斩击、拔刀刀光与幻影剑、SE 扇形处决与 SA 全域处决）对合法目标造成任何伤害时播放 `loli_success` 音效，并加入同一 Tick 多目标防爆音节流防护。
- **萝莉之刃 SA 触发修复与生命周期状态清理**：在 `ModComboStates` 中为 `LOLI_AREA_EXECUTION_STATE` 补全 `TimeLineTickAction`（在动作第 8 tick 执行广域处决），`ModSlashArts` 改为调用 `prepareCast(player)`，并在登出与切换维度事件中补充清理战斗冷却与节流状态。
- **多语言补全与版本迭代**：在 `zh_cn`、`en_us`、`zh_hk`、`zh_tw` 中补齐魔龙夜陨 Tooltip 全部形态标题、副标题、形态印记与 9 大龙威权能芯片翻译；`gradle.properties` 与模组工程版本正式升级至 `2.9.2-1.21.1-neoforge`。
### 2026-08-29 Loli Blade 移植补齐
- 将 1.20.1 Forge 版 `loli_blade` 功能块补入 NeoForge 1.21.1：包含定义、战斗处决、防御保护、主人绑定、Theseus 兜底守护、原生 SE 扇形处决与 SA 范围处决。
- 新增 `ModSounds` 并注册 `annihilationbladeex:loli_success`，补齐 Loli Blade 模型、贴图、音效、JEI blade_desc、named_blade 与合成配方资源。
- 将 Loli Blade 的运行数据改为 1.21.1 DataComponent / SlashBladeDataComponents 写法，保留目标工程 `annihilationbladeex` 命名空间、Java 21 与 NeoForge 21.1.228 构建体系。
- 合并四个语言文件中的 Loli Blade、Slash Art、Special Effect、音效字幕与 Jupiter common config 文案；资源扫描已清理旧 `annihilationblade:` / `forge:` 引用。
- 验证命令：`./gradlew.bat compileJava --stacktrace`、`./gradlew.bat processResources --stacktrace` 与 `./gradlew.bat build --stacktrace` 已通过；仍建议进游戏实测模型显示、SA/SE 触发、玩家防御与音效播放。

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
