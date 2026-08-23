package QWQ.QingYi.annihilationbladeex.nightfall_dragon.logic;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.item.NightfallDragonItemSupport;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "annihilationbladeex")
public final class NightfallDragonReachLogic {
   private static final ResourceLocation REACH_MODIFIER_ID = AnnihilationBladeEX.prefix("nightfall_dragon_entity_reach");
   private static final double ENTITY_REACH_BONUS = 3.0;

   private NightfallDragonReachLogic() {
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      Player player = event.getEntity();
      if (player.level().isClientSide) {
         return;
      }

      updateReach(player);
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

      AttributeInstance attribute = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
      if (attribute == null) {
         return;
      }

      boolean hasModifier = attribute.getModifier(REACH_MODIFIER_ID) != null;
      if (NightfallDragonItemSupport.isHoldingNightfallDragon(player)) {
         if (!hasModifier) {
            attribute.addTransientModifier(new AttributeModifier(REACH_MODIFIER_ID, ENTITY_REACH_BONUS, Operation.ADD_VALUE));
         }
      } else if (hasModifier) {
         attribute.removeModifier(REACH_MODIFIER_ID);
      }
   }

   private static void refreshHeldBlade(Player player) {
      if (NightfallDragonItemSupport.isNightfallDragon(player.getMainHandItem())) {
         NightfallDragonDefinitions.ensureStats(player.getMainHandItem(), player.level());
      }

      if (NightfallDragonItemSupport.isNightfallDragon(player.getOffhandItem())) {
         NightfallDragonDefinitions.ensureStats(player.getOffhandItem(), player.level());
      }
   }

   private static void removeReach(Player player) {
      AttributeInstance attribute = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
      if (attribute != null && attribute.getModifier(REACH_MODIFIER_ID) != null) {
         attribute.removeModifier(REACH_MODIFIER_ID);
      }
   }
}
