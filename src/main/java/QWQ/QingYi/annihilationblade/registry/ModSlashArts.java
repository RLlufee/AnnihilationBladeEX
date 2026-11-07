package QWQ.QingYi.annihilationblade.registry;

import QWQ.QingYi.annihilationblade.blood_prison.logic.BloodPrisonLogic;
import QWQ.QingYi.annihilationblade.infinity_stellaris.logic.VacuumDecayCollapseLogic;
import QWQ.QingYi.annihilationblade.loli_blade.logic.LoliBladeCombatLogic;
import QWQ.QingYi.annihilationblade.nightfall_dragon.logic.CosmicNightfallDescentLogic;
import QWQ.QingYi.annihilationblade.nightfall_dragon.logic.NightfallDragonJudgementCutLogic;
import QWQ.QingYi.annihilationblade.nightfall_dragon.logic.ScaleGuardLogic;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModSlashArts {
   public static final DeferredRegister<SlashArts> ARTS = DeferredRegister.create(SlashArts.REGISTRY_KEY, "annihilationblade");
   public static final RegistryObject<SlashArts> SPATIAL_FRACTURE = ARTS.register(
      "spatial_fracture", () -> new SlashArts(entity -> ModComboStates.SPATIAL_FRACTURE_STATE.getId())
   );
   public static final RegistryObject<SlashArts> INFERNAL_SLAUGHTER = ARTS.register("infernal_slaughter", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         BloodPrisonLogic.activateDomain(player);
      }

      return ModComboStates.INFERNAL_SLAUGHTER_STATE.getId();
   }));
   public static final RegistryObject<SlashArts> VACUUM_DECAY_COLLAPSE = ARTS.register("vacuum_decay_collapse", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         VacuumDecayCollapseLogic.prepareCast(player);
      }

      return ModComboStates.VACUUM_DECAY_COLLAPSE_STATE.getId();
   }));
   public static final RegistryObject<SlashArts> NIGHTFALL_JUDGEMENT_CUT = ARTS.register("nightfall_judgement_cut", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         NightfallDragonJudgementCutLogic.prepareCast(player);
      }

      return ModComboStates.NIGHTFALL_JUDGEMENT_CUT_STATE.getId();
   }));
   public static final RegistryObject<SlashArts> SCALE_GUARD = ARTS.register("scale_guard", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         ScaleGuardLogic.prepareCast(player);
      }

      return ModComboStates.SCALE_GUARD_STATE.getId();
   }));
   public static final RegistryObject<SlashArts> COSMIC_NIGHTFALL_DESCENT = ARTS.register("cosmic_nightfall_descent", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         CosmicNightfallDescentLogic.prepareCast(player);
      }

      return ModComboStates.COSMIC_NIGHTFALL_DESCENT_STATE.getId();
   }));
   public static final RegistryObject<SlashArts> LOLI_AREA_EXECUTION = ARTS.register("loli_area_execution", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         LoliBladeCombatLogic.prepareCast(player);
      }

      return ModComboStates.LOLI_AREA_EXECUTION_STATE.getId();
   }));

   public static void register(IEventBus eventBus) {
      ARTS.register(eventBus);
   }
}
