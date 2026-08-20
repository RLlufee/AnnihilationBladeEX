# Annihilation Blade · Terminus 2.7.2-1.20.1-forge

> 基于 SlashBlade / SlashBlade Resharped 的 Forge 1.20.1 拔刀剑扩展模组。模组围绕“终焉、裂界、坍缩、审判、血狱、宇宙法则、混沌魔龙”主题，提供四把命名刀、五个 SA、完整 SE 链路、可配置低风险参数，以及面向实战可读性的视觉与按键控制。

作者：青衣_璃

## 概览

当前版本包含：

- 主刀 `annihilationblade:annihilation_blade`
- 血狱刀 `annihilationblade:blood_prison`
- 无尽星空 `annihilationblade:infinity_stellaris`
- 魔龙夜陨 `annihilationblade:nightfall_dragon`
- 5 个 SA：`spatial_fracture`、`infernal_slaughter`、`vacuum_decay_collapse`、`nightfall_judgement_cut`、`dragon_head_charge`
- 23 个 SE 注册项，其中湮灭之刃使用 8 个终焉系 SE，血狱使用 3 个血狱系 SE，无尽星空使用 4 个宇宙法则系 SE，魔龙夜陨三形态使用 8 个魔龙系 SE
- 命名刀 datapack 定义
- Forge common 配置文件
- 中英语言资源 #感觉繁中没必要，遂删掉
- JEI SlashBlade 联动描述资源
- 断空闪现模式热键与动作栏提示
- 无尽星空 AI 删除热键，默认 `I`，默认关闭
- 魔龙夜陨形态切换热键，默认 `Z`
- 湮灭之刃 / 无尽星空专属 tooltip 渲染配置开关，默认开启
- SlashBlade 原生友伤 / PVP 判定统一接入

## 需求

| 项目 | 版本 |
| --- | --- |
| Minecraft | `1.20.1` |
| Forge | `47.4.21` |
| Java | `17` |
| 前置 | SlashBlade / SlashBlade Resharped |

## 武器

### `annihilationblade:annihilation_blade`

主武器“湮灭之刃 · 终焉”。命名刀定义位于：

`src/main/resources/data/annihilationblade/slashblade/named_blades/annihilation_blade.json`

| 项目 | 内容 |
| --- | --- |
| 基础攻击力 | `50.0` |
| 耐久 | `2000` |
| SA | `annihilationblade:spatial_fracture` |
| SE 数量 | `8` |
| 额外被动 | 绝对庇护、虚空飞行、终焉处决、永昼视界 |

湮灭之刃位于背包、主手或副手时，客户端会获得永昼视界效果；战斗判定统一遵循 SlashBlade 的原生攻击规则，默认不会误伤玩家、宠物或非敌对单位。

裂界会在湮灭之刃伤害生效 5 tick 后，以受击位置为中心处决周围所有合法目标，并继续连锁。连锁次数和以初始攻击者为中心的最大连锁范围可在 common config 的 `annihilation_blade.world_rift.chain_count` 与 `chain_range` 调整。

### `annihilationblade:blood_prison`

血狱刀“魔刀 · 血狱”。命名刀定义位于：

`src/main/resources/data/annihilationblade/slashblade/named_blades/blood_prison.json`

| 项目 | 内容 |
| --- | --- |
| 基础攻击力 | `16.0` |
| 耐久 | `2400` |
| SA | `annihilationblade:infernal_slaughter` |
| SE | `blood_leech`、`spirit_shield`、`phantom_mark` |

血狱围绕低血量风险、吸血、护盾、领域与幻影爆发构建。伤害、吸血、护盾触发和处决类逻辑保持写死，不开放到配置文件，避免破坏平衡或造成服务端误用。

### `annihilationblade:infinity_stellaris`

无尽星空是一把拥有终焉级合成门槛、不可平衡化的最终兵器命名刀。命名刀定义位于：

`src/main/resources/data/annihilationblade/slashblade/named_blades/infinity_stellaris.json`

| 项目 | 内容 |
| --- | --- |
| 基础攻击力 | `1000000.0` |
| 耐久 | `2147483647` |
| SA | `annihilationblade:vacuum_decay_collapse` |
| SE | `entropy_dissolution`、`curvature_rupture`、`gamma_thunderburst`、`cosmic_string_cut` |
| 额外被动 | 飞行、无敌、死亡无效、kill 防御、虚空坠落保护、永昼视界 |

无尽星空只在主手或副手持有时授予主动战斗权能；背包内也会提供基础生存兜底。它拥有 10 级剑、弓、弩相关附魔，但按设定排除火焰附加与火矢。当前配方需要四枚湮灭核心、一枚龙蛋、两座信标、一枚下界之星，以及一把杀敌 `5000`、耀魂 `25000`、精炼 `50` 的魔刀·血狱，定位为终局后的终焉级仪式合成。它拥有专属旗舰 tooltip renderer：收束后的外扩黑洞背景、外侧旋转白色魔法阵、动态白色绕框、绕框自转六芒星符号、星图叠层、宇宙谱线标题、权能芯片、属性记录与附魔回路会在客户端完整重绘。若玩家背包界面、Tooltip 增强或其它客户端渲染模组出现兼容问题，可在 common config 的 `client_tooltips` 分组中分别关闭湮灭之刃或无尽星空的专属 tooltip 渲染，关闭后回退为原版物品 tooltip。血狱当前没有独立的专属 tooltip 重绘器，默认仍走普通 tooltip。

战斗层面分为多套最终兵器逻辑：熵增蚀解按命中叠加热寂归零，所有无尽星空伤害会拉出 5 tick 粒子锁链，伽马霆爆会在玩家周围 128 格内连续 3 tick 落下自定义彩色闪电。曲率撕裂的 AI 删除现在默认关闭，按 `I` 切换；开启且手持无尽星空时才会临时关闭符合 SlashBlade 友伤/PVP 配置的合法目标 AI，停止手持或关闭后恢复。宇宙弦切现在作为无尽星空自带 SE，通过 SlashBlade 原生斩击事件触发，不再占用潜行右键。它们位于 `QWQ.QingYi.annihilationblade.infinity_stellaris` 包下，不复用湮灭之刃的终焉处决体系。

### `annihilationblade:nightfall_dragon`

魔龙夜陨是一把围绕混沌魔龙、黯焰寄生与空间龙威设计的多形态命名刀。命名刀定义位于：

`src/main/resources/data/annihilationblade/slashblade/named_blades/nightfall_dragon.json`

| 项目 | 内容 |
| --- | --- |
| 基础攻击力 | `22.0` |
| 耐久 | `2400` |
| 第一形态 SA | `annihilationblade:nightfall_judgement_cut` |
| 第二形态 SA | `annihilationblade:dragon_head_charge` |
| 第三形态 SA | `slashblade:none` |
| 第一形态 SE | `demonic_blood_parasite`、`outer_god_scar` |
| 第二形态 SE | 继承 `demonic_blood_parasite`、`outer_god_scar`，追加 `dragon_pressure_domain`、`reverse_scale_hunt` |
| 第三形态 SE | 继承 `demonic_blood_parasite`、`dragon_pressure_domain`，追加 `dragon_god_body`、`absolute_annihilation_domain`、`myriad_dragon_blade_storm`、`world_cleaving_slash` |

默认形态为【封印·淬血】，合法攻击与非外神伤痕、非逆鳞剑阵、非灭世龙刃的魔龙幻影剑命中会叠加无上限魔血印记，施加最高凋零 V、失明和 5% 最大生命值额外魔法伤害；已被黯焰标记的目标会让后续合法魔龙刀攻击走真实伤害路径。封印形态拥有专属 SA【夜陨次元斩】：以玩家为中心扫描 20 格内合法目标，在目标脚下或随机补位点每 5 tick 生成 1 个原版次元斩实体，总计 20 个。按 `Z` 可在【封印·淬血】、【觉醒·龙魂复苏】与【终焉·神陨夜陨】之间三态循环，第三形态直接开放。觉醒形态继承封印形态的魔血寄生与外神伤痕，持有时额外获得速度 III、力量 III、夜视 III、伤害吸收 III，挥刀会释放金紫逆鳞剑阵并短暂获得抗性提升 III；觉醒形态 SA【龙魂冲撞】会发射放大的 `dragon_head.obj` 龙头，伴随龙吼沿玩家目视方向飞行 200 格，对路径合法目标造成 2000 点魔法伤害，并把命中的目标吸到龙头前方一路撞飞。

第三形态定位为湮灭之刃与无尽星空之下的魔龙系终局形态，仍保留 `slashblade:none`，不注册未设计的 SA【神陨·宇宙夜陨降临】。终焉态继承前两态中不重复的核心特点：保留魔血/黯焰标记与龙威速度、力量、夜视，但低阶 `outer_god_scar` 与 `reverse_scale_hunt` 两套挥刀召剑不再重复挂载，由终焉态的灭世龙刃和贯穿刀波上位替代；觉醒态原本的伤害吸收也由创世龙盾统一承接。背包内存在第三形态魔龙夜陨时即可获得创世神体兜底：拦截可捕获伤害与死亡、刷新生命/饥饿、清除负面效果、提供虚空坠落保护与飞行，并把受到的合法伤害反弹给攻击者；第三态造成的合法伤害会治疗玩家，溢出部分转为最高 200 点生命值的创世龙盾，避免吸收生命条过量渲染。手持第三形态时每秒展开 64 格终焉龙域，最多处理 128 个合法目标，剥离增益与吸收盾、冻结行动、造成当前生命百分比伤害并处决半血以下目标；挥刀会同时释放 20 柄灭世龙刃和 72 格撕裂苍穹剑气，撕裂苍穹最多落下 16 道紫色伽马同源实体闪电。所有广域效果均遵循 SlashBlade 原生 PVP / 友伤目标判定，冻结 Mob 记录原始 NoAI 并在停止手持终焉形态、退出或换维度时恢复；第三态视觉粒子每维度每 tick 上限为 500 个。

## SA

### `空间破碎` / `Spatial Fracture`

湮灭之刃绑定的主 SA。触发后会沿玩家视线寻找裂隙中心，生成空间裂环、裂界蛛网、传送门粒子、闪电散射和剑雨演出。

逻辑上会优先锁定视线前方落点；如果准星路径上存在合适目标，则以目标中心作为裂隙焦点。命中实体会逐个执行终焉处决，并尽量走 SlashBlade 的真实击杀路径来维持击杀计数。

已开放配置项包括最大距离、裂隙半径、视线扫描步长、采样半径、锁定半径、备用搜索半径、目标上限、可视化目标数、斩击线数量和视觉倍率。

### `炼狱杀戮` / `Infernal Slaughter`

血狱刀绑定的 SA。触发后展开血狱领域，并同步客户端领域覆盖效果。领域持续期间，玩家斩击会在领域内选取敌对单位进行穿梭打击，并记录领域内造成的伤害，用于结束时的治疗反馈。

已开放配置项包括领域持续时间、领域半径、边界粒子刷新间隔、玩家血气粒子间隔、领域脉冲间隔和视觉倍率。

### `绝对湮灭圈` / `Absolute Annihilation Zone`

无尽星空绑定的 SA，注册 ID 仍为 `annihilationblade:vacuum_decay_collapse` 以保持旧命名刀定义兼容。释放时从玩家视线 raycast，优先在目视方块上展开领域；若没有命中方块，则在视线前方生成备用领域。

领域为 `128×128` 水平正方形，高度约 `64` 格，持续 `100 tick`。领域内每 tick 扫描 SlashBlade 合法目标，进入者直接热寂处决并压制掉落物和经验；方块不再被替换为空气。视觉上使用正方形边界、角柱、中心坍缩粒子与每 tick 12 道自定义彩色闪电模拟真空衰变的剧烈幻化感。

### `夜陨次元斩` / `Nightfall Judgement Cut`

魔龙夜陨第一形态绑定的 SA，注册 ID 为 `annihilationblade:nightfall_judgement_cut`。释放时以玩家为中心扫描 20 格内的 SlashBlade 合法目标，优先记录目标脚下位置；若合法目标不足 20 个，则在半径内随机位置补位。

实际演出按序列节流执行：每 5 tick 生成 1 个原版 `EntityJudgementCut` 次元斩实体，总计 20 个后停止。视觉上会表现为敌人脚下和周围随机点位一个接一个浮现次元斩，避免瞬间刷满 20 个实体造成视觉和性能压力。

## SE

| 名称 | 类型 | 表现 |
| --- | --- | --- |
| `断空` / `Dankong` | 瞬移连斩 | 在多个目标之间连续闪现并逐个斩杀，最后返回原位 |
| `裂界` / `World Rift` | 范围裂隙 | 以被命中目标为中心打开裂界，牵引并处决半径内敌对单位 |
| `归墟回响` / `Terminus Echo` | 前向回声 | 沿面朝方向连续释放多波回响斩击 |
| `虚无权域` / `Void Dominion` | 大范围领域 | 在前方区域展开裂界并逐个清场 |
| `因果坍缩` / `Causality Collapse` | 连锁审判 | 从首个目标开始按最近目标续接，生成因果锚点与桥接斩线 |
| `星寂裁决` / `Starless Judgement` | 直线裁决 | 在前方展开裁决波带，按投影判定沿途处决 |
| `幻影审判` / `Phantom Judgement` | 召剑审判 | 先环绕搜索，再以召唤剑落下集中打击 |
| `归墟天诏` / `Abyssal Decree` | 高位裁定 | 在头顶构筑冠冕后，从高空逐个降下审判 |
| `嗜血` / `Blood Leech` | 血狱被动 | 配合血狱主逻辑提供吸血与风险收益 |
| `源流灵盾` / `Spirit Shield` | 血狱被动 | 低血量时提供护盾与短时增益 |
| `幻影印记` / `Phantom Mark` | 血狱被动 | 累积标记后触发幻影剑爆发 |
| `熵增蚀解` / `Entropy Dissolution` | 无尽星空被动 | 每次合法伤害追加扣除 10% 最大生命，同目标十层后触发热寂归零与移除兜底 |
| `曲率撕裂` / `Curvature Rupture` | 无尽星空控场 | 默认关闭，按 `I` 切换；开启且手持时冻结 25 格内符合 SlashBlade 友伤/PVP 配置的合法目标，并在停止手持或关闭后恢复 AI |
| `伽马霆暴` / `Gamma Thunderburst` | 无尽星空爆发 | 无尽星空伤害触发，玩家周围 128 格随机 12 道自定义彩色闪电，每 tick 一轮，持续 3 tick |
| `宇宙弦切` / `Cosmic String Cut` | 无尽星空斩击 SE | 通过 SlashBlade 原生斩击事件触发，只在玩家周围生成 5×5×5 局部星线，并处决 128 格立方内合法目标 |
| `魔血寄生` / `Demonic Blood Parasite` | 魔龙封印被动 | 合法命中与非外神伤痕、非逆鳞剑阵、非灭世龙刃的魔龙幻影剑叠加魔血/黯焰，施加凋零、失明、5% 最大生命魔法伤害和后续真实伤害 |
| `外神伤痕` / `Outer God Scar` | 魔龙封印召剑 | 挥刀生成暗紫裂隙和伴生幻影剑，短暂环绕后追猎合法目标 |
| `龙威重域` / `Dragon Pressure Domain` | 魔龙觉醒被动 | 手持觉醒形态获得速度 III、力量 III、夜视 III、伤害吸收 III |
| `逆鳞剑阵` / `Reverse Scale Hunt` | 魔龙觉醒召剑 | 挥刀释放金紫逆鳞剑阵，拥有更高速度与贯穿，并授予短时抗性提升 III |
| `终焉神像` / `Dragon God Body` | 魔龙终焉神体 | 背包内存在终焉形态即可获得死锁兜底、飞行、负面清除、虚空坠落保护、反弹、吸血与最高 200 点创世龙盾 |
| `终焉结界` / `Domain of Absolute Annihilation` | 魔龙终焉领域 | 每秒扫描 64 格内最多 128 个合法目标，剥离增益与吸收盾、冻结行动并处决半血以下目标 |
| `万刃龙魂` / `Myriad Dragon Blade Storm` | 魔龙终焉召剑 | 挥刀释放 20 柄暗金/星空紫灭世龙刃追猎合法目标 |
| `灭界龙威` / `World-Cleaving Slash` | 魔龙终焉剑气 | 普通挥刀追加 72 格撕裂型虚空剑气，拉扯并压制路径内合法目标，最多落下 16 道紫色伽马同源实体闪电 |

## 断空控制

`断空` 是高速连续闪现 SE。为了避免日常杀怪时不断闪现导致视野混乱，当前版本提供两层保险：

- 按住 Shift 时，断空不会开始新的闪现序列。
- 断空序列进行中按住 Shift，会中断并返回起点。
- 新增可配置按键“切换断空闪现模式”，默认 `Left Ctrl`。
- 热键只在玩家主手或副手手持湮灭之刃时生效。
- 按下热键后，屏幕下方动作栏会显示本地化提示，例如“切换断空闪现模式：当前闪现：开 / 关”。

提示文本已经使用语言文件，不再硬编码中文，便于其他语言翻译和样式调整。

## 魔龙夜陨控制

`魔龙夜陨` 默认使用【封印·淬血】形态。按 `Z` 会发送服务端校验包，只在玩家主手或副手持有魔龙夜陨时切换形态。

- 【封印·淬血】：挂载 `demonic_blood_parasite` 与 `outer_god_scar`，SA 为 `nightfall_judgement_cut`。
- 【觉醒·龙魂复苏】：继承 `demonic_blood_parasite` 与 `outer_god_scar`，并追加 `dragon_pressure_domain` 与 `reverse_scale_hunt`。
- 【终焉·神陨夜陨】：挂载 `dragon_god_body`、`absolute_annihilation_domain`、`myriad_dragon_blade_storm` 与 `world_cleaving_slash`。
- 【觉醒·龙魂复苏】与【终焉·神陨夜陨】当前保持 `slashblade:none`，避免与后续上位 SA 设计冲突。
- 第三形态按上位替代规则继承不重复核心 SE，低阶召剑由终焉态技能覆盖。
- 切换成功后仅更新形态，不再显示动作栏提示。

## 配置文件

首次启动后，Forge 会生成：

`config/annihilationblade-common.toml`

配置文件只开放低风险参数：

- 范围：例如搜索范围、领域半径、裁决宽度。
- 间隔：例如连续闪现间隔、回响波次间隔、领域粒子刷新间隔。
- 冷却：例如各 SE 的触发冷却。
- 数量：例如最大目标数、召唤剑数量、可视化目标数。
- 视觉倍率：例如粒子数量、视觉半径或演出密度。
- 客户端兼容开关：例如 `client_tooltips.enable_annihilation_blade_renderer` 与 `client_tooltips.enable_infinity_stellaris_renderer`，默认开启；关闭后对应刀使用原版 tooltip 渲染。

不会开放的内容：

- 伤害倍率
- 终焉处决逻辑
- 血狱吸血与护盾核心数值
- 无敌、庇护、飞行等安全相关逻辑
- SlashBlade 真实击杀路径

每个配置项都带有中文 and 英文注释，并写明建议最小 / 最大值。Forge 也会通过 `defineInRange` 对配置值做硬范围限制，避免新玩家填入极端数值导致卡顿或逻辑异常。

### 配置分组

主要分组如下：

```toml
[client_tooltips]
[annihilation_blade.spatial_fracture]
[annihilation_blade.dankong]
[annihilation_blade.world_rift]
[annihilation_blade.terminus_echo]
[annihilation_blade.void_dominion]
[annihilation_blade.causality_collapse]
[annihilation_blade.starless_judgement]
[annihilation_blade.phantom_judgement]
[annihilation_blade.abyssal_decree]
[blood_prison.domain]
[blood_prison.phantom_burst]
[infinity_stellaris]
```

## 本地化

语言资源位于：

`src/main/resources/assets/annihilationblade/lang/`

当前包含：

- `zh_cn.json`
- `zh_tw.json`
- `zh_hk.json`
- `en_us.json`

断空按键名称、动作栏提示、物品名、SA / SE 名称、物品描述和 JEI 说明文案均已接入语言文件。

## JEI SlashBlade 联动

当前版本为 `jei_slashblade` 添加了资源级联动，并补充了两把命名刀、SA 与 SE 的本地化说明：

- `assets/annihilationblade/blade_desc/annihilation_blade.json`
- `assets/annihilationblade/blade_desc/blood_prison.json`
- `assets/annihilationblade/blade_desc/infinity_stellaris.json`
- SA 描述键：`slashblade.slash_art.annihilationblade.*.desc`
- SE 描述键：`se.annihilationblade.*.desc`

安装 JEI SlashBlade 后，可以在 JEI 中查看四把命名刀的简介，并在 SA / SE 分类里阅读湮灭之刃、血狱、无尽星空和魔龙夜陨相关效果说明。

## 注册与源码路径

SA / SE 注册位置：

- `src/main/java/QWQ/QingYi/annihilationblade/registry/ModSlashArts.java`
- `src/main/java/QWQ/QingYi/annihilationblade/registry/ModSpecialEffects.java`

主要实现路径：

- `src/main/java/QWQ/QingYi/annihilationblade/annihilation_blade/`
- `src/main/java/QWQ/QingYi/annihilationblade/blood_prison/`
- `src/main/java/QWQ/QingYi/annihilationblade/infinity_stellaris/`
- `src/main/java/QWQ/QingYi/annihilationblade/common/`
- `src/main/java/QWQ/QingYi/annihilationblade/config/ModConfig.java`
- `src/main/java/QWQ/QingYi/annihilationblade/network/`

## 拔刀剑 OBJ 模型制作参考

本节根据 MC 百科教程《适用于“拔刀剑：重锋”的拔刀剑 obj 模型制作教程》整理，原文地址：

`https://www.mcmod.cn/post/5202.html`

该教程面向 SlashBlade: Resharped 的附属开发或资源替换场景，重点说明如何在 Blender 中制作可被重锋读取的拔刀剑 OBJ 模型。原教程标注为 CC BY-NC-SA 协议；本 README 仅做项目内开发笔记式转述，不直接搬运原文。

### 适用目标

- 为 SlashBlade: Resharped 制作新的拔刀剑 OBJ 模型。
- 将外部武器模型整理成拔刀剑可识别的结构。
- 为附属模组或资源替换准备 `model/*.obj` 与对应 `model/*.png` 贴图。
- 检查本项目三把命名刀的模型资源是否满足重锋的分组和显示要求。

### 准备材料

制作前至少需要准备：

- Blender，并掌握基础导入、导出、移动、旋转、缩放、合并与拆分操作。
- 一份目标武器的 OBJ 模型，可来自自制模型或合规授权的模型资源站。
- 与目标 OBJ 匹配的 PNG 贴图，通常与模型文件一起提供。
- 一份基础拔刀剑 OBJ 或 Blender 参考模型，用于对齐位置、比例和分组。

注意：外部模型和贴图必须确认授权来源，不能随意把不明来源资源打包进发行版。

### 基准模型原则

制作时应先把基础拔刀剑模型和自己的目标武器模型同时导入 Blender。基础模型只作为坐标、比例、姿态和分组参考，不能移动、旋转或缩放，否则导出的模型在游戏内会出现位置偏移、尺寸异常或持刀姿态不对。

目标武器模型需要围绕基础模型进行调整，使刀身、刀柄、刀鞘等关键部件与基础拔刀剑的位置尽量重合。可以理解为：基础模型是“尺子”，新模型必须去贴合这把尺子，而不是反过来改尺子。

### 制作流程

推荐流程如下：

1. 在 Blender 中导入基础拔刀剑模型。
2. 导入目标武器 OBJ 与贴图。
3. 保持基础模型不动，只移动、旋转、缩放目标模型。
4. 让目标模型的刀身、刀柄、刀鞘位置对齐基础模型。
5. 按拔刀剑需要复制或裁切目标模型，制作第三人称、物品栏、损坏刀、碎片等变体。
6. 按固定分组名称重命名各个模型部分。
7. 确认贴图路径、UV、透明区域和材质显示正常。
8. 删除作为参考的基础拔刀剑模型，只保留最终新模型。
9. 导出 OBJ，并把 OBJ 与 PNG 放入资源包的 `assets/<modid>/model/` 目录。
10. 在命名刀 JSON 的 `render.model` 和 `render.texture` 中指向新资源。

本项目示例：

```json
"render": {
  "model": "annihilationblade:model/annihilation_blade.obj",
  "texture": "annihilationblade:model/annihilation_blade.png"
}
```

### 标准分组命名

重锋读取 OBJ 时依赖固定分组名称。制作或修改模型时，需要保证关键部分按下表命名：

| 分组名 | 用途 |
| --- | --- |
| `sheath` | 第三人称刀鞘 |
| `blade` | 第三人称完整刀身 |
| `blade_damaged` | 第三人称损坏刀身 |
| `blade_fragment` | 刀断裂后飞出并落地的碎片 |
| `effect` | 使用 SA 蓄力或按住时，刀鞘发光相关部分 |
| `item_blade` | 物品栏中的完整刀 |
| `item_damaged` | 物品栏中的损坏刀 |
| `item_bladens` | 原教程标注为用途不明；通常保留或按原模型结构处理，避免兼容性问题 |

本项目当前 `annihilation_blade.obj` 还包含以下发光或扩展分组：

- `blade_luminous`
- `item_blade_luminous`
- `item_bladens_luminous1`
- `item_back`

这些分组可以用于发光层、物品栏背板或特殊显示层。修改时应尽量保留已有分组结构；如果要新增几何，优先复制同类分组的坐标、UV 与材质习惯，避免游戏内出现不可见、错位或贴图错乱。

### 变体制作要点

拔刀剑不是只显示一把完整刀。模型通常需要同时服务于第三人称持刀、物品栏图标、损坏状态和碎片掉落等多个场景。因此，制作模型时不能只把完整武器导出一次，还需要根据基础模型的位置关系制作对应变体。

建议检查：

- `blade` 和 `sheath` 是否适合第三人称显示。
- `item_blade` 是否适合 GUI、物品栏和 Item Zoom 视角。
- `blade_damaged` 与 `item_damaged` 是否能表现损坏状态。
- `blade_fragment` 是否是小段碎片，而不是完整刀身。
- `effect` 或 luminous 分组是否和发光贴图区域对应。

### 贴图与 UV 注意事项

OBJ 模型的复杂度只决定轮廓和部件层次，贴图决定大部分近看质感。制作贴图时应注意：

- PNG 尺寸可以提升，但必须保持 UV 岛对应关系，否则模型会贴错区域。
- 透明区域不要被误填色，尤其是物品栏背板、发光层和碎片边缘。
- 背板、刀身、刀柄、护手最好有明确的材质差异，避免只靠单色渐变。
- 发光层可使用高对比冷色或亮色，但不宜覆盖所有区域，否则会显得廉价。
- 用外部素材时应重新调色、裁切和融合，使其服从武器主题，而不是像一张照片直接贴在模型上。

本项目“湮灭之刃 · 终焉”的大背板使用深空破碎行星素材时，处理思路是：先将素材压入背板 UV，再统一成黑曜紫与冷蓝色调，最后叠加星环、碎片、边框和符文线，让它成为终焉主题的一部分。

### Blender 基础快捷键

教程中提到的常用快捷键可以作为最低限度备忘：

| 模式 | 快捷键 | 用途 |
| --- | --- | --- |
| 物体模式 | `G` | 移动模型 |
| 物体模式 | `R` | 旋转模型 |
| 物体模式 | `S` | 缩放模型 |
| 物体模式 | `Ctrl + J` | 合并多个模型 |
| 编辑模式 | `P` | 拆分模型 |

### 本项目维护建议

- 修改 `src/main/resources/assets/annihilationblade/model/*.obj` 前，先确认分组名称没有被破坏。
- 只改贴图时，优先保持原 PNG 的透明遮罩和 UV 岛布局。
- 如果要重做模型轮廓，建议先复制现有 OBJ 作为备份，再在 Blender 中按标准分组重做，而不是直接手写大量 OBJ 面片。
- 修改完成后至少运行：

```powershell
$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8'
./gradlew.bat --no-daemon processResources --console=plain
```

- 准备发布前再运行：

```powershell
$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8'
./gradlew.bat --no-daemon build --console=plain
```

## 构建

建议使用 Java 17：

```powershell
$env:JAVA_HOME='C:\Program Files\Zulu\zulu-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8'
./gradlew.bat --no-daemon clean build --console=plain
```

构建产物位于：

`build/libs/annihilationblade-2.7.2-1.20.1-forge.jar`

## Changelog

### 未发布

- 新增“魔龙夜陨”第二形态 SA【龙魂冲撞】：觉醒形态绑定 `annihilationblade:dragon_head_charge`，触发后使用 `dragon_head.obj` 与 `dragon_head.png` 渲染放大魔龙头颅，伴随末影龙咆哮沿玩家目视方向飞行 200 格；路径合法目标首次命中受到 2000 点魔法伤害，并被持续吸附到龙头前方随其撞飞。贴图文件名改为全小写以符合 Minecraft `ResourceLocation` 资源路径规则。
- 参考“暴怒”龙形投射物的客户端链路，给 `dragon_head_charge` 补充 Forge 自定义实体客户端生成工厂，并将 `dragon_head.obj` 的渲染分组改为 SlashBlade OBJ renderer 常用的 `base`，避免服务端伤害生效但客户端模型不显示。
- 补强“龙魂冲撞”客户端可见性排查：渲染器改为返回实际 `dragon_head.png` 贴图，并按“暴怒”参考的满亮 OBJ 渲染方式绘制 `base` 分组；同时在服务端生成、客户端实体创建和首次渲染处各输出一次诊断日志，用于区分网络同步问题与模型绘制问题。
- 修正“龙魂冲撞”放大龙头从模型内部/背面观察时不可见的问题：`dragon_head.obj` 会同时绘制正向与反向满亮 pass，避免单面剔除导致释放瞬间或贴近目标时看不到模型外壳。
- 排查“龙魂冲撞”OBJ 面结构：使用 Python 对 `dragon_head.obj` 全量扫描并执行三角面化重写，确认 19318 个 `f` 面本身已经全部为三角面，未发现需要拆分的四边面或多边面；原始备份移动到 `local_backups/`，不参与资源打包。
**新增功能 (Features)**
- 完成“魔龙夜陨”前两形态 SE 实装：第一形态【封印·淬血】挂载 `demonic_blood_parasite` 与 `outer_god_scar`，第二形态【觉醒·龙魂复苏】继承第一形态 SE，并追加 `dragon_pressure_domain` 与 `reverse_scale_hunt`。
- 新增默认 `Z` 键形态切换，客户端只在主手或副手持有魔龙夜陨时发送切换请求，服务端校验后更新 `bladeState`、颜色、翻译键与 SE 列表；当前切换不再显示动作栏提示。
- 第一形态命中逻辑支持玩家直砍与合法魔龙幻影剑实体：叠加无上限魔血层数，施加最高凋零 V、失明、5% 最大生命值额外魔法伤害，并让已标记目标后续承受真实伤害路径；`outer_god_scar` 的快速前刺伴生剑、`reverse_scale_hunt` 的逆鳞剑阵与第三态 `myriad_dragon_blade_storm` 的灭世龙刃不再叠加黯焰印记，避免召剑数量直接触发爆发连锁。
- 第二形态持有时刷新速度 III、力量 III、夜视 III、伤害吸收 III；挥刀释放金紫逆鳞剑阵并获得短时抗性提升 III，已移除攻击时围绕玩家生成的一圈白色/紫色粒子环。
- 完成“魔龙夜陨”第三形态【终焉·神陨夜陨】v1 落地：`Z` 键改为封印、觉醒、终焉三态循环，第三态继承 `demonic_blood_parasite` 与 `dragon_pressure_domain`，并追加 `dragon_god_body`、`absolute_annihilation_domain`、`myriad_dragon_blade_storm` 与 `world_cleaving_slash`。
- 第三态继承按“同类上位替代”去重：低阶 `outer_god_scar` 与 `reverse_scale_hunt` 不重复触发，由终焉灭世龙刃和贯穿刀波覆盖；觉醒态伤害吸收由创世龙盾统一承接。
- 第三形态按“终局但节流”实现：背包内存在终焉形态即可获得创世神体的可捕获伤害/死亡兜底、飞行、负面清除、虚空坠落保护、法则反弹、吸血与最高 200 点创世龙盾；终焉龙域仍要求手持，每秒扫描 64 格内最多 128 个合法目标，冻结并恢复 Mob AI，半血以下调用最终处决路径。
- 第三形态挥刀会释放 20 柄灭世龙刃与 72 格撕裂苍穹剑气，撕裂苍穹新增与无尽星空同源的紫色 GammaThunderbolt 实体闪电，单次最多 16 道，并锁定合法目标中心生成，避免近身时视觉落在持刀者身上；本次删除灭世龙刃命中后的微型黑洞与虚空残痕，降低敌群场景粒子压力；本次未注册或绑定尚未设计的 SA【神陨·宇宙夜陨降临】。
- 重构第三形态视觉性能边界：创世龙盾现在在生成、第三态刷新和退出第三态时都会钳制到 200 点生命值；第三态所有视觉粒子统一走每维度每 tick 500 个预算，敌人扎堆时后续粒子爆发会自动跳过。
- 优化“魔龙夜陨”粒子频率：龙威重域手持光效改为每 20 tick 迸发约 25 个粒子；魔血寄生命中魂火/黑曜粒子加入每目标冷却、每维度每 tick 爆发次数与粒子总量预算，当前预算进一步压到每维度每 tick 90 个粒子，避免敌群扎堆时无限制刷屏。
- 调整“魔龙夜陨”战斗细节：手持魔龙夜陨时实体攻击距离额外增加 3 格；黯焰印记每累计 20 层会复用血狱幻影印记的从天而降幻影剑爆发。
- 新增“魔龙夜陨”第一形态 SA【夜陨次元斩】：启动后以玩家为中心扫描 20 格内合法目标，优先在目标脚下每 5 tick 生成 1 个原版次元斩实体，目标不足时用半径内随机位置补足，总计 20 个后停止；第二、第三形态仍保持 `slashblade:none` 等待后续上位 SA 设计。
- 修复“魔龙夜陨”第一形态 SA 未挂载：补上无破坏 X 附魔，使其满足 SlashBlade 的 `BEWITCHED` 前置；手持旧存档中的魔龙刀时每秒自动补齐该状态，因此不需要重新获取物品。
- 修复“夜陨次元斩”随机补位埋入地面：随机点现在只会选取可站立地面上方连续三格无碰撞、无液体的空间；抽取失败时重试并回退至玩家附近的安全落点。
- 修复“魔龙夜陨”第三形态递归触发：灭世龙刃、终焉龙域、撕裂苍穹与反弹伤害不再反向叠加黯焰印记，避免第三态召剑和二段伤害无限连锁。
- 修复“魔刀·血狱”幻影印记来源判定：只有玩家主手直接使用血刀造成的伤害才叠加印记，排除魔龙刀、其它 SlashBlade 幻影剑或斩击实体误触发血刀印记爆发。

**开发环境 (Development)**
- 配置 Forge 1.20.1 子项目 Git 仓库 remote：`origin=https://github.com/RLlufee/AnnihilationBladeEX.git`，并新增 `.gitignore` 排除 Gradle 缓存、构建产物、本地依赖、IDE 状态与临时文件。

**资源与视觉 (Assets & Visuals)**
- 注册命名刀“魔龙夜陨” (`annihilationblade:nightfall_dragon`)，接入创造栏、本地化、命名刀数据与 `nightfall_dragon.obj/png` 模型贴图，用于进游戏预览黯夜魔龙主题美术效果；当前第一形态已接入自定义 SA，仍未添加合成配方。
- 重制“魔龙夜陨”模型路线：删除先前过度外放的程序化黯夜魔龙 OBJ 生成脚本，改为以 `infinity_stellaris.obj` 的稳定拔刀剑比例、分组和全三角面拓扑为基准，避免进游戏后显示成电锯、狼牙棒或不可正常拔出的异形轮廓。
- 在无尽星空基准模型上为“魔龙夜陨”追加专属 OBJ 修饰：近刀镡和柄尾区域增加短龙角护手、柄尾龙晶与贴刃薄魔焰翼片，并沿剑脊向剑尖延展低面数发光龙骨暗纹；新增面全部保持三角面，并挂入 `blade` / `blade_luminous` 分组，不改动主刀身与刀鞘的大比例结构。
- 依照无尽星空模型的原 UV 布局重绘 `assets/annihilationblade/model/nightfall_dragon.png`，输出 `1024x1024` 紫黑魔龙专属材质，保留透明度与明暗结构，替换为黯紫金属、亮紫裂纹与冷白魔焰高光，并为发光龙骨线追加专用高亮 UV 采样带。
- 新增“魔龙夜陨”客户端模型发光渲染事件：监听 SlashBlade `RenderOverrideEvent`，仅对该刀的 `luminous` 分组额外补画满光能量层，并复用重锋已有能量流动 RenderType 生成紫色魔焰流动效果。
- 调整“魔龙夜陨”物品图标表现：移除 `item_blade` / `item_damaged` / `item_bladens` 分组中继承自无尽星空的大型低层背板面，降低 Item Zoom 或物品栏预览中青蓝背框抢占紫色主题的问题，并进一步提亮紫色贴图与满光魔焰渲染层。
- 将“魔龙夜陨”的物品菱形背板从纯白底改为魔龙封印图：把外部紫黑魔龙头像素材融合进 `nightfall_dragon.png` 的背板 UV，保留紫色边框并叠加封印裂纹，使 Item Zoom 预览更接近湮灭之刃使用主题图像背板的处理方式。
- 补强“魔龙夜陨”的龙印细节与刀鞘动效：新增 `sheath_luminous` 刀鞘发光分组，并在 `blade_luminous` 追加细长龙脊印记；贴图中同步绘制亮紫符文采样带、暗紫龙鳞线和细高光，使刀身与刀鞘都能获得重锋 `luminous` / 能量流动渲染层。
- 参考外部项目 `sjap-adder-master` 中“终狱刀「绝念」”的 `item_blade_luminous` 物品光环做法，为“魔龙夜陨”新增独立 `nightfall_dragon_halo.png` 龙纹紫色光环贴图与 `item_dragon_halo_luminous` OBJ 分组；客户端渲染事件会在物品图标 `luminous` pass 中额外绘制一层独立满光紫色旋转光环，并移除不按中心旋转的能量流式光环层。
- 将“魔龙夜陨”的旋转光环贴图替换为用户绘制的高细节紫色龙纹魔法阵，并将原图黑色背景抠为透明 alpha；随后压低贴图 alpha、RGB 亮度与渲染叠色强度，保留外圈龙纹、符文、节点辉光和紫色泛光细节，同时避免光环抢过刀身主体。
- 调整“魔龙夜陨”刀镡附近的动态表现：确认物品光环平面中心位于 OBJ 原点，旋转中心并未偏移；将贴近刀镡的薄魔焰翼片从 `blade_luminous` 动态发光分组移回普通 `blade` 分组，只保留沿剑脊延展的龙骨线参与能量流动，避免护手附近出现不自然的流动/跳动观感。
- 重绘“魔龙夜陨”物品菱形背板贴图：移除旧背板中叠加的五叶草、雪花片、白色划痕和波纹干扰层；改用用户提供的模糊龙头菱形图作为主体，并从锁链封印图中抽取灰黑锁链层叠入菱形内部，达到“模糊魔龙被锁链封印”的背板效果，同时避免清晰龙头素材在 MC 物品图标中出戏。
- 将主武器“湮灭之刃 · 终焉”的模型贴图 `assets/annihilationblade/model/annihilation_blade.png` 从 `256x512` 提升至 `512x1024`。
- 重塑为黑曜紫终焉风格，加入裂界纹章背板、蓝白能量刃纹、紫青符文线、核心圆环高光与黑金属护柄阴影，减少与低阶新手武器的换色相似感。
- 修正大背板 `item_back` 实际 UV 采样区域，将深空破碎行星素材精准融合到右上贴图区，并叠加星环、碎片、星点与冷蓝边框，强化终焉级命名刀的视觉识别度。
- 回退先前过度外放的程序化 OBJ 重置，恢复 `annihilation_blade.obj` 的稳定基准结构，保留重锋标准分组、显示尺度和全三角面拓扑，避免第一人称遮挡、物品栏背板空框和模型错位。
- 移除“魔龙夜陨”第三形态常驻龙虚影粒子，保留克制的神体光环、64 格终焉龙域环与黑曜波纹；挥刀视觉改为贯穿式暗金/星空紫刀波，不新增 OBJ 实体渲染。
- 第三形态神体光环刷新频率由每 4 tick 再调整为每 8 tick，持刀/背包常驻光环粒子在上一轮基础上再次削弱约 50%；第三态全局视觉粒子预算同步由每维度每 tick 1000 降至 500。

**文档 (Documentation)**
- 根据 MC 百科 SlashBlade: Resharped OBJ 模型制作教程，新增拔刀剑 OBJ 模型制作参考，整理工具准备、基准模型原则、标准分组命名、变体制作、贴图 UV 注意事项与 Blender 基础快捷键。

### 2.7.1-1.20.1-forge

**修复 (Bug Fixes)**
- 修复湮灭之刃在生存模式下作为物品丢出时会立即消失的问题。现在生存模式按丢弃键会正常生成掉落物实体，不再被事件拦截取消。
- 保留湮灭之刃物品本身的数据刷新逻辑；掉落物重新捡起后仍会继续校正专属刀体标记、模型、贴图和运行时状态。

### 2.7.0-1.20.1-forge

**新增与重构功能 (Features & Refactoring)**
- **新增最终兵器拔刀剑「无尽星空」 (`annihilationblade:infinity_stellaris`)**：
  - 最终兵器定位：基础攻击力提升至 `1,000,000.0`，耐久提升至 `2,147,483,647` (不可合成、不可平衡化)。
  - 被动权能：手持或背包内可获得飞行、无敌、死亡兜底、虚空坠落保护及永昼视界等基础生存保障。
  - 附魔设定：保留剑、弓、弩相关 10 级附魔，按背景设定移除了火焰附加与火矢附魔。
  - 移除旧有的麦克斯韦妖虹吸与终极协议机制，将语言文件、Tooltip、JEI 描述均更新为最终兵器设定。
- **重构专属 SE 特性**：
  - `entropy_dissolution` (熵增蚀解)：每次伤害可叠加层数，10 层后触发热寂归零处决，并剥夺目标的所有免死与保护。
  - `curvature_rupture` (曲率撕裂)：使持刀者周围 25 格内的合法目标完全静止，永久锁死 Mob AI、寻路和行动速度；删除了此前可能导致卡顿的引力拉扯位移逻辑。
  - `gamma_thunderburst` (伽马霆爆)：无尽星空伤害触发，在玩家周围 128 格内连续 3 tick 落雷，每 tick 落下 12 道自定义彩色闪电。将其改为自定义实体与专用渲染器以提升效率，移除旧版拦截原版闪电渲染的 Mixin。
  - `cosmic_string_cut` (宇宙弦切)：重构为由原生斩击事件 (`DoSlashEvent`) 触发，删除了会抢占普通右键与长按 SA 的潜行右键入口。视觉上精简了远距大面积星线，仅保留玩家周围 5x5x5 的局部星线以保证视野。
- **重构专属 SA 技能**：
  - `vacuum_decay_collapse` (重构为「绝对湮灭圈」)：以目视方块或视线前方为中心展开 `128×128` (高约 64 格) 的正方领域，持续 100 tick。领域内合法目标入圈即死，压制掉落物与经验，且不再替换方块，保证游戏流畅度。

**视觉与 GUI 重构 (Visuals & UI Aesthetics)**
- **设计了顶级「无尽星空」专属 Tooltip 悬停渲染器**：
  - **背景与魔法阵**：
    - 采用黑洞专属暗色背景面板与星图叠层线框纹理。
    - 接入动态分段光带边框与沿边框公转加自转的白色六芒星/菱形符号。
    - 新增手绘白色大型复杂多维魔法阵（包含多层圆环、多边轨道、星形连线与刻度），由 GUI 渲染代码实时实时差速绘制。
    - 限制了外扩黑洞背景的露边范围，避免在背包或 Creative/JEI 视图中铺满过宽。
  - **流光标题与内容分区**：
    - 标题 `最终兵器：无尽星空` 改为基于安全采样的运行时逐字流动 RGB 宇宙谱线渲染，彻底修复浮点越界导致创造栏或背包渲染崩溃的问题。
    - 界面模块化排版：权能核心、刀体记录、宇宙法则芯片和附魔回路分区展示，优化不同分区的动态指示条为短轨短脉冲微动效。
    - 将 Tooltip 追加逻辑完全解耦并移至客户端专用事件类中，使核心战斗逻辑不再直接依赖客户端渲染 API。

**优化与修复 (Improvements & Bug Fixes)**
- **热键与右键修复**：修复了宇宙弦切拦截普通右键长按的问题，将蓄力/释放链路完全归还给 SA。
- **渲染性能优化**：移除了先前打包进 GUI 的大体积外扩背景贴图（`infinity_stellaris_cosmic_backdrop.png`），完全依靠实时代码进行魔法阵的手绘。
- **本地化与配置同步**：同步更新了简中、繁中、香港繁中和英文的本地化文案，重构了命名刀 JSON、JEI 联动说明 JSON 与 README，保证描述与当前版本统一。

### 2.7.1-1.20.1-forge 文档补充

**文档 (Documentation)**
- 新增项目根目录文档 `infinity_stellaris.md`，按当前源码、命名刀 JSON、Tooltip、JEI 与本地化资源整理无尽星空 (`annihilationblade:infinity_stellaris`) 的功能特色、基础属性、持有者权能、专属 SA / SE、视觉表现、配置项与源码索引。
- 本次仅补充说明文档，不改变代码、资源包内容、构建产物行为或版本号。

**修复 (Bug Fixes)**
- 修复湮灭之刃/无尽星空客户端全亮视界在部分 1.20.1 Forge 运行环境中反射误写 `OptionInstance` tooltip 字段，导致客户端 Tick 崩溃的问题。
- 保留原有伽马覆写全亮功能，将字段定位改为运行时探测真实 gamma value 字段，并在反射失败时执行安全降级，避免崩溃扩散。
- 修复血狱在背包刷新时整包覆盖命名刀 NBT，导致铁砧追加的附魔被强制还原的问题。现在血狱刷新基础刀体定义时会保留当前物品已有附魔。
- 清理血狱旧物品栈残留的 `minecraft:multishot`，并迁移为 `minecraft:power`，使源码、命名刀 JSON、运行时刷新结果保持一致。
- 将血狱的命名刀 NBT 覆盖限制为获取刀时的一次性生成。后续背包、主手、副手运行时刷新只保留身份识别兜底，不再复制命名刀定义或重写附魔。
- 避免玩家通过铁砧、附魔书或其它方式给血狱追加的新附魔被周期刷新覆盖。

### 2.7.2-1.20.1-forge 构建修复

**修复与配置固化 (Fixes & Config)**
- **修复 Gradle Worker Daemon 启动崩溃**：定位 `compileJava FAILED`（`ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`）的原因在于全局系统默认 JDK 切换为 Java 25，导致 Gradle 8.8 机制兼容失效。
- **显式配置 JDK 17**：在 `gradle.properties` 中固化配置 `org.gradle.java.home=C:/Program Files/Zulu/zulu-17`，确保 Gradle 构建及 Gradle Worker 强制使用 1.20.1 Forge 要求的 JDK 17 环境。
- **构建验证**：使用 PowerShell 完成 `compileJava` 与全量 `build` / `reobfJar` 打包验证，构建已恢复 `BUILD SUCCESSFUL`。
**新增 (Features)**
- 为最终兵器命名刀「无尽星空」(`annihilationblade:infinity_stellaris`) 新增 `slashblade:shaped_blade` 合成配方。
- 配方采用终焉级门槛：四枚湮灭核心、一枚龙蛋、两座信标、一枚下界之星，以及一把杀敌 `5000`、耀魂 `25000`、精炼 `50` 的魔刀·血狱作为中心刀胚。
- 同步更新简中、繁中、香港繁中、英文本地化与 JEI 说明，将旧的“不可合成”描述改为“终焉级仪式合成”。
- 修正无尽星空与血狱的 `slashblade:shaped_blade` JSON 格式：`result` 改为原版 `ShapedRecipe` 可解析的 `item` 字段，并将单个拔刀剑材料改为参考源码生成配方使用的 `item` 写法。

**调整与安全性 (Tuning & Safety)**
- 削弱无尽星空「曲率撕裂」的 AI 删除：新增可配置按键 `I` 切换，默认关闭，不再常驻自动删除周围生物 AI。
- 曲率撕裂目标筛选改为走 SlashBlade 原生 PVP / friendly fire 目标判定，默认不再影响玩家、宠物、友军与非敌对单位。
- 被曲率撕裂临时关闭 AI 的 Mob 会记录原始 NoAI 状态；当玩家关闭开关、停止手持无尽星空、退出或换维度时恢复，避免永久创伤。

**兼容性配置 (Compatibility Config)**
- 新增 `client_tooltips.enable_annihilation_blade_renderer` 与 `client_tooltips.enable_infinity_stellaris_renderer` 配置项，默认 `true`，用于分别控制湮灭之刃与无尽星空专属 tooltip 渲染器是否启用。
- 当对应配置关闭时，该刀不再取消原版 tooltip 渲染事件，背包、创造栏、JEI 或其它 tooltip 增强模组会回到更兼容的原版渲染路径。
- 检查血狱当前源码后确认其没有独立的专属 tooltip 重绘器，因此本次没有加入无实际效果的血狱开关。
