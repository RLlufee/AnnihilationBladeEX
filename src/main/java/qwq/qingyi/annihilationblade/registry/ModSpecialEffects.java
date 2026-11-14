package qwq.qingyi.annihilationblade.registry;

import mods.flammpfeil.slashblade.registry.SpecialEffectsRegistry;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.AbyssalDecree;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.CausalityCollapse;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.Dankong;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.PhantomJudgement;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.StarlessJudgement;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.TerminusEcho;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.VoidDominion;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.WorldRift;
import qwq.qingyi.annihilationblade.infinity_stellaris.specialeffect.CosmicStringCut;
import qwq.qingyi.annihilationblade.infinity_stellaris.specialeffect.CurvatureRupture;
import qwq.qingyi.annihilationblade.infinity_stellaris.specialeffect.EntropyDissolution;
import qwq.qingyi.annihilationblade.infinity_stellaris.specialeffect.GammaThunderburst;
import qwq.qingyi.annihilationblade.loli_blade.specialeffect.LoliFacingExecution;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.AbsoluteAnnihilationDomain;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.DemonicBloodParasite;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.DragonGodBody;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.DragonPressureDomain;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.MyriadDragonBladeStorm;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.OuterGodScar;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.ReverseScaleHunt;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.WorldCleavingSlash;

/**
 * 拔刀剑特殊效果（SE）注册表，提供常量 ID 与已注册实例。
 */
public final class ModSpecialEffects {
    public static final Identifier DANKONG_ID = AnnihilationBladeex.prefix("dankong");
    public static final Identifier WORLD_RIFT_ID = AnnihilationBladeex.prefix("world_rift");
    public static final Identifier TERMINUS_ECHO_ID = AnnihilationBladeex.prefix("terminus_echo");
    public static final Identifier VOID_DOMINION_ID = AnnihilationBladeex.prefix("void_dominion");
    public static final Identifier CAUSALITY_COLLAPSE_ID = AnnihilationBladeex.prefix("causality_collapse");
    public static final Identifier STARLESS_JUDGEMENT_ID = AnnihilationBladeex.prefix("starless_judgement");
    public static final Identifier PHANTOM_JUDGEMENT_ID = AnnihilationBladeex.prefix("phantom_judgement");
    public static final Identifier ABYSSAL_DECREE_ID = AnnihilationBladeex.prefix("abyssal_decree");

    public static final Identifier BLOOD_LEECH_ID = AnnihilationBladeex.prefix("blood_leech");
    public static final Identifier SPIRIT_SHIELD_ID = AnnihilationBladeex.prefix("spirit_shield");
    public static final Identifier PHANTOM_MARK_ID = AnnihilationBladeex.prefix("phantom_mark");

    public static final Identifier ENTROPY_DISSOLUTION_ID = AnnihilationBladeex.prefix("entropy_dissolution");
    public static final Identifier CURVATURE_RUPTURE_ID = AnnihilationBladeex.prefix("curvature_rupture");
    public static final Identifier GAMMA_THUNDERBURST_ID = AnnihilationBladeex.prefix("gamma_thunderburst");
    public static final Identifier COSMIC_STRING_CUT_ID = AnnihilationBladeex.prefix("cosmic_string_cut");

    public static final Identifier DEMONIC_BLOOD_PARASITE_ID = AnnihilationBladeex.prefix("demonic_blood_parasite");
    public static final Identifier OUTER_GOD_SCAR_ID = AnnihilationBladeex.prefix("outer_god_scar");
    public static final Identifier DRAGON_PRESSURE_DOMAIN_ID = AnnihilationBladeex.prefix("dragon_pressure_domain");
    public static final Identifier REVERSE_SCALE_HUNT_ID = AnnihilationBladeex.prefix("reverse_scale_hunt");
    public static final Identifier DRAGON_GOD_BODY_ID = AnnihilationBladeex.prefix("dragon_god_body");
    public static final Identifier ABSOLUTE_ANNIHILATION_DOMAIN_ID = AnnihilationBladeex.prefix("absolute_annihilation_domain");
    public static final Identifier MYRIAD_DRAGON_BLADE_STORM_ID = AnnihilationBladeex.prefix("myriad_dragon_blade_storm");
    public static final Identifier WORLD_CLEAVING_SLASH_ID = AnnihilationBladeex.prefix("world_cleaving_slash");
    public static final Identifier LOLI_FACING_EXECUTION_ID = AnnihilationBladeex.prefix("loli_facing_execution");

    public static final SpecialEffect DANKONG = register(DANKONG_ID, new Dankong());
    public static final SpecialEffect WORLD_RIFT = register(WORLD_RIFT_ID, new WorldRift());
    public static final SpecialEffect TERMINUS_ECHO = register(TERMINUS_ECHO_ID, new TerminusEcho());
    public static final SpecialEffect VOID_DOMINION = register(VOID_DOMINION_ID, new VoidDominion());
    public static final SpecialEffect CAUSALITY_COLLAPSE = register(CAUSALITY_COLLAPSE_ID, new CausalityCollapse());
    public static final SpecialEffect STARLESS_JUDGEMENT = register(STARLESS_JUDGEMENT_ID, new StarlessJudgement());
    public static final SpecialEffect PHANTOM_JUDGEMENT = register(PHANTOM_JUDGEMENT_ID, new PhantomJudgement());
    public static final SpecialEffect ABYSSAL_DECREE = register(ABYSSAL_DECREE_ID, new AbyssalDecree());

    public static final SpecialEffect BLOOD_LEECH = register(BLOOD_LEECH_ID, new SpecialEffect(0, false, false));
    public static final SpecialEffect SPIRIT_SHIELD = register(SPIRIT_SHIELD_ID, new SpecialEffect(0, false, false));
    public static final SpecialEffect PHANTOM_MARK = register(PHANTOM_MARK_ID, new SpecialEffect(0, false, false));

    public static final SpecialEffect ENTROPY_DISSOLUTION = register(ENTROPY_DISSOLUTION_ID, new EntropyDissolution());
    public static final SpecialEffect CURVATURE_RUPTURE = register(CURVATURE_RUPTURE_ID, new CurvatureRupture());
    public static final SpecialEffect GAMMA_THUNDERBURST = register(GAMMA_THUNDERBURST_ID, new GammaThunderburst());
    public static final SpecialEffect COSMIC_STRING_CUT = register(COSMIC_STRING_CUT_ID, new CosmicStringCut());

    public static final SpecialEffect DEMONIC_BLOOD_PARASITE = register(DEMONIC_BLOOD_PARASITE_ID, new DemonicBloodParasite());
    public static final SpecialEffect OUTER_GOD_SCAR = register(OUTER_GOD_SCAR_ID, new OuterGodScar());
    public static final SpecialEffect DRAGON_PRESSURE_DOMAIN = register(DRAGON_PRESSURE_DOMAIN_ID, new DragonPressureDomain());
    public static final SpecialEffect REVERSE_SCALE_HUNT = register(REVERSE_SCALE_HUNT_ID, new ReverseScaleHunt());
    public static final SpecialEffect DRAGON_GOD_BODY = register(DRAGON_GOD_BODY_ID, new DragonGodBody());
    public static final SpecialEffect ABSOLUTE_ANNIHILATION_DOMAIN = register(ABSOLUTE_ANNIHILATION_DOMAIN_ID, new AbsoluteAnnihilationDomain());
    public static final SpecialEffect MYRIAD_DRAGON_BLADE_STORM = register(MYRIAD_DRAGON_BLADE_STORM_ID, new MyriadDragonBladeStorm());
    public static final SpecialEffect WORLD_CLEAVING_SLASH = register(WORLD_CLEAVING_SLASH_ID, new WorldCleavingSlash());
    public static final SpecialEffect LOLI_FACING_EXECUTION = register(LOLI_FACING_EXECUTION_ID, new LoliFacingExecution());

    private ModSpecialEffects() {
    }

    private static SpecialEffect register(Identifier id, SpecialEffect effect) {
        return Registry.register(SpecialEffectsRegistry.SPECIAL_EFFECT, id, effect);
    }

    public static void register() {
        // 触发静态初始化完成注册
    }
}
