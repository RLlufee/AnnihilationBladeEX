package QWQ.QingYi.annihilationbladeex.loli_blade.specialeffect;

import QWQ.QingYi.annihilationbladeex.loli_blade.LoliBladeDefinitions;
import QWQ.QingYi.annihilationbladeex.loli_blade.logic.LoliBladeCombatLogic;
import QWQ.QingYi.annihilationbladeex.registry.ModSpecialEffects;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationbladeex")
public class LoliFacingExecution extends SpecialEffect {
   public LoliFacingExecution() {
      super(0, false, false);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onDoingSlash(DoSlashEvent event) {
      if (!(event.getUser() instanceof ServerPlayer player)) {
         return;
      }

      if (player.isShiftKeyDown()) {
         return;
      }

      ISlashBladeState state = event.getSlashBladeState();
      if (!state.hasSpecialEffect(ModSpecialEffects.LOLI_FACING_EXECUTION.getId())) {
         return;
      }

      if (!LoliBladeDefinitions.isOwnedBy(player.getMainHandItem(), player)) {
         return;
      }

      LoliBladeCombatLogic.handleFacingSpecialEffect(player);
   }
}
