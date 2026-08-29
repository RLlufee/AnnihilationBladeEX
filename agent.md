# 拔刀剑：终焉藏刀阁 (SlashBlade: Finale Blade Sanctum) AI Agent 开发指南

为了让 AI Agent 更好地协助开发本模组（基于 Forge 1.20.1 的 SlashBlade: Resharped 扩展），请在生成代码和修改项目时严格遵守以下核心准则：

## 1. 代码参考与复用原则
- **参考现有成品**：在实现新的命名刀、SA或SE时，必须首先查阅项目中已有的成熟实现（如 `annihilation_blade`、`blood_prison`、`infinity_stellaris`）。
- **学习现有逻辑**：仔细学习现有代码中事件监听、伤害判定、SlashBlade 原生友伤/PVP 判定接入以及特效渲染的方式。
- **保持风格一致**：遵循项目现有的代码风格，保留合理的注释，确保代码的可读性和逻辑的严密性。

## 2. 目录结构与包管理规范
- **包路径规范**：所有 Java 源码必须位于 `src/main/java/QWQ/QingYi/annihilationblade/` 及其子包下。
- **模块化结构**：制作新的刀或大型功能时，应当建立独立的子包（如 `annihilation_blade`、`infinity_stellaris` 等），保证目录结构清晰、条理整齐，**不可乱放**。
- **统一注册管理**：
  - SA (Slash Arts) 注册集中在 `slash_art/ModSlashArts.java`
  - SE (Special Effects) 注册集中在 `specialeffect/...... .java`
  - 等等......类似的需要不同功能的部分代码 **不要吝啬于创建子文件夹**！
- **资源文件目录**：
  - 命名刀定义 (JSON): `src/main/resources/data/annihilationblade/slashblade/named_blades/`
  - 本地化语言文件 (JSON): `src/main/resources/assets/annihilationblade/lang/`
  - 模型与贴图 (OBJ & PNG): `src/main/resources/assets/annihilationblade/model/`

## 3. 拔刀剑机制与模型规范
- **命名刀配置**：通过 Datapack JSON 定义刀的基础属性、SA、SE 数量及被动技能。
- **OBJ 模型分组限制**：如果需要修改或生成新的拔刀剑模型，必须严格遵守重锋（SlashBlade: Resharped）的分组命名规范，包括 `blade`, `sheath`, `blade_damaged`, `blade_fragment`, `item_blade`, `effect` 等，切勿随意更改分组名导致渲染错误。
- **客户端与服务端分离**：处理复杂的视觉渲染（如专属 Tooltip、炫酷的魔法阵或粒子闪电）时，必须严格区分客户端和逻辑端，确保代码不在专用服务端引发崩溃。

## 4. 本地化与配置说明
- **拒绝硬编码**：所有的物品名称、SA/SE 描述、动作栏提示信息都必须写入 `lang` 目录下的中英文字典文件中，并通过本地化键值调用，方便多语言适配和修改。（特别说明的是此项包括配置项文件，也尽量使用本地化键值调用）

- **配置文件原则**：`config/ModConfig.java` (Forge Common Config) 中仅开放低风险参数（如：索敌范围、冷却时间、粒子数量、客户端渲染开关）。**绝对不要**将关键的安全保护逻辑暴露给配置，以防破坏平衡。

## 5. 构建与环境要求
- **Java 版本**：Forge 1.20.1 要求使用 Java 17。
- **字符编码**：必须在 UTF-8 环境下运行和编译代码，注意配置环境变量 `$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8'` 以防中文乱码。

## 6. 调试、反射与日志
- **规范日志**：在必要时进行编写日志输出，**方便追踪与排错**。

## 7.风险、调试
- **风险控制**：在对于代码对想法的实现时，请勿过多因为风险而选择保守的实现，要勇于尝试，大胆创新！
- **调试**：本机的所有功能均可以被调用，若有必要，可以进行修改系统变量等全局操作，来更方便地进行调试。

## 8. 代码库参考及模仿
- **参考**：本工作区内（`S:\google_IDE\IDE_workhome\AnnihilationBlade-SE-main`）拥有**大量完成度很高的模组**，这些模组有的渲染功能十分强大，有的视觉效果安排惊艳而合理，有的功能性十分强大，在遇到问题的时候，*大多数情况可以在这些模组的代码库中找到答案*。（如果找不到，为了解决当前麻烦，可以**无限制向我提出问题，并对我做出参考项目要求！**）
- **模仿**：模仿的时候请莫要死板的进行搬运，可以创新性的找出其中的问题进行改进，一切均以**完成度高的项目**为目标！
