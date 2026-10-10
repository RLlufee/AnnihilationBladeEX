package QWQ.QingYi.annihilationbladeex.loli_blade;

import QWQ.QingYi.annihilationbladeex.loli_blade.logic.LoliBladeCombatLogic;
import QWQ.QingYi.annihilationbladeex.loli_blade.logic.LoliBladeDefenseLogic;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationbladeex")
public final class LoliBladeEventHandler {
   private LoliBladeEventHandler() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onLivingAttack(LivingIncomingDamageEvent event) {
      LoliBladeDefenseLogic.onLivingAttack(event);
      LoliBladeCombatLogic.handleLivingAttack(event);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onLivingHurt(LivingDamageEvent.Pre event) {
      LoliBladeDefenseLogic.onLivingHurt(event);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onLivingDamage(LivingDamageEvent.Post event) {
      LoliBladeCombatLogic.handleLivingDamage(event);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onLivingDeath(LivingDeathEvent event) {
      LoliBladeDefenseLogic.onLivingDeath(event);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onAttackEntity(AttackEntityEvent event) {
      LoliBladeCombatLogic.handleEntityAttack(event);
   }

   @SubscribeEvent
   public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
      if (!event.getEntity().level().isClientSide) {
         LoliBladeDefinitions.bindUnownedStacks(event.getEntity());
      }
      LoliBladeDefenseLogic.onPlayerTick(event);
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerLoggedOutEvent event) {
      LoliBladeDefenseLogic.clearPlayerState(event.getEntity());
      LoliBladeCombatLogic.clearPlayerState(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      LoliBladeDefenseLogic.clearPlayerState(event.getEntity());
      LoliBladeCombatLogic.clearPlayerState(event.getEntity().getUUID());
   }
}
