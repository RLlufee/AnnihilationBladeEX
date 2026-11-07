package QWQ.QingYi.annihilationblade.loli_blade;

import QWQ.QingYi.annihilationblade.loli_blade.logic.LoliBladeCombatLogic;
import QWQ.QingYi.annihilationblade.loli_blade.logic.LoliBladeDefenseLogic;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationblade")
public final class LoliBladeEventHandler {
   private LoliBladeEventHandler() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onLivingAttack(LivingAttackEvent event) {
      LoliBladeDefenseLogic.onLivingAttack(event);
      LoliBladeCombatLogic.handleLivingAttack(event);
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onLivingHurt(LivingHurtEvent event) {
      LoliBladeDefenseLogic.onLivingHurt(event);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onLivingDamage(LivingDamageEvent event) {
      if (!event.isCanceled()) {
         LoliBladeCombatLogic.handleLivingDamage(event);
      }
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
   public static void onPlayerTick(PlayerTickEvent event) {
      if (!event.player.level().isClientSide) {
         LoliBladeDefinitions.bindUnownedStacks(event.player);
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
