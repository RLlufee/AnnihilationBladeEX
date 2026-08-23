package QWQ.QingYi.annihilationblade.config;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import com.iafenvoy.jupiter.config.container.AutoInitConfigContainer;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.config.entry.DoubleEntry;
import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ModConfig {
   public static final Common COMMON = new Common();
   public static final Client CLIENT = new Client();

   private ModConfig() {
   }

   public static final class Common extends AutoInitConfigContainer {
      public final AnnihilationBlade annihilationBlade;
      public final BloodPrison bloodPrison;
      public final InfinityStellaris infinityStellaris;
      public final NightfallDragon nightfallDragon;
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

      private Common() {
         super(ResourceLocation.fromNamespaceAndPath(Annihilationblade.MODID, "common"), "config.annihilationblade.common.title", "./config/annihilationblade/common.json");
         this.annihilationBlade = new AnnihilationBlade();
         this.bloodPrison = new BloodPrison();
         this.infinityStellaris = new InfinityStellaris();
         this.nightfallDragon = new NightfallDragon();
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

   public static final class Client extends AutoInitConfigContainer {
      public final ClientTooltips clientTooltips = new ClientTooltips();

      private Client() {
         super(ResourceLocation.fromNamespaceAndPath(Annihilationblade.MODID, "client"), "config.annihilationblade.client.title", "./config/annihilationblade/client.json");
      }

   }

   public static final class ClientTooltips extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final BooleanEntry enableAnnihilationBladeRenderer = boolValue("enable_annihilation_blade_renderer", true, "config.annihilationblade.client_tooltips.enable_annihilation_blade_renderer.tooltip");
      public final BooleanEntry enableInfinityStellarisRenderer = boolValue("enable_infinity_stellaris_renderer", true, "config.annihilationblade.client_tooltips.enable_infinity_stellaris_renderer.tooltip");

      private ClientTooltips() {
         super("client_tooltips", "config.annihilationblade.client_tooltips.title");
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

      private AnnihilationBlade() {
         this.spatialFracture = new SpatialFracture();
         this.dankong = new Dankong();
         this.worldRift = new WorldRift();
         this.terminusEcho = new TerminusEcho();
         this.voidDominion = new VoidDominion();
         this.causalityCollapse = new CausalityCollapse();
         this.starlessJudgement = new StarlessJudgement();
         this.phantomJudgement = new PhantomJudgement();
         this.abyssalDecree = new AbyssalDecree();
      }
   }

   public static final class SpatialFracture extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry maxDistance;
      public final DoubleEntry fractureRadius;
      public final DoubleEntry rayStep;
      public final DoubleEntry raySampleRadius;
      public final DoubleEntry entityLockRadius;
      public final DoubleEntry backupRadius;
      public final IntegerEntry maxTargets;
      public final IntegerEntry maxVisualizedTargets;
      public final IntegerEntry fractureSlashes;
      public final IntegerEntry centerSlashes;
      public final DoubleEntry visualScale;

      private SpatialFracture() {
         super("annihilation_blade.spatial_fracture", "config.annihilationblade.annihilation_blade.spatial_fracture.title");
         this.maxDistance = doubleValue("max_distance", 160.0, 32.0, 256.0, "最大施法距离，建议 64-192。        还有，我他妈也忘记了这哪个SE对应啥技能效果了，我记忆力不好我是废物qwq", "Maximum cast distance. Suggested: 64-192.");
         this.fractureRadius = doubleValue("fracture_radius", 20.0, 4.0, 64.0, "空间破碎主判定半径，建议 12-32。", "Main Spatial Fracture target radius. Suggested: 12-32.");
         this.rayStep = doubleValue("ray_step", 4.0, 1.0, 12.0, "沿视线补充扫描步长，越小越密，建议 2-6。", "Backup ray scan step; lower means denser scan. Suggested: 2-6.");
         this.raySampleRadius = doubleValue("ray_sample_radius", 5.0, 1.0, 16.0, "沿视线每个采样点的扫描半径，建议 3-8。", "Backup ray sample radius. Suggested: 3-8.");
         this.entityLockRadius = doubleValue("entity_lock_radius", 3.0, 0.5, 10.0, "准星锁定实体的宽容半径，建议 2-5。", "Aim lock tolerance around entities. Suggested: 2-5.");
         this.backupRadius = doubleValue("backup_radius", 48.0, 8.0, 96.0, "没有命中目标时的备用搜索半径，建议 24-64。", "Fallback search radius when no target is found. Suggested: 24-64.");
         this.maxTargets = intValue("max_targets", 128, 1, 256, "最大影响目标数，建议 32-160。", "Maximum affected targets. Suggested: 32-160.");
         this.maxVisualizedTargets = intValue("max_visualized_targets", 32, 0, 128, "最多播放单体斩击视觉的目标数，建议 16-48。", "Targets with individual slash visuals. Suggested: 16-48.");
         this.fractureSlashes = intValue("fracture_slashes", 24, 0, 96, "裂隙风暴斩击线数量，建议 12-36。", "Fracture storm visual slash count. Suggested: 12-36.");
         this.centerSlashes = intValue("center_slashes", 12, 0, 48, "中心 SlashBlade 斩击数量，建议 6-18。", "Center SlashBlade slash count. Suggested: 6-18.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class Dankong extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry range;
      public final IntegerEntry maxTargets;
      public final IntegerEntry stepInterval;
      public final IntegerEntry cooldownTicks;
      public final DoubleEntry visualScale;

      private Dankong() {
         super("annihilation_blade.dankong", "config.annihilationblade.annihilation_blade.dankong.title");
         this.range = doubleValue("range", 72.0, 8.0, 256.0, "断空搜索范围，建议 32-96。", "Severed Space target search range. Suggested: 32-96.");
         this.maxTargets = intValue("max_targets", 48, 1, 128, "连续闪现最多目标数，建议 16-64。", "Maximum blink targets. Suggested: 16-64.");
         this.stepInterval = intValue("step_interval_ticks", 3, 1, 12, "连续闪现间隔 tick，20 tick=1秒，建议 3-6。", "Ticks between blink steps; 20 ticks = 1 second. Suggested: 3-6.");
         this.cooldownTicks = intValue("cooldown_ticks", 12, 0, 100, "触发冷却 tick，建议 8-24。", "Trigger cooldown in ticks. Suggested: 8-24.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class WorldRift extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry radius;
      public final IntegerEntry maxTargets;
      public final IntegerEntry chainCount;
      public final DoubleEntry chainRange;
      public final DoubleEntry visualScale;

      private WorldRift() {
         super("annihilation_blade.world_rift", "config.annihilationblade.annihilation_blade.world_rift.title");
         this.radius = doubleValue("radius", 8.0, 2.0, 32.0, "裂界扩散半径，建议 5-14。", "World Rift spread radius. Suggested: 5-14.");
         this.maxTargets = intValue("max_targets", 24, 1, 96, "裂界最多影响目标数，建议 12-36。", "Maximum World Rift targets. Suggested: 12-36.");
         this.chainCount = intValue("chain_count", 3, 1, 10, "裂界处决最多连锁次数，建议 3-5。", "Maximum World Rift execution chain depth. Suggested: 3-5.");
         this.chainRange = doubleValue("chain_range", 256.0, 1.0, 1024.0, "裂界连锁距离上限，以最初攻击者为中心，建议 128-256。", "World Rift chain range cap from the original attacker. Suggested: 128-256.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class TerminusEcho extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry range;
      public final DoubleEntry width;
      public final IntegerEntry echoCount;
      public final IntegerEntry echoInterval;
      public final IntegerEntry cooldownTicks;
      public final IntegerEntry maxActiveSequences;
      public final IntegerEntry maxTargetsPerWave;
      public final DoubleEntry visualScale;

      private TerminusEcho() {
         super("annihilation_blade.terminus_echo", "config.annihilationblade.annihilation_blade.terminus_echo.title");
         this.range = doubleValue("range", 36.0, 8.0, 96.0, "归墟回响基础射程，建议 24-48。", "Base Terminus Echo range. Suggested: 24-48.");
         this.width = doubleValue("width", 4.4, 1.0, 16.0, "归墟回响基础宽度，建议 3-7。", "Base Terminus Echo width. Suggested: 3-7.");
         this.echoCount = intValue("echo_count", 5, 1, 12, "回响波次数量，建议 3-6。", "Echo wave count. Suggested: 3-6.");
         this.echoInterval = intValue("echo_interval_ticks", 3, 1, 12, "回响波次间隔 tick，建议 2-5。", "Ticks between echo waves. Suggested: 2-5.");
         this.cooldownTicks = intValue("cooldown_ticks", 16, 0, 120, "触发冷却 tick，建议 12-30。", "Trigger cooldown in ticks. Suggested: 12-30.");
         this.maxActiveSequences = intValue("max_active_sequences", 2, 1, 8, "同一玩家最多并存回响序列，建议 1-3。", "Maximum simultaneous echo sequences per player. Suggested: 1-3.");
         this.maxTargetsPerWave = intValue("max_targets_per_wave", 32, 1, 128, "每波最多影响目标数，建议 16-48。", "Maximum targets per echo wave. Suggested: 16-48.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class VoidDominion extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry range;
      public final IntegerEntry maxTargets;
      public final IntegerEntry cooldownTicks;
      public final DoubleEntry visualScale;

      private VoidDominion() {
         super("annihilation_blade.void_dominion", "config.annihilationblade.annihilation_blade.void_dominion.title");
         this.range = doubleValue("range", 26.0, 4.0, 64.0, "虚无权域半径，建议 16-36。", "Void Dominion radius. Suggested: 16-36.");
         this.maxTargets = intValue("max_targets", 64, 1, 160, "虚无权域最多影响目标数，建议 32-96。", "Maximum Void Dominion targets. Suggested: 32-96.");
         this.cooldownTicks = intValue("cooldown_ticks", 70, 0, 200, "触发冷却 tick，建议 50-100。", "Trigger cooldown in ticks. Suggested: 50-100.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class CausalityCollapse extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry chainRadius;
      public final IntegerEntry maxChain;
      public final IntegerEntry cooldownTicks;
      public final DoubleEntry visualScale;

      private CausalityCollapse() {
         super("annihilation_blade.causality_collapse", "config.annihilationblade.annihilation_blade.causality_collapse.title");
         this.chainRadius = doubleValue("chain_radius", 14.0, 2.0, 48.0, "因果链搜索半径，建议 8-20。", "Causality chain search radius. Suggested: 8-20.");
         this.maxChain = intValue("max_chain", 18, 1, 96, "因果链最大目标数，建议 8-30。", "Maximum causality chain targets. Suggested: 8-30.");
         this.cooldownTicks = intValue("cooldown_ticks", 10, 0, 100, "触发冷却 tick，建议 8-20。", "Trigger cooldown in ticks. Suggested: 8-20.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class StarlessJudgement extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry range;
      public final DoubleEntry width;
      public final IntegerEntry cooldownTicks;
      public final IntegerEntry maxTargets;
      public final DoubleEntry visualScale;

      private StarlessJudgement() {
         super("annihilation_blade.starless_judgement", "config.annihilationblade.annihilation_blade.starless_judgement.title");
         this.range = doubleValue("range", 56.0, 8.0, 128.0, "星寂裁决射程，建议 36-72。", "Starless Judgement beam range. Suggested: 36-72.");
         this.width = doubleValue("width", 8.5, 1.0, 24.0, "星寂裁决宽度，建议 5-12。", "Starless Judgement beam width. Suggested: 5-12.");
         this.cooldownTicks = intValue("cooldown_ticks", 34, 0, 160, "触发冷却 tick，建议 24-60。", "Trigger cooldown in ticks. Suggested: 24-60.");
         this.maxTargets = intValue("max_targets", 80, 1, 200, "星寂裁决最多影响目标数，建议 40-120。", "Maximum Starless Judgement targets. Suggested: 40-120.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class PhantomJudgement extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry range;
      public final IntegerEntry searchTicks;
      public final IntegerEntry swordCount;
      public final IntegerEntry rainSwordsPerTarget;
      public final IntegerEntry fallingSwordDelayTicks;
      public final IntegerEntry lingerTicks;
      public final IntegerEntry maxTargets;
      public final IntegerEntry maxLingeringSwords;
      public final IntegerEntry cooldownTicks;
      public final DoubleEntry visualScale;

      private PhantomJudgement() {
         super("annihilation_blade.phantom_judgement", "config.annihilationblade.annihilation_blade.phantom_judgement.title");
         this.range = doubleValue("range", 40.0, 8.0, 96.0, "幻影审判搜索范围，建议 24-56。", "Phantom Judgement search range. Suggested: 24-56.");
         this.searchTicks = intValue("search_ticks", 20, 4, 80, "锁定阶段持续 tick，建议 12-30。", "Target lock phase duration in ticks. Suggested: 12-30.");
         this.swordCount = intValue("search_sword_count", 8, 1, 24, "锁定阶段环绕剑数量，建议 6-12。", "Orbiting search sword count. Suggested: 6-12.");
         this.rainSwordsPerTarget = intValue("rain_swords_per_target", 6, 1, 16, "每个目标落剑数量，建议 4-8。", "Falling swords per target. Suggested: 4-8.");
         this.fallingSwordDelayTicks = intValue("falling_sword_delay_ticks", 24, 0, 80, "落剑命中延迟 tick，建议 18-30。", "Falling sword impact delay in ticks. Suggested: 18-30.");
         this.lingerTicks = intValue("linger_ticks", 60, 0, 200, "落剑残留 tick，建议 30-80。", "Lingering sword duration in ticks. Suggested: 30-80.");
         this.maxTargets = intValue("max_targets", 24, 1, 96, "幻影审判最多目标数，建议 12-36。", "Maximum Phantom Judgement targets. Suggested: 12-36.");
         this.maxLingeringSwords = intValue("max_lingering_swords", 96, 0, 256, "同一玩家最多残留剑数量，建议 48-128。", "Maximum lingering swords per player. Suggested: 48-128.");
         this.cooldownTicks = intValue("cooldown_ticks", 44, 0, 200, "触发冷却 tick，建议 34-80。", "Trigger cooldown in ticks. Suggested: 34-80.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class AbyssalDecree extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry range;
      public final IntegerEntry maxTargets;
      public final IntegerEntry strikeInterval;
      public final IntegerEntry cooldownTicks;
      public final DoubleEntry visualScale;

      private AbyssalDecree() {
         super("annihilation_blade.abyssal_decree", "config.annihilationblade.annihilation_blade.abyssal_decree.title");
         this.range = doubleValue("range", 34.0, 6.0, 96.0, "归墟天诏搜索范围，建议 20-48。", "Abyssal Decree search range. Suggested: 20-48.");
         this.maxTargets = intValue("max_targets", 16, 1, 80, "归墟天诏最多目标数，建议 8-24。", "Maximum Abyssal Decree targets. Suggested: 8-24.");
         this.strikeInterval = intValue("strike_interval_ticks", 3, 1, 12, "逐个裁决间隔 tick，建议 2-5。", "Ticks between decree strikes. Suggested: 2-5.");
         this.cooldownTicks = intValue("cooldown_ticks", 82, 0, 240, "触发冷却 tick，建议 70-120。", "Trigger cooldown in ticks. Suggested: 70-120.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class BloodPrison {
      public final Domain domain;
      public final PhantomBurst phantomBurst;

      private BloodPrison() {
         this.domain = new Domain();
         this.phantomBurst = new PhantomBurst();
      }
   }

   public static final class Domain extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final IntegerEntry durationTicks;
      public final DoubleEntry radius;
      public final IntegerEntry borderIntervalTicks;
      public final IntegerEntry playerAuraIntervalTicks;
      public final IntegerEntry pulseIntervalTicks;
      public final DoubleEntry visualScale;

      private Domain() {
         super("blood_prison.domain", "config.annihilationblade.blood_prison.domain.title");
         this.durationTicks = intValue("duration_ticks", 400, 40, 1200, "血狱领域持续 tick，20 tick=1秒，建议 200-600。", "Blood Prison domain duration in ticks. Suggested: 200-600.");
         this.radius = doubleValue("radius", 10.0, 3.0, 32.0, "血狱领域半径，建议 8-14。", "Blood Prison domain radius. Suggested: 8-14.");
         this.borderIntervalTicks = intValue("border_interval_ticks", 10, 2, 40, "领域边界粒子刷新间隔，建议 8-16。", "Domain border particle interval. Suggested: 8-16.");
         this.playerAuraIntervalTicks = intValue("player_aura_interval_ticks", 4, 1, 40, "领域内玩家血气粒子间隔，建议 4-10。", "Player aura particle interval inside domain. Suggested: 4-10.");
         this.pulseIntervalTicks = intValue("pulse_interval_ticks", 20, 4, 80, "领域脉冲视觉间隔，建议 16-30。", "Domain pulse visual interval. Suggested: 16-30.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class PhantomBurst extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final IntegerEntry swordCount;
      public final IntegerEntry swordDelayTicks;
      public final DoubleEntry burstRadiusScale;
      public final DoubleEntry visualScale;

      private PhantomBurst() {
         super("blood_prison.phantom_burst", "config.annihilationblade.blood_prison.phantom_burst.title");
         this.swordCount = intValue("sword_count", 10, 1, 32, "血狱幻影爆发剑数量，建议 6-14。", "Blood Prison phantom burst sword count. Suggested: 6-14.");
         this.swordDelayTicks = intValue("sword_delay_ticks", 18, 0, 80, "幻影剑命中延迟 tick，建议 12-24。", "Phantom sword impact delay in ticks. Suggested: 12-24.");
         this.burstRadiusScale = doubleValue("burst_radius_scale", 2.2, 0.5, 5.0, "爆发视觉半径与目标宽度倍率，建议 1.5-2.8。", "Burst visual radius scale against target width. Suggested: 1.5-2.8.");
         this.visualScale = visualScale(1.0);
      }
   }

   public static final class InfinityStellaris extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry entropyPercent;
      public final IntegerEntry entropyMarks;
      public final IntegerEntry entropyBlacklistTicks;
      public final DoubleEntry curvatureRadius;
      public final IntegerEntry curvatureTickInterval;
      public final IntegerEntry curvatureMaxTargets;
      public final IntegerEntry curvatureBurstMarks;

      private InfinityStellaris() {
         super("infinity_stellaris", "config.annihilationblade.infinity_stellaris.title");
         this.entropyPercent = doubleValue("entropy_percent", 0.10, 0.0, 1.0, "熵增蚀解每次追加的最大生命百分比。", "Max-health percentage added by Entropy Dissolution.");
         this.entropyMarks = intValue("entropy_marks", 10, 1, 100, "触发热寂归零所需累计次数。", "Marks required before heat-death zeroing.");
         this.entropyBlacklistTicks = intValue("entropy_blacklist_ticks", 40, 0, 400, "非玩家目标最终阶段后的短期黑名单 tick。", "Temporary blacklist ticks after final non-player execution.");
         this.curvatureRadius = doubleValue("curvature_radius", 32.0, 1.0, 128.0, "曲率撕裂冻结半径。", "Curvature rupture freeze radius.");
         this.curvatureTickInterval = intValue("curvature_tick_interval", 20, 1, 100, "曲率压缩伤害间隔 tick。", "Curvature strain mark interval in ticks.");
         this.curvatureMaxTargets = intValue("curvature_max_targets", 64, 1, 256, "曲率撕裂每次最多压制目标数。", "Maximum targets suppressed by Curvature Rupture.");
         this.curvatureBurstMarks = intValue("curvature_burst_marks", 5, 1, 100, "触发曲率爆裂所需层数。", "Marks required to trigger curvature burst.");
      }
   }

   public static final class NightfallDragon extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry judgementCutRange;
      public final IntegerEntry judgementCutTotalCuts;
      public final IntegerEntry judgementCutIntervalTicks;
      public final DoubleEntry judgementCutDamage;
      public final DoubleEntry judgementCutScale;

      public final IntegerEntry scaleGuardSwordCount;

      public final DoubleEntry cosmicDescentMaxDistance;
      public final DoubleEntry cosmicDescentVortexRadius;
      public final DoubleEntry cosmicDescentExplosionRadius;
      public final IntegerEntry cosmicDescentMeteorWaves;
      public final IntegerEntry cosmicDescentSwordsPerWave;
      public final DoubleEntry cosmicDescentMeteorDamage;
      public final DoubleEntry cosmicDescentCollapsePanelMultiplier;

      public final DoubleEntry demonicBloodExtraDamagePercent;
      public final IntegerEntry demonicBloodPhantomBurstMarks;

      public final DoubleEntry absoluteDomainRadius;
      public final IntegerEntry absoluteDomainMaxTargets;
      public final IntegerEntry bladeStormSwords;
      public final DoubleEntry worldCleavingRange;

      private NightfallDragon() {
         super("nightfall_dragon", "config.annihilationblade.nightfall_dragon.title");
         this.judgementCutRange = doubleValue("judgement_cut_range", 50.0, 10.0, 200.0, "夜陨次元斩索敌与落点最大半径。", "Nightfall Judgement Cut search radius.");
         this.judgementCutTotalCuts = intValue("judgement_cut_total_cuts", 100, 10, 500, "夜陨次元斩单次释放总切痕数。", "Nightfall Judgement Cut total cuts.");
         this.judgementCutIntervalTicks = intValue("judgement_cut_interval_ticks", 2, 1, 20, "夜陨次元斩每斩间隔 tick。", "Ticks between Judgement Cut slashes.");
         this.judgementCutDamage = doubleValue("judgement_cut_damage", 91.0, 1.0, 1000.0, "单次次元斩基础伤害。", "Base damage per Judgement Cut.");
         this.judgementCutScale = doubleValue("judgement_cut_scale", 8.0, 1.0, 32.0, "次元斩判定与模型尺寸倍率。", "Judgement Cut size scale multiplier.");

         this.scaleGuardSwordCount = intValue("scale_guard_sword_count", 16, 4, 64, "鳞之卫魔龙剑环召唤的幻影剑数量。", "Scale Guard sword count.");

         this.cosmicDescentMaxDistance = doubleValue("cosmic_descent_max_distance", 120.0, 20.0, 500.0, "神陨降临焦点搜索最大距离。", "Cosmic Nightfall Descent max focal distance.");
         this.cosmicDescentVortexRadius = doubleValue("cosmic_descent_vortex_radius", 72.0, 10.0, 256.0, "神陨降临引力漩涡拉扯半径。", "Vortex pull radius for Cosmic Descent.");
         this.cosmicDescentExplosionRadius = doubleValue("cosmic_descent_explosion_radius", 72.0, 10.0, 256.0, "神陨降临暗星坍缩最终爆炸半径。", "Final collapse explosion radius for Cosmic Descent.");
         this.cosmicDescentMeteorWaves = intValue("cosmic_descent_meteor_waves", 20, 3, 50, "神陨降临夜陨星雨轰炸波次数。", "Meteor rain wave count for Cosmic Descent.");
         this.cosmicDescentSwordsPerWave = intValue("cosmic_descent_swords_per_wave", 5, 1, 20, "神陨降临夜陨星雨每波降落的剑雨数量。", "Falling swords per meteor wave for Cosmic Descent.");
         this.cosmicDescentMeteorDamage = doubleValue("cosmic_descent_meteor_damage", 78.0, 1.0, 1000.0, "夜陨星雨单波基础伤害。", "Base meteor wave damage.");
         this.cosmicDescentCollapsePanelMultiplier = doubleValue("cosmic_descent_collapse_panel_multiplier", 6.0, 1.0, 50.0, "暗星坍缩最终爆发面板伤害倍率。", "Collapse explosion panel damage multiplier.");

         this.demonicBloodExtraDamagePercent = doubleValue("demonic_blood_extra_damage_percent", 0.05, 0.0, 1.0, "魔血寄生命中时扣除的最大生命值百分比。", "Demonic Blood Parasite extra max health damage percent.");
         this.demonicBloodPhantomBurstMarks = intValue("demonic_blood_phantom_burst_marks", 20, 1, 100, "触发魔血幻影爆发所需印记层数。", "Demonic Blood Parasite phantom burst required marks.");

         this.absoluteDomainRadius = doubleValue("absolute_domain_radius", 64.0, 8.0, 256.0, "终焉绝对结界辐射半径。", "Absolute Annihilation Domain radius.");
         this.absoluteDomainMaxTargets = intValue("absolute_domain_max_targets", 12, 1, 64, "终焉绝对结界单次处理最大目标数。", "Absolute Annihilation Domain max targets per sweep.");
         this.bladeStormSwords = intValue("blade_storm_swords", 16, 10, 300, "万刃龙魂挥刀生成的灭世龙刃总数量。", "Myriad Dragon Blade Storm summoned sword count.");
         this.worldCleavingRange = doubleValue("world_cleaving_range", 72.0, 10.0, 300.0, "灭界龙威剑气撕裂苍穹最大射程。", "World Cleaving Slash beam max range.");
      }
   }

   private static DoubleEntry visualScale(double defaultValue) {
      return doubleValue("visual_scale", defaultValue, 0.25, 2.0, "视觉倍率，只影响低风险视觉半径/粒子密度，建议 0.5-1.25。", "Visual scale; only affects low-risk visual radius/particle density. Suggested: 0.5-1.25.");
   }

   private static IntegerEntry intValue(
      String name, int defaultValue, int min, int max, String chineseComment, String englishComment
   ) {
      return IntegerEntry.builder("config.annihilationblade." + name, defaultValue)
         .range(min, max)
         .key(name)
         .tooltip(Component.literal(chineseComment + " 推荐最小/最大值: " + min + "-" + max + ".\n" + englishComment + " Suggested min/max: " + min + "-" + max + "."))
         .build();
   }

   private static DoubleEntry doubleValue(
      String name, double defaultValue, double min, double max, String chineseComment, String englishComment
   ) {
      return DoubleEntry.builder("config.annihilationblade." + name, defaultValue)
         .range(min, max)
         .key(name)
         .tooltip(Component.literal(chineseComment + " 推荐最小/最大值: " + min + "-" + max + ".\n" + englishComment + " Suggested min/max: " + min + "-" + max + "."))
         .build();
   }

   private static BooleanEntry boolValue(String name, boolean defaultValue, String tooltipKey) {
      return BooleanEntry.builder("config.annihilationblade." + name, defaultValue)
         .key(name)
         .tooltip(tooltipKey)
         .build();
   }
}
