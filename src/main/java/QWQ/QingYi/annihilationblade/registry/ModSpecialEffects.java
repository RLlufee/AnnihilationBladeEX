package QWQ.QingYi.annihilationblade.registry;

import QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect.AbyssalDecree;
import QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect.CausalityCollapse;
import QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect.Dankong;
import QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect.PhantomJudgement;
import QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect.StarlessJudgement;
import QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect.TerminusEcho;
import QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect.VoidDominion;
import QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect.WorldRift;
import QWQ.QingYi.annihilationblade.infinity_stellaris.specialeffect.CosmicStringCut;
import QWQ.QingYi.annihilationblade.infinity_stellaris.specialeffect.CurvatureRupture;
import QWQ.QingYi.annihilationblade.infinity_stellaris.specialeffect.EntropyDissolution;
import QWQ.QingYi.annihilationblade.infinity_stellaris.specialeffect.GammaThunderburst;
import QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect.AbsoluteAnnihilationDomain;
import QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect.DemonicBloodParasite;
import QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect.DragonPressureDomain;
import QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect.DragonGodBody;
import QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect.MyriadDragonBladeStorm;
import QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect.OuterGodScar;
import QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect.ReverseScaleHunt;
import QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect.WorldCleavingSlash;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModSpecialEffects {
   public static final DeferredRegister<SpecialEffect> SPECIAL_EFFECTS = DeferredRegister.create(SpecialEffect.REGISTRY_KEY, "annihilationblade");
   public static final RegistryObject<SpecialEffect> DANKONG = SPECIAL_EFFECTS.register("dankong", Dankong::new);
   public static final RegistryObject<SpecialEffect> WORLD_RIFT = SPECIAL_EFFECTS.register("world_rift", WorldRift::new);
   public static final RegistryObject<SpecialEffect> TERMINUS_ECHO = SPECIAL_EFFECTS.register("terminus_echo", TerminusEcho::new);
   public static final RegistryObject<SpecialEffect> VOID_DOMINION = SPECIAL_EFFECTS.register("void_dominion", VoidDominion::new);
   public static final RegistryObject<SpecialEffect> CAUSALITY_COLLAPSE = SPECIAL_EFFECTS.register("causality_collapse", CausalityCollapse::new);
   public static final RegistryObject<SpecialEffect> STARLESS_JUDGEMENT = SPECIAL_EFFECTS.register("starless_judgement", StarlessJudgement::new);
   public static final RegistryObject<SpecialEffect> PHANTOM_JUDGEMENT = SPECIAL_EFFECTS.register("phantom_judgement", PhantomJudgement::new);
   public static final RegistryObject<SpecialEffect> ABYSSAL_DECREE = SPECIAL_EFFECTS.register("abyssal_decree", AbyssalDecree::new);
   public static final RegistryObject<SpecialEffect> BLOOD_LEECH = SPECIAL_EFFECTS.register("blood_leech", () -> new SpecialEffect(0, false, false));
   public static final RegistryObject<SpecialEffect> SPIRIT_SHIELD = SPECIAL_EFFECTS.register("spirit_shield", () -> new SpecialEffect(0, false, false));
   public static final RegistryObject<SpecialEffect> PHANTOM_MARK = SPECIAL_EFFECTS.register("phantom_mark", () -> new SpecialEffect(0, false, false));
   public static final RegistryObject<SpecialEffect> ENTROPY_DISSOLUTION = SPECIAL_EFFECTS.register("entropy_dissolution", EntropyDissolution::new);
   public static final RegistryObject<SpecialEffect> CURVATURE_RUPTURE = SPECIAL_EFFECTS.register("curvature_rupture", CurvatureRupture::new);
   public static final RegistryObject<SpecialEffect> GAMMA_THUNDERBURST = SPECIAL_EFFECTS.register("gamma_thunderburst", GammaThunderburst::new);
   public static final RegistryObject<SpecialEffect> COSMIC_STRING_CUT = SPECIAL_EFFECTS.register("cosmic_string_cut", CosmicStringCut::new);
   public static final RegistryObject<SpecialEffect> DEMONIC_BLOOD_PARASITE = SPECIAL_EFFECTS.register("demonic_blood_parasite", DemonicBloodParasite::new);
   public static final RegistryObject<SpecialEffect> OUTER_GOD_SCAR = SPECIAL_EFFECTS.register("outer_god_scar", OuterGodScar::new);
   public static final RegistryObject<SpecialEffect> DRAGON_PRESSURE_DOMAIN = SPECIAL_EFFECTS.register("dragon_pressure_domain", DragonPressureDomain::new);
   public static final RegistryObject<SpecialEffect> REVERSE_SCALE_HUNT = SPECIAL_EFFECTS.register("reverse_scale_hunt", ReverseScaleHunt::new);
   public static final RegistryObject<SpecialEffect> DRAGON_GOD_BODY = SPECIAL_EFFECTS.register("dragon_god_body", DragonGodBody::new);
   public static final RegistryObject<SpecialEffect> ABSOLUTE_ANNIHILATION_DOMAIN = SPECIAL_EFFECTS.register("absolute_annihilation_domain", AbsoluteAnnihilationDomain::new);
   public static final RegistryObject<SpecialEffect> MYRIAD_DRAGON_BLADE_STORM = SPECIAL_EFFECTS.register("myriad_dragon_blade_storm", MyriadDragonBladeStorm::new);
   public static final RegistryObject<SpecialEffect> WORLD_CLEAVING_SLASH = SPECIAL_EFFECTS.register("world_cleaving_slash", WorldCleavingSlash::new);

   public static void register(IEventBus eventBus) {
      SPECIAL_EFFECTS.register(eventBus);
   }
}
