package QWQ.QingYi.annihilationbladeex.registry;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.blood_prison.logic.BloodPrisonLogic;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.logic.VacuumDecayCollapseLogic;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.logic.CosmicNightfallDescentLogic;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.logic.NightfallDragonJudgementCutLogic;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.logic.ScaleGuardLogic;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSlashArts {
   public static final DeferredRegister<SlashArts> ARTS = DeferredRegister.create(SlashArts.REGISTRY_KEY, AnnihilationBladeEX.MODID);
   public static final DeferredHolder<SlashArts, SlashArts> SPATIAL_FRACTURE = ARTS.register(
      "spatial_fracture", () -> new SlashArts(entity -> ModComboStates.SPATIAL_FRACTURE_STATE.getId())
   );
   public static final DeferredHolder<SlashArts, SlashArts> INFERNAL_SLAUGHTER = ARTS.register("infernal_slaughter", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         BloodPrisonLogic.activateDomain(player);
      }

      return ModComboStates.INFERNAL_SLAUGHTER_STATE.getId();
   }));
   public static final DeferredHolder<SlashArts, SlashArts> VACUUM_DECAY_COLLAPSE = ARTS.register("vacuum_decay_collapse", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         VacuumDecayCollapseLogic.prepareCast(player);
      }

      return ModComboStates.VACUUM_DECAY_COLLAPSE_STATE.getId();
   }));
   public static final DeferredHolder<SlashArts, SlashArts> NIGHTFALL_JUDGEMENT_CUT = ARTS.register("nightfall_judgement_cut", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         NightfallDragonJudgementCutLogic.prepareCast(player);
      }

      return ModComboStates.NIGHTFALL_JUDGEMENT_CUT_STATE.getId();
   }));
   public static final DeferredHolder<SlashArts, SlashArts> SCALE_GUARD = ARTS.register("scale_guard", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         ScaleGuardLogic.prepareCast(player);
      }

      return ModComboStates.SCALE_GUARD_STATE.getId();
   }));
   public static final DeferredHolder<SlashArts, SlashArts> COSMIC_NIGHTFALL_DESCENT = ARTS.register("cosmic_nightfall_descent", () -> new SlashArts(entity -> {
      if (entity instanceof Player player && !player.level().isClientSide) {
         CosmicNightfallDescentLogic.prepareCast(player);
      }

      return ModComboStates.COSMIC_NIGHTFALL_DESCENT_STATE.getId();
   }));

   public static void register(IEventBus eventBus) {
      ARTS.register(eventBus);
   }
}
