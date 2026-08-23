package QWQ.QingYi.annihilationbladeex.config;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
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
         super(ResourceLocation.fromNamespaceAndPath(AnnihilationBladeEX.MODID, "common"), "config.annihilationbladeex.common.title", "./config/annihilationbladeex/common.json");
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
         super(ResourceLocation.fromNamespaceAndPath(AnnihilationBladeEX.MODID, "client"), "config.annihilationbladeex.client.title", "./config/annihilationbladeex/client.json");
      }
   }

   public static final class ClientTooltips extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final BooleanEntry enableAnnihilationBladeRenderer = boolValue("client_tooltips", "enable_annihilation_blade_renderer", true);
      public final BooleanEntry enableInfinityStellarisRenderer = boolValue("client_tooltips", "enable_infinity_stellaris_renderer", true);

      private ClientTooltips() {
         super("client_tooltips", "config.annihilationbladeex.client_tooltips.title");
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
         super("annihilation_blade.spatial_fracture", "config.annihilationbladeex.annihilation_blade.spatial_fracture.title");
         this.maxDistance = doubleValue("annihilation_blade.spatial_fracture", "max_distance", 160.0, 32.0, 256.0);
         this.fractureRadius = doubleValue("annihilation_blade.spatial_fracture", "fracture_radius", 20.0, 4.0, 64.0);
         this.rayStep = doubleValue("annihilation_blade.spatial_fracture", "ray_step", 4.0, 1.0, 12.0);
         this.raySampleRadius = doubleValue("annihilation_blade.spatial_fracture", "ray_sample_radius", 5.0, 1.0, 16.0);
         this.entityLockRadius = doubleValue("annihilation_blade.spatial_fracture", "entity_lock_radius", 3.0, 0.5, 10.0);
         this.backupRadius = doubleValue("annihilation_blade.spatial_fracture", "backup_radius", 48.0, 8.0, 96.0);
         this.maxTargets = intValue("annihilation_blade.spatial_fracture", "max_targets", 128, 1, 256);
         this.maxVisualizedTargets = intValue("annihilation_blade.spatial_fracture", "max_visualized_targets", 32, 0, 128);
         this.fractureSlashes = intValue("annihilation_blade.spatial_fracture", "fracture_slashes", 24, 0, 96);
         this.centerSlashes = intValue("annihilation_blade.spatial_fracture", "center_slashes", 12, 0, 48);
         this.visualScale = visualScale("annihilation_blade.spatial_fracture", 1.0);
      }
   }

   public static final class Dankong extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry range;
      public final IntegerEntry maxTargets;
      public final IntegerEntry stepInterval;
      public final IntegerEntry cooldownTicks;
      public final DoubleEntry visualScale;

      private Dankong() {
         super("annihilation_blade.dankong", "config.annihilationbladeex.annihilation_blade.dankong.title");
         this.range = doubleValue("annihilation_blade.dankong", "range", 72.0, 8.0, 256.0);
         this.maxTargets = intValue("annihilation_blade.dankong", "max_targets", 48, 1, 128);
         this.stepInterval = intValue("annihilation_blade.dankong", "step_interval_ticks", 3, 1, 12);
         this.cooldownTicks = intValue("annihilation_blade.dankong", "cooldown_ticks", 12, 0, 100);
         this.visualScale = visualScale("annihilation_blade.dankong", 1.0);
      }
   }

   public static final class WorldRift extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry radius;
      public final IntegerEntry maxTargets;
      public final IntegerEntry chainCount;
      public final DoubleEntry chainRange;
      public final DoubleEntry visualScale;

      private WorldRift() {
         super("annihilation_blade.world_rift", "config.annihilationbladeex.annihilation_blade.world_rift.title");
         this.radius = doubleValue("annihilation_blade.world_rift", "radius", 8.0, 2.0, 32.0);
         this.maxTargets = intValue("annihilation_blade.world_rift", "max_targets", 24, 1, 96);
         this.chainCount = intValue("annihilation_blade.world_rift", "chain_count", 3, 1, 10);
         this.chainRange = doubleValue("annihilation_blade.world_rift", "chain_range", 256.0, 1.0, 1024.0);
         this.visualScale = visualScale("annihilation_blade.world_rift", 1.0);
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
         super("annihilation_blade.terminus_echo", "config.annihilationbladeex.annihilation_blade.terminus_echo.title");
         this.range = doubleValue("annihilation_blade.terminus_echo", "range", 36.0, 8.0, 96.0);
         this.width = doubleValue("annihilation_blade.terminus_echo", "width", 4.4, 1.0, 16.0);
         this.echoCount = intValue("annihilation_blade.terminus_echo", "echo_count", 5, 1, 12);
         this.echoInterval = intValue("annihilation_blade.terminus_echo", "echo_interval_ticks", 3, 1, 12);
         this.cooldownTicks = intValue("annihilation_blade.terminus_echo", "cooldown_ticks", 0, 0, 120);
         this.maxActiveSequences = intValue("annihilation_blade.terminus_echo", "max_active_sequences", 2, 1, 8);
         this.maxTargetsPerWave = intValue("annihilation_blade.terminus_echo", "max_targets_per_wave", 32, 1, 128);
         this.visualScale = visualScale("annihilation_blade.terminus_echo", 1.0);
      }
   }

   public static final class VoidDominion extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry range;
      public final IntegerEntry maxTargets;
      public final IntegerEntry cooldownTicks;
      public final DoubleEntry visualScale;

      private VoidDominion() {
         super("annihilation_blade.void_dominion", "config.annihilationbladeex.annihilation_blade.void_dominion.title");
         this.range = doubleValue("annihilation_blade.void_dominion", "range", 26.0, 4.0, 64.0);
         this.maxTargets = intValue("annihilation_blade.void_dominion", "max_targets", 64, 1, 160);
         this.cooldownTicks = intValue("annihilation_blade.void_dominion", "cooldown_ticks", 70, 0, 200);
         this.visualScale = visualScale("annihilation_blade.void_dominion", 1.0);
      }
   }

   public static final class CausalityCollapse extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry chainRadius;
      public final IntegerEntry maxChain;
      public final IntegerEntry cooldownTicks;
      public final DoubleEntry visualScale;

      private CausalityCollapse() {
         super("annihilation_blade.causality_collapse", "config.annihilationbladeex.annihilation_blade.causality_collapse.title");
         this.chainRadius = doubleValue("annihilation_blade.causality_collapse", "chain_radius", 14.0, 2.0, 48.0);
         this.maxChain = intValue("annihilation_blade.causality_collapse", "max_chain", 18, 1, 96);
         this.cooldownTicks = intValue("annihilation_blade.causality_collapse", "cooldown_ticks", 10, 0, 100);
         this.visualScale = visualScale("annihilation_blade.causality_collapse", 1.0);
      }
   }

   public static final class StarlessJudgement extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry range;
      public final DoubleEntry width;
      public final IntegerEntry cooldownTicks;
      public final IntegerEntry maxTargets;
      public final DoubleEntry visualScale;

      private StarlessJudgement() {
         super("annihilation_blade.starless_judgement", "config.annihilationbladeex.annihilation_blade.starless_judgement.title");
         this.range = doubleValue("annihilation_blade.starless_judgement", "range", 56.0, 8.0, 128.0);
         this.width = doubleValue("annihilation_blade.starless_judgement", "width", 8.5, 1.0, 24.0);
         this.cooldownTicks = intValue("annihilation_blade.starless_judgement", "cooldown_ticks", 34, 0, 160);
         this.maxTargets = intValue("annihilation_blade.starless_judgement", "max_targets", 80, 1, 200);
         this.visualScale = visualScale("annihilation_blade.starless_judgement", 1.0);
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
         super("annihilation_blade.phantom_judgement", "config.annihilationbladeex.annihilation_blade.phantom_judgement.title");
         this.range = doubleValue("annihilation_blade.phantom_judgement", "range", 40.0, 8.0, 96.0);
         this.searchTicks = intValue("annihilation_blade.phantom_judgement", "search_ticks", 20, 4, 80);
         this.swordCount = intValue("annihilation_blade.phantom_judgement", "search_sword_count", 8, 1, 24);
         this.rainSwordsPerTarget = intValue("annihilation_blade.phantom_judgement", "rain_swords_per_target", 6, 1, 16);
         this.fallingSwordDelayTicks = intValue("annihilation_blade.phantom_judgement", "falling_sword_delay_ticks", 24, 0, 80);
         this.lingerTicks = intValue("annihilation_blade.phantom_judgement", "linger_ticks", 60, 0, 200);
         this.maxTargets = intValue("annihilation_blade.phantom_judgement", "max_targets", 24, 1, 96);
         this.maxLingeringSwords = intValue("annihilation_blade.phantom_judgement", "max_lingering_swords", 96, 0, 256);
         this.cooldownTicks = intValue("annihilation_blade.phantom_judgement", "cooldown_ticks", 44, 0, 200);
         this.visualScale = visualScale("annihilation_blade.phantom_judgement", 1.0);
      }
   }

   public static final class AbyssalDecree extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final DoubleEntry range;
      public final IntegerEntry maxTargets;
      public final IntegerEntry strikeInterval;
      public final IntegerEntry cooldownTicks;
      public final DoubleEntry visualScale;

      private AbyssalDecree() {
         super("annihilation_blade.abyssal_decree", "config.annihilationbladeex.annihilation_blade.abyssal_decree.title");
         this.range = doubleValue("annihilation_blade.abyssal_decree", "range", 34.0, 6.0, 96.0);
         this.maxTargets = intValue("annihilation_blade.abyssal_decree", "max_targets", 16, 1, 80);
         this.strikeInterval = intValue("annihilation_blade.abyssal_decree", "strike_interval_ticks", 3, 1, 12);
         this.cooldownTicks = intValue("annihilation_blade.abyssal_decree", "cooldown_ticks", 82, 0, 240);
         this.visualScale = visualScale("annihilation_blade.abyssal_decree", 1.0);
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
         super("blood_prison.domain", "config.annihilationbladeex.blood_prison.domain.title");
         this.durationTicks = intValue("blood_prison.domain", "duration_ticks", 400, 40, 1200);
         this.radius = doubleValue("blood_prison.domain", "radius", 10.0, 3.0, 32.0);
         this.borderIntervalTicks = intValue("blood_prison.domain", "border_interval_ticks", 10, 2, 40);
         this.playerAuraIntervalTicks = intValue("blood_prison.domain", "player_aura_interval_ticks", 4, 1, 40);
         this.pulseIntervalTicks = intValue("blood_prison.domain", "pulse_interval_ticks", 20, 4, 80);
         this.visualScale = visualScale("blood_prison.domain", 1.0);
      }
   }

   public static final class PhantomBurst extends AutoInitConfigContainer.AutoInitConfigCategoryBase {
      public final IntegerEntry swordCount;
      public final IntegerEntry swordDelayTicks;
      public final DoubleEntry burstRadiusScale;
      public final DoubleEntry visualScale;

      private PhantomBurst() {
         super("blood_prison.phantom_burst", "config.annihilationbladeex.blood_prison.phantom_burst.title");
         this.swordCount = intValue("blood_prison.phantom_burst", "sword_count", 10, 1, 32);
         this.swordDelayTicks = intValue("blood_prison.phantom_burst", "sword_delay_ticks", 18, 0, 80);
         this.burstRadiusScale = doubleValue("blood_prison.phantom_burst", "burst_radius_scale", 2.2, 0.5, 5.0);
         this.visualScale = visualScale("blood_prison.phantom_burst", 1.0);
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
         super("infinity_stellaris", "config.annihilationbladeex.infinity_stellaris.title");
         this.entropyPercent = doubleValue("infinity_stellaris", "entropy_percent", 0.10, 0.0, 1.0);
         this.entropyMarks = intValue("infinity_stellaris", "entropy_marks", 10, 1, 100);
         this.entropyBlacklistTicks = intValue("infinity_stellaris", "entropy_blacklist_ticks", 40, 0, 400);
         this.curvatureRadius = doubleValue("infinity_stellaris", "curvature_radius", 32.0, 1.0, 128.0);
         this.curvatureTickInterval = intValue("infinity_stellaris", "curvature_tick_interval", 20, 1, 100);
         this.curvatureMaxTargets = intValue("infinity_stellaris", "curvature_max_targets", 64, 1, 256);
         this.curvatureBurstMarks = intValue("infinity_stellaris", "curvature_burst_marks", 5, 1, 100);
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
         super("nightfall_dragon", "config.annihilationbladeex.nightfall_dragon.title");
         this.judgementCutRange = doubleValue("nightfall_dragon", "judgement_cut_range", 50.0, 10.0, 200.0);
         this.judgementCutTotalCuts = intValue("nightfall_dragon", "judgement_cut_total_cuts", 100, 10, 500);
         this.judgementCutIntervalTicks = intValue("nightfall_dragon", "judgement_cut_interval_ticks", 2, 1, 20);
         this.judgementCutDamage = doubleValue("nightfall_dragon", "judgement_cut_damage", 91.0, 1.0, 1000.0);
         this.judgementCutScale = doubleValue("nightfall_dragon", "judgement_cut_scale", 8.0, 1.0, 32.0);
         this.scaleGuardSwordCount = intValue("nightfall_dragon", "scale_guard_sword_count", 16, 4, 64);
         this.cosmicDescentMaxDistance = doubleValue("nightfall_dragon", "cosmic_descent_max_distance", 120.0, 20.0, 500.0);
         this.cosmicDescentVortexRadius = doubleValue("nightfall_dragon", "cosmic_descent_vortex_radius", 72.0, 10.0, 256.0);
         this.cosmicDescentExplosionRadius = doubleValue("nightfall_dragon", "cosmic_descent_explosion_radius", 72.0, 10.0, 256.0);
         this.cosmicDescentMeteorWaves = intValue("nightfall_dragon", "cosmic_descent_meteor_waves", 20, 3, 50);
         this.cosmicDescentSwordsPerWave = intValue("nightfall_dragon", "cosmic_descent_swords_per_wave", 5, 1, 20);
         this.cosmicDescentMeteorDamage = doubleValue("nightfall_dragon", "cosmic_descent_meteor_damage", 78.0, 1.0, 1000.0);
         this.cosmicDescentCollapsePanelMultiplier = doubleValue("nightfall_dragon", "cosmic_descent_collapse_panel_multiplier", 6.0, 1.0, 50.0);
         this.demonicBloodExtraDamagePercent = doubleValue("nightfall_dragon", "demonic_blood_extra_damage_percent", 0.05, 0.0, 1.0);
         this.demonicBloodPhantomBurstMarks = intValue("nightfall_dragon", "demonic_blood_phantom_burst_marks", 20, 1, 100);
         this.absoluteDomainRadius = doubleValue("nightfall_dragon", "absolute_domain_radius", 64.0, 8.0, 256.0);
         this.absoluteDomainMaxTargets = intValue("nightfall_dragon", "absolute_domain_max_targets", 12, 1, 64);
         this.bladeStormSwords = intValue("nightfall_dragon", "blade_storm_swords", 16, 10, 300);
         this.worldCleavingRange = doubleValue("nightfall_dragon", "world_cleaving_range", 72.0, 10.0, 300.0);
      }
   }

   private static DoubleEntry visualScale(String category, double defaultValue) {
      return doubleValue(category, "visual_scale", defaultValue, 0.25, 2.0);
   }

   private static IntegerEntry intValue(String category, String name, int defaultValue, int min, int max) {
      String key = "config.annihilationbladeex." + category + "." + name;
      return IntegerEntry.builder(key, defaultValue)
         .range(min, max)
         .key(name)
         .tooltip(Component.translatable(key + ".tooltip"))
         .build();
   }

   private static DoubleEntry doubleValue(String category, String name, double defaultValue, double min, double max) {
      String key = "config.annihilationbladeex." + category + "." + name;
      return DoubleEntry.builder(key, defaultValue)
         .range(min, max)
         .key(name)
         .tooltip(Component.translatable(key + ".tooltip"))
         .build();
   }

   private static BooleanEntry boolValue(String category, String name, boolean defaultValue) {
      String key = "config.annihilationbladeex." + category + "." + name;
      return BooleanEntry.builder(key, defaultValue)
         .key(name)
         .tooltip(Component.translatable(key + ".tooltip"))
         .build();
   }
}
