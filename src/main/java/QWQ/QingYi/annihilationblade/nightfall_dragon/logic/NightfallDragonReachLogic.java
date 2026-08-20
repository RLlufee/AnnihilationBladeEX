package QWQ.QingYi.annihilationblade.nightfall_dragon.logic;

import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationblade")
public final class NightfallDragonReachLogic {
   private static final UUID ENTITY_REACH_MODIFIER_ID = UUID.fromString("1d9d9ab1-3ea2-4efb-8428-2bda68d3f9b4");
   private static final double ENTITY_REACH_BONUS = 3.0;

   private NightfallDragonReachLogic() {
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase != Phase.END || event.player.level().isClientSide) {
         return;
      }

      updateReach(event.player);
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerLoggedOutEvent event) {
      removeReach(event.getEntity());
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      removeReach(event.getEntity());
   }

   private static void updateReach(Player player) {
      if (player.tickCount % 20 == 0) {
         refreshHeldBlade(player);
      }

      AttributeInstance attribute = player.getAttribute(ForgeMod.ENTITY_REACH.get());
      if (attribute == null) {
         return;
      }

      AttributeModifier modifier = attribute.getModifier(ENTITY_REACH_MODIFIER_ID);
      if (NightfallDragonItemSupport.isHoldingNightfallDragon(player)) {
         if (modifier == null) {
            attribute.addTransientModifier(new AttributeModifier(ENTITY_REACH_MODIFIER_ID, "nightfall_dragon_entity_reach", ENTITY_REACH_BONUS, Operation.ADDITION));
         }
      } else if (modifier != null) {
         attribute.removeModifier(ENTITY_REACH_MODIFIER_ID);
      }
   }

   private static void refreshHeldBlade(Player player) {
      if (NightfallDragonItemSupport.isNightfallDragon(player.getMainHandItem())) {
         NightfallDragonDefinitions.ensureStats(player.getMainHandItem());
      }

      if (NightfallDragonItemSupport.isNightfallDragon(player.getOffhandItem())) {
         NightfallDragonDefinitions.ensureStats(player.getOffhandItem());
      }
   }

   private static void removeReach(Player player) {
      AttributeInstance attribute = player.getAttribute(ForgeMod.ENTITY_REACH.get());
      if (attribute != null && attribute.getModifier(ENTITY_REACH_MODIFIER_ID) != null) {
         attribute.removeModifier(ENTITY_REACH_MODIFIER_ID);
      }
   }
}
