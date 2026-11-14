package qwq.qingyi.annihilationblade.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置（基于 ForgeConfigApiPort 的 ModConfigSpec 体系）。
 * <p>
 * 对外保留原有的 {@code COMMON}/{@code CLIENT} 层级结构及 {@code getValue()} 兼容读取方式，
 * 以便各模块平滑迁移，同时避免配置尚未加载完成时访问产生异常。
 */
public final class ModConfig {
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    // 通过静态方法引用延迟读取 COMMON_SPEC / CLIENT_SPEC，既避免前向引用编译错误又保证在 COMMON / CLIENT 构造前完成初始化
    private interface SpecSupplier {
        ModConfigSpec getSpec();
    }

    private static final SpecSupplier COMMON_SPEC_REF = ModConfig::getCommonSpec;
    private static final SpecSupplier CLIENT_SPEC_REF = ModConfig::getClientSpec;

    public static final Common COMMON = new Common(COMMON_BUILDER);
    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    public static final Client CLIENT = new Client(CLIENT_BUILDER);
    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    private static ModConfigSpec getCommonSpec() {
        return COMMON_SPEC;
    }

    private static ModConfigSpec getClientSpec() {
        return CLIENT_SPEC;
    }

    private ModConfig() {
    }

    public static final class Common {
        public final AnnihilationBlade annihilationBlade;
        public final BloodPrison bloodPrison;
        public final InfinityStellaris infinityStellaris;
        public final NightfallDragon nightfallDragon;
        public final LoliBlade loliBlade;
        public final SpatialFracture spatialFracture;
        public final Dankong dankong;
        public final WorldRift worldRift;
        public final TerminusEcho terminusEcho;
        public final VoidDominion voidDominion;
        public final CausalityCollapse causalityCollapse;
        public final StarlessJudgement starlessJudgement;
        public final PhantomJudgement phantomJudgement;
        public final AbyssalDecree abyssalDecree;
        public final Domain bloodPrisonDomain;
        public final PhantomBurst bloodPrisonPhantomBurst;

        private Common(ModConfigSpec.Builder builder) {
            this.annihilationBlade = new AnnihilationBlade(builder);
            this.bloodPrison = new BloodPrison(builder);
            this.infinityStellaris = new InfinityStellaris(builder);
            this.nightfallDragon = new NightfallDragon(builder);
            this.loliBlade = new LoliBlade(builder);
            this.spatialFracture = this.annihilationBlade.spatialFracture;
            this.dankong = this.annihilationBlade.dankong;
            this.worldRift = this.annihilationBlade.worldRift;
            this.terminusEcho = this.annihilationBlade.terminusEcho;
            this.voidDominion = this.annihilationBlade.voidDominion;
            this.causalityCollapse = this.annihilationBlade.causalityCollapse;
            this.starlessJudgement = this.annihilationBlade.starlessJudgement;
            this.phantomJudgement = this.annihilationBlade.phantomJudgement;
            this.abyssalDecree = this.annihilationBlade.abyssalDecree;
            this.bloodPrisonDomain = this.bloodPrison.domain;
            this.bloodPrisonPhantomBurst = this.bloodPrison.phantomBurst;
        }
    }

    public static final class Client {
        public final ClientTooltips clientTooltips;

        private Client(ModConfigSpec.Builder builder) {
            this.clientTooltips = new ClientTooltips(builder);
        }
    }

    public static final class ClientTooltips {
        public final BooleanEntry enableAnnihilationBladeRenderer;
        public final BooleanEntry enableInfinityStellarisRenderer;
        public final BooleanEntry enableNightfallDragonRenderer;

        private ClientTooltips(ModConfigSpec.Builder builder) {
            builder.comment("客户端专属 Tooltip 特效渲染开关").push("client_tooltips");
            this.enableAnnihilationBladeRenderer = boolValue(builder, CLIENT_SPEC_REF, "enable_annihilation_blade_renderer", true, "是否启用湮灭之刃自定义 Tooltip 渲染器");
            this.enableInfinityStellarisRenderer = boolValue(builder, CLIENT_SPEC_REF, "enable_infinity_stellaris_renderer", true, "是否启用无尽星空自定义 Tooltip 渲染器");
            this.enableNightfallDragonRenderer = boolValue(builder, CLIENT_SPEC_REF, "enable_nightfall_dragon_renderer", true, "是否启用夜临玄龙自定义 Tooltip 渲染器");
            builder.pop();
        }
    }

    public static final class AnnihilationBlade {
        public final SpatialFracture spatialFracture;
        public final Dankong dankong;
        public final WorldRift worldRift;
        public final TerminusEcho terminusEcho;
        public final VoidDominion voidDominion;
        public final CausalityCollapse causalityCollapse;
        public final StarlessJudgement starlessJudgement;
        public final PhantomJudgement phantomJudgement;
        public final AbyssalDecree abyssalDecree;

        private AnnihilationBlade(ModConfigSpec.Builder builder) {
            builder.comment("湮灭之刃相关配置").push("annihilation_blade");
            this.spatialFracture = new SpatialFracture(builder);
            this.dankong = new Dankong(builder);
            this.worldRift = new WorldRift(builder);
            this.terminusEcho = new TerminusEcho(builder);
            this.voidDominion = new VoidDominion(builder);
            this.causalityCollapse = new CausalityCollapse(builder);
            this.starlessJudgement = new StarlessJudgement(builder);
            this.phantomJudgement = new PhantomJudgement(builder);
            this.abyssalDecree = new AbyssalDecree(builder);
            builder.pop();
        }
    }

    public static final class LoliBlade {
        public final BooleanEntry attackEnabled;
        public final BooleanEntry defenseEnabled;
        public final BooleanEntry reflectDamage;
        public final BooleanEntry restoreHealth;
        public final BooleanEntry grantFlight;
        public final BooleanEntry voidProtection;
        public final BooleanEntry clearInventory;
        public final BooleanEntry kickPlayer;
        public final BooleanEntry forceDeath;
        public final BooleanEntry removeEntity;
        public final DoubleEntry attackRange;
        public final DoubleEntry slashArtRange;
        public final IntEntry maxTargets;
        public final IntEntry facingCooldownTicks;
        public final IntEntry areaCooldownTicks;
        /** 以不可拦截的物理抹除替换原版伤害链兜底（五路血量归零 + 非玩家硬移除）。 */
        public final BooleanEntry ultimateObliterate;
        /** 方法级不可杀：通过 Mixin 在字节码层拦截 hurt/die/actuallyHurt/kill/tickDeath。 */
        public final BooleanEntry ultimateInvincible;
        /** 服务端每 tick 校验，被任何手段移除后恢复持有者存活态。 */
        public final BooleanEntry ultimateTheseus;

        private LoliBlade(ModConfigSpec.Builder builder) {
            builder.comment("萝莉之刃相关配置").push("loli_blade");
            this.attackEnabled = boolValue(builder, COMMON_SPEC_REF, "attack_enabled", true, "是否启用萝莉之刃攻击效果");
            this.defenseEnabled = boolValue(builder, COMMON_SPEC_REF, "defense_enabled", true, "是否启用萝莉之刃防御机制");
            this.reflectDamage = boolValue(builder, COMMON_SPEC_REF, "reflect_damage", true, "受到攻击时是否反伤攻击者");
            this.restoreHealth = boolValue(builder, COMMON_SPEC_REF, "restore_health", true, "是否持续恢复满血");
            this.grantFlight = boolValue(builder, COMMON_SPEC_REF, "grant_flight", true, "持有时是否赋予创造飞行能力");
            this.voidProtection = boolValue(builder, COMMON_SPEC_REF, "void_protection", true, "掉入虚空时是否自动拉回并保持飞行");
            this.clearInventory = boolValue(builder, COMMON_SPEC_REF, "clear_inventory", false, "攻击目标玩家时是否清空其物品栏");
            this.kickPlayer = boolValue(builder, COMMON_SPEC_REF, "kick_player", false, "攻击目标玩家时是否断开其连接");
            this.forceDeath = boolValue(builder, COMMON_SPEC_REF, "force_death", true, "是否允许绕过常规保护的强制处决");
            this.removeEntity = boolValue(builder, COMMON_SPEC_REF, "remove_entity", false, "终极抹除时是否强制移除非玩家实体");
            this.attackRange = doubleValue(builder, COMMON_SPEC_REF, "attack_range", 48.0, 4.0, 128.0, "普通/面对斩击的判定距离");
            this.slashArtRange = doubleValue(builder, COMMON_SPEC_REF, "slash_art_range", 128.0, 16.0, 256.0, "大范围处决 SA 覆盖半径");
            this.maxTargets = intValue(builder, COMMON_SPEC_REF, "max_targets", 128, 1, 512, "单次最大处决目标数");
            this.facingCooldownTicks = intValue(builder, COMMON_SPEC_REF, "facing_cooldown_ticks", 5, 0, 200, "面对斩击冷却（tick）");
            this.areaCooldownTicks = intValue(builder, COMMON_SPEC_REF, "area_cooldown_ticks", 10, 0, 200, "大范围 SA 冷却（tick）");
            this.ultimateObliterate = boolValue(builder, COMMON_SPEC_REF, "ultimate_obliterate", false, "是否开启不可拦截的物理抹除兜底");
            this.ultimateInvincible = boolValue(builder, COMMON_SPEC_REF, "ultimate_invincible", false, "是否开启 Mixin 字节码层方法级无敌");
            this.ultimateTheseus = boolValue(builder, COMMON_SPEC_REF, "ultimate_theseus", false, "是否开启服务端每 tick 忒修斯存在守护");
            builder.pop();
        }
    }

    public static final class SpatialFracture {
        public final DoubleEntry maxDistance;
        public final DoubleEntry fractureRadius;
        public final DoubleEntry rayStep;
        public final DoubleEntry raySampleRadius;
        public final DoubleEntry entityLockRadius;
        public final DoubleEntry backupRadius;
        public final IntEntry maxTargets;
        public final IntEntry maxVisualizedTargets;
        public final IntEntry fractureSlashes;
        public final IntEntry centerSlashes;
        public final DoubleEntry visualScale;

        private SpatialFracture(ModConfigSpec.Builder builder) {
            builder.push("spatial_fracture");
            this.maxDistance = doubleValue(builder, COMMON_SPEC_REF, "max_distance", 160.0, 32.0, 256.0, "空间碎裂最大射线距离");
            this.fractureRadius = doubleValue(builder, COMMON_SPEC_REF, "fracture_radius", 20.0, 4.0, 64.0, "空间碎裂爆炸半径");
            this.rayStep = doubleValue(builder, COMMON_SPEC_REF, "ray_step", 4.0, 1.0, 12.0, "射线采样步长");
            this.raySampleRadius = doubleValue(builder, COMMON_SPEC_REF, "ray_sample_radius", 5.0, 1.0, 16.0, "射线沿途搜敌半径");
            this.entityLockRadius = doubleValue(builder, COMMON_SPEC_REF, "entity_lock_radius", 3.0, 0.5, 10.0, "直线锁敌半径");
            this.backupRadius = doubleValue(builder, COMMON_SPEC_REF, "backup_radius", 48.0, 8.0, 96.0, "后备索敌半径");
            this.maxTargets = intValue(builder, COMMON_SPEC_REF, "max_targets", 128, 1, 256, "最大命中目标数");
            this.maxVisualizedTargets = intValue(builder, COMMON_SPEC_REF, "max_visualized_targets", 32, 0, 128, "最大特效可视化目标数");
            this.fractureSlashes = intValue(builder, COMMON_SPEC_REF, "fracture_slashes", 24, 0, 96, "裂隙斩痕特效数");
            this.centerSlashes = intValue(builder, COMMON_SPEC_REF, "center_slashes", 12, 0, 48, "中心聚爆斩痕数");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class Dankong {
        public final DoubleEntry range;
        public final IntEntry maxTargets;
        public final IntEntry stepInterval;
        public final IntEntry cooldownTicks;
        public final DoubleEntry visualScale;

        private Dankong(ModConfigSpec.Builder builder) {
            builder.push("dankong");
            this.range = doubleValue(builder, COMMON_SPEC_REF, "range", 72.0, 8.0, 256.0, "瞬步距离上限");
            this.maxTargets = intValue(builder, COMMON_SPEC_REF, "max_targets", 48, 1, 128, "沿途处决最大目标数");
            this.stepInterval = intValue(builder, COMMON_SPEC_REF, "step_interval_ticks", 3, 1, 12, "斩击结算间隔（tick）");
            this.cooldownTicks = intValue(builder, COMMON_SPEC_REF, "cooldown_ticks", 12, 0, 100, "冷却时间（tick）");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class WorldRift {
        public final DoubleEntry radius;
        public final IntEntry maxTargets;
        public final IntEntry chainCount;
        public final DoubleEntry chainRange;
        public final DoubleEntry visualScale;

        private WorldRift(ModConfigSpec.Builder builder) {
            builder.push("world_rift");
            this.radius = doubleValue(builder, COMMON_SPEC_REF, "radius", 8.0, 2.0, 32.0, "世界裂隙半径");
            this.maxTargets = intValue(builder, COMMON_SPEC_REF, "max_targets", 24, 1, 96, "最大波及目标数");
            this.chainCount = intValue(builder, COMMON_SPEC_REF, "chain_count", 3, 1, 10, "连锁次数");
            this.chainRange = doubleValue(builder, COMMON_SPEC_REF, "chain_range", 256.0, 1.0, 1024.0, "连锁搜寻距离");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class TerminusEcho {
        public final DoubleEntry range;
        public final DoubleEntry width;
        public final IntEntry echoCount;
        public final IntEntry echoInterval;
        public final IntEntry cooldownTicks;
        public final IntEntry maxActiveSequences;
        public final IntEntry maxTargetsPerWave;
        public final DoubleEntry visualScale;

        private TerminusEcho(ModConfigSpec.Builder builder) {
            builder.push("terminus_echo");
            this.range = doubleValue(builder, COMMON_SPEC_REF, "range", 36.0, 8.0, 96.0, "终焉回响长度");
            this.width = doubleValue(builder, COMMON_SPEC_REF, "width", 4.4, 1.0, 16.0, "波束宽度");
            this.echoCount = intValue(builder, COMMON_SPEC_REF, "echo_count", 5, 1, 12, "回响波次");
            this.echoInterval = intValue(builder, COMMON_SPEC_REF, "echo_interval_ticks", 3, 1, 12, "波次间隔（tick）");
            this.cooldownTicks = intValue(builder, COMMON_SPEC_REF, "cooldown_ticks", 0, 0, 120, "冷却（tick）");
            this.maxActiveSequences = intValue(builder, COMMON_SPEC_REF, "max_active_sequences", 2, 1, 8, "同时活跃序列数上限");
            this.maxTargetsPerWave = intValue(builder, COMMON_SPEC_REF, "max_targets_per_wave", 32, 1, 128, "每波最大目标数");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class VoidDominion {
        public final DoubleEntry range;
        public final IntEntry maxTargets;
        public final IntEntry cooldownTicks;
        public final DoubleEntry visualScale;

        private VoidDominion(ModConfigSpec.Builder builder) {
            builder.push("void_dominion");
            this.range = doubleValue(builder, COMMON_SPEC_REF, "range", 26.0, 4.0, 64.0, "虚空支配范围半径");
            this.maxTargets = intValue(builder, COMMON_SPEC_REF, "max_targets", 64, 1, 160, "最大目标数");
            this.cooldownTicks = intValue(builder, COMMON_SPEC_REF, "cooldown_ticks", 70, 0, 200, "冷却（tick）");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class CausalityCollapse {
        public final DoubleEntry chainRadius;
        public final IntEntry maxChain;
        public final IntEntry cooldownTicks;
        public final DoubleEntry visualScale;

        private CausalityCollapse(ModConfigSpec.Builder builder) {
            builder.push("causality_collapse");
            this.chainRadius = doubleValue(builder, COMMON_SPEC_REF, "chain_radius", 14.0, 2.0, 48.0, "因果崩塌连锁半径");
            this.maxChain = intValue(builder, COMMON_SPEC_REF, "max_chain", 18, 1, 96, "最大连锁目标数");
            this.cooldownTicks = intValue(builder, COMMON_SPEC_REF, "cooldown_ticks", 10, 0, 100, "冷却（tick）");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class StarlessJudgement {
        public final DoubleEntry range;
        public final DoubleEntry width;
        public final IntEntry cooldownTicks;
        public final IntEntry maxTargets;
        public final DoubleEntry visualScale;

        private StarlessJudgement(ModConfigSpec.Builder builder) {
            builder.push("starless_judgement");
            this.range = doubleValue(builder, COMMON_SPEC_REF, "range", 56.0, 8.0, 128.0, "无星裁决射程");
            this.width = doubleValue(builder, COMMON_SPEC_REF, "width", 8.5, 1.0, 24.0, "裁决宽度");
            this.cooldownTicks = intValue(builder, COMMON_SPEC_REF, "cooldown_ticks", 34, 0, 160, "冷却（tick）");
            this.maxTargets = intValue(builder, COMMON_SPEC_REF, "max_targets", 80, 1, 200, "最大目标数");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class PhantomJudgement {
        public final DoubleEntry range;
        public final IntEntry searchTicks;
        public final IntEntry swordCount;
        public final IntEntry rainSwordsPerTarget;
        public final IntEntry fallingSwordDelayTicks;
        public final IntEntry lingerTicks;
        public final IntEntry maxTargets;
        public final IntEntry maxLingeringSwords;
        public final IntEntry cooldownTicks;
        public final DoubleEntry visualScale;

        private PhantomJudgement(ModConfigSpec.Builder builder) {
            builder.push("phantom_judgement");
            this.range = doubleValue(builder, COMMON_SPEC_REF, "range", 40.0, 8.0, 96.0, "幻影裁决检索范围");
            this.searchTicks = intValue(builder, COMMON_SPEC_REF, "search_ticks", 20, 4, 80, "锁敌检索持续 tick");
            this.swordCount = intValue(builder, COMMON_SPEC_REF, "search_sword_count", 8, 1, 24, "索敌幻影剑数量");
            this.rainSwordsPerTarget = intValue(builder, COMMON_SPEC_REF, "rain_swords_per_target", 6, 1, 16, "每个目标降下的剑雨数量");
            this.fallingSwordDelayTicks = intValue(builder, COMMON_SPEC_REF, "falling_sword_delay_ticks", 24, 0, 80, "剑雨落雷延迟 tick");
            this.lingerTicks = intValue(builder, COMMON_SPEC_REF, "linger_ticks", 60, 0, 200, "剑影滞留时长 tick");
            this.maxTargets = intValue(builder, COMMON_SPEC_REF, "max_targets", 24, 1, 96, "最大目标数");
            this.maxLingeringSwords = intValue(builder, COMMON_SPEC_REF, "max_lingering_swords", 96, 0, 256, "最大滞留幻影剑上限");
            this.cooldownTicks = intValue(builder, COMMON_SPEC_REF, "cooldown_ticks", 44, 0, 200, "冷却（tick）");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class AbyssalDecree {
        public final DoubleEntry range;
        public final IntEntry maxTargets;
        public final IntEntry strikeInterval;
        public final IntEntry cooldownTicks;
        public final DoubleEntry visualScale;

        private AbyssalDecree(ModConfigSpec.Builder builder) {
            builder.push("abyssal_decree");
            this.range = doubleValue(builder, COMMON_SPEC_REF, "range", 34.0, 6.0, 96.0, "深渊神谕半径");
            this.maxTargets = intValue(builder, COMMON_SPEC_REF, "max_targets", 16, 1, 80, "最大打击目标数");
            this.strikeInterval = intValue(builder, COMMON_SPEC_REF, "strike_interval_ticks", 3, 1, 12, "降雷间隔 tick");
            this.cooldownTicks = intValue(builder, COMMON_SPEC_REF, "cooldown_ticks", 82, 0, 240, "冷却（tick）");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class BloodPrison {
        public final Domain domain;
        public final PhantomBurst phantomBurst;

        private BloodPrison(ModConfigSpec.Builder builder) {
            builder.comment("血狱相关配置").push("blood_prison");
            this.domain = new Domain(builder);
            this.phantomBurst = new PhantomBurst(builder);
            builder.pop();
        }
    }

    public static final class Domain {
        public final IntEntry durationTicks;
        public final DoubleEntry radius;
        public final IntEntry borderIntervalTicks;
        public final IntEntry playerAuraIntervalTicks;
        public final IntEntry pulseIntervalTicks;
        public final DoubleEntry visualScale;

        private Domain(ModConfigSpec.Builder builder) {
            builder.push("domain");
            this.durationTicks = intValue(builder, COMMON_SPEC_REF, "duration_ticks", 400, 40, 1200, "血狱领域持续时间（tick）");
            this.radius = doubleValue(builder, COMMON_SPEC_REF, "radius", 10.0, 3.0, 32.0, "领域半径");
            this.borderIntervalTicks = intValue(builder, COMMON_SPEC_REF, "border_interval_ticks", 10, 2, 40, "边界粒子刷新间隔（tick）");
            this.playerAuraIntervalTicks = intValue(builder, COMMON_SPEC_REF, "player_aura_interval_ticks", 4, 1, 40, "玩家周身光环刷新间隔");
            this.pulseIntervalTicks = intValue(builder, COMMON_SPEC_REF, "pulse_interval_ticks", 20, 4, 80, "领域脉冲判定间隔");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class PhantomBurst {
        public final IntEntry swordCount;
        public final IntEntry swordDelayTicks;
        public final DoubleEntry burstRadiusScale;
        public final DoubleEntry visualScale;

        private PhantomBurst(ModConfigSpec.Builder builder) {
            builder.push("phantom_burst");
            this.swordCount = intValue(builder, COMMON_SPEC_REF, "sword_count", 10, 1, 32, "幻影爆裂生成的幻影剑数目");
            this.swordDelayTicks = intValue(builder, COMMON_SPEC_REF, "sword_delay_ticks", 18, 0, 80, "引爆延迟（tick）");
            this.burstRadiusScale = doubleValue(builder, COMMON_SPEC_REF, "burst_radius_scale", 2.2, 0.5, 5.0, "爆裂半径缩放倍率");
            this.visualScale = visualScale(builder);
            builder.pop();
        }
    }

    public static final class InfinityStellaris {
        public final DoubleEntry entropyPercent;
        public final IntEntry entropyMarks;
        public final IntEntry entropyBlacklistTicks;
        public final DoubleEntry curvatureRadius;
        public final IntEntry curvatureTickInterval;
        public final IntEntry curvatureMaxTargets;
        public final IntEntry curvatureBurstMarks;

        private InfinityStellaris(ModConfigSpec.Builder builder) {
            builder.comment("无尽星空相关配置").push("infinity_stellaris");
            this.entropyPercent = doubleValue(builder, COMMON_SPEC_REF, "entropy_percent", 0.10, 0.0, 1.0, "熵增瓦解最大生命值百分比真伤");
            this.entropyMarks = intValue(builder, COMMON_SPEC_REF, "entropy_marks", 10, 1, 100, "每层印记附加数");
            this.entropyBlacklistTicks = intValue(builder, COMMON_SPEC_REF, "entropy_blacklist_ticks", 40, 0, 400, "免疫复活冷却黑名单 tick");
            this.curvatureRadius = doubleValue(builder, COMMON_SPEC_REF, "curvature_radius", 32.0, 1.0, 128.0, "曲率断裂域半径");
            this.curvatureTickInterval = intValue(builder, COMMON_SPEC_REF, "curvature_tick_interval", 20, 1, 100, "曲率领域作用间隔 tick");
            this.curvatureMaxTargets = intValue(builder, COMMON_SPEC_REF, "curvature_max_targets", 64, 1, 256, "曲率领域最大目标数");
            this.curvatureBurstMarks = intValue(builder, COMMON_SPEC_REF, "curvature_burst_marks", 5, 1, 100, "引力引爆印记层数");
            builder.pop();
        }
    }

    public static final class NightfallDragon {
        public final DoubleEntry judgementCutRange;
        public final IntEntry judgementCutTotalCuts;
        public final IntEntry judgementCutIntervalTicks;
        public final DoubleEntry judgementCutDamage;
        public final DoubleEntry judgementCutScale;
        public final IntEntry scaleGuardSwordCount;
        public final DoubleEntry cosmicDescentMaxDistance;
        public final DoubleEntry cosmicDescentVortexRadius;
        public final DoubleEntry cosmicDescentExplosionRadius;
        public final IntEntry cosmicDescentMeteorWaves;
        public final IntEntry cosmicDescentSwordsPerWave;
        public final DoubleEntry cosmicDescentMeteorDamage;
        public final DoubleEntry cosmicDescentCollapsePanelMultiplier;
        public final DoubleEntry demonicBloodExtraDamagePercent;
        public final IntEntry demonicBloodPhantomBurstMarks;
        public final DoubleEntry absoluteDomainRadius;
        public final IntEntry absoluteDomainMaxTargets;
        public final IntEntry bladeStormSwords;
        public final DoubleEntry worldCleavingRange;

        private NightfallDragon(ModConfigSpec.Builder builder) {
            builder.comment("夜临玄龙相关配置").push("nightfall_dragon");
            this.judgementCutRange = doubleValue(builder, COMMON_SPEC_REF, "judgement_cut_range", 50.0, 10.0, 200.0, "封印形态次元斩索敌范围");
            this.judgementCutTotalCuts = intValue(builder, COMMON_SPEC_REF, "judgement_cut_total_cuts", 100, 10, 500, "总斩击数");
            this.judgementCutIntervalTicks = intValue(builder, COMMON_SPEC_REF, "judgement_cut_interval_ticks", 2, 1, 20, "每次斩击间隔 tick");
            this.judgementCutDamage = doubleValue(builder, COMMON_SPEC_REF, "judgement_cut_damage", 91.0, 1.0, 1000.0, "单次斩击伤害");
            this.judgementCutScale = doubleValue(builder, COMMON_SPEC_REF, "judgement_cut_scale", 8.0, 1.0, 32.0, "次元斩几何缩放倍率");
            this.scaleGuardSwordCount = intValue(builder, COMMON_SPEC_REF, "scale_guard_sword_count", 16, 4, 64, "护身逆鳞幻影剑数量");
            this.cosmicDescentMaxDistance = doubleValue(builder, COMMON_SPEC_REF, "cosmic_descent_max_distance", 120.0, 20.0, 500.0, "永夜神降最大释放距离");
            this.cosmicDescentVortexRadius = doubleValue(builder, COMMON_SPEC_REF, "cosmic_descent_vortex_radius", 72.0, 10.0, 256.0, "引力漩涡吸引半径");
            this.cosmicDescentExplosionRadius = doubleValue(builder, COMMON_SPEC_REF, "cosmic_descent_explosion_radius", 72.0, 10.0, 256.0, "终焉坍缩爆破半径");
            this.cosmicDescentMeteorWaves = intValue(builder, COMMON_SPEC_REF, "cosmic_descent_meteor_waves", 20, 3, 50, "星陨轰炸波数");
            this.cosmicDescentSwordsPerWave = intValue(builder, COMMON_SPEC_REF, "cosmic_descent_swords_per_wave", 5, 1, 20, "每波陨剑数量");
            this.cosmicDescentMeteorDamage = doubleValue(builder, COMMON_SPEC_REF, "cosmic_descent_meteor_damage", 78.0, 1.0, 1000.0, "单颗星陨剑伤害");
            this.cosmicDescentCollapsePanelMultiplier = doubleValue(builder, COMMON_SPEC_REF, "cosmic_descent_collapse_panel_multiplier", 6.0, 1.0, 50.0, "坍缩核心面板倍率");
            this.demonicBloodExtraDamagePercent = doubleValue(builder, COMMON_SPEC_REF, "demonic_blood_extra_damage_percent", 0.05, 0.0, 1.0, "魔血寄生额外目标最大生命百分比伤害");
            this.demonicBloodPhantomBurstMarks = intValue(builder, COMMON_SPEC_REF, "demonic_blood_phantom_burst_marks", 20, 1, 100, "魔血寄生附加印记数");
            this.absoluteDomainRadius = doubleValue(builder, COMMON_SPEC_REF, "absolute_domain_radius", 64.0, 8.0, 256.0, "绝对湮灭领域半径");
            this.absoluteDomainMaxTargets = intValue(builder, COMMON_SPEC_REF, "absolute_domain_max_targets", 12, 1, 64, "绝对湮灭领域每次脉冲最大目标数");
            this.bladeStormSwords = intValue(builder, COMMON_SPEC_REF, "blade_storm_swords", 16, 10, 300, "万龙剑阵幻影剑生成数");
            this.worldCleavingRange = doubleValue(builder, COMMON_SPEC_REF, "world_cleaving_range", 72.0, 10.0, 300.0, "劈界斩击最大长度");
            builder.pop();
        }
    }

    private static DoubleEntry visualScale(ModConfigSpec.Builder builder) {
        return doubleValue(builder, COMMON_SPEC_REF, "visual_scale", 1.0, 0.25, 2.0, "视觉特效缩放倍率");
    }

    private static IntEntry intValue(ModConfigSpec.Builder builder, SpecSupplier specRef, String name, int defaultValue, int min, int max, String comment) {
        ModConfigSpec.IntValue value = builder.comment(comment).defineInRange(name, defaultValue, min, max);
        return new IntEntry(specRef, value, defaultValue);
    }

    private static DoubleEntry doubleValue(ModConfigSpec.Builder builder, SpecSupplier specRef, String name, double defaultValue, double min, double max, String comment) {
        ModConfigSpec.DoubleValue value = builder.comment(comment).defineInRange(name, defaultValue, min, max);
        return new DoubleEntry(specRef, value, defaultValue);
    }

    private static BooleanEntry boolValue(ModConfigSpec.Builder builder, SpecSupplier specRef, String name, boolean defaultValue, String comment) {
        ModConfigSpec.BooleanValue value = builder.comment(comment).define(name, defaultValue);
        return new BooleanEntry(specRef, value, defaultValue);
    }

    private static boolean isSpecLoaded(SpecSupplier specRef) {
        if (specRef == null) {
            return false;
        }
        ModConfigSpec spec = specRef.getSpec();
        return spec != null && spec.isLoaded();
    }

    /** 布尔配置项兼容封装，支持 getValue() 与未加载前安全降级。 */
    public static final class BooleanEntry {
        private final SpecSupplier specRef;
        private final ModConfigSpec.BooleanValue backing;
        private final boolean defaultValue;

        private BooleanEntry(SpecSupplier specRef, ModConfigSpec.BooleanValue backing, boolean defaultValue) {
            this.specRef = specRef;
            this.backing = backing;
            this.defaultValue = defaultValue;
        }

        public boolean getValue() {
            if (isSpecLoaded(specRef)) {
                return Boolean.TRUE.equals(backing.get());
            }
            return defaultValue;
        }

        public boolean get() {
            return getValue();
        }
    }

    /** 整型配置项兼容封装。 */
    public static final class IntEntry {
        private final SpecSupplier specRef;
        private final ModConfigSpec.IntValue backing;
        private final int defaultValue;

        private IntEntry(SpecSupplier specRef, ModConfigSpec.IntValue backing, int defaultValue) {
            this.specRef = specRef;
            this.backing = backing;
            this.defaultValue = defaultValue;
        }

        public int getValue() {
            if (isSpecLoaded(specRef)) {
                return backing.get();
            }
            return defaultValue;
        }

        public int get() {
            return getValue();
        }
    }

    /** 浮点配置项兼容封装。 */
    public static final class DoubleEntry {
        private final SpecSupplier specRef;
        private final ModConfigSpec.DoubleValue backing;
        private final double defaultValue;

        private DoubleEntry(SpecSupplier specRef, ModConfigSpec.DoubleValue backing, double defaultValue) {
            this.specRef = specRef;
            this.backing = backing;
            this.defaultValue = defaultValue;
        }

        public double getValue() {
            if (isSpecLoaded(specRef)) {
                return backing.get();
            }
            return defaultValue;
        }

        public double get() {
            return getValue();
        }
    }
}
