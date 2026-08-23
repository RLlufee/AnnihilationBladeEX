package QWQ.QingYi.annihilationbladeex.nightfall_dragon.client;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.network.ModNetwork;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.item.NightfallDragonItemSupport;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = AnnihilationBladeEX.MODID, value = Dist.CLIENT)
public final class NightfallDragonModeKeyHandler {
   private static final String KEY_NAME = "key.annihilationbladeex.switch_nightfall_dragon_mode";
   private static final String KEY_CATEGORY = "key.categories.annihilationbladeex";
   private static final KeyMapping SWITCH_MODE = new KeyMapping(
      KEY_NAME, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KEY_CATEGORY
   );

   private NightfallDragonModeKeyHandler() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      Minecraft minecraft = Minecraft.getInstance();
      Player player = minecraft.player;
      if (player == null) {
         return;
      }

      while (SWITCH_MODE.consumeClick()) {
         if (NightfallDragonItemSupport.isHoldingNightfallDragon(player)) {
            ModNetwork.sendNightfallDragonModeSwitch("");
         }
      }
   }

   @EventBusSubscriber(modid = AnnihilationBladeEX.MODID, bus = Bus.MOD, value = Dist.CLIENT)
   public static final class ModBusEvents {
      private ModBusEvents() {
      }

      @SubscribeEvent
      public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
         event.register(SWITCH_MODE);
      }
   }
}
