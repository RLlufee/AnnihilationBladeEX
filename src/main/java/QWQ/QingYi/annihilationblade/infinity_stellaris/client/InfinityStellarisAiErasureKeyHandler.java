package QWQ.QingYi.annihilationblade.infinity_stellaris.client;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;
import QWQ.QingYi.annihilationblade.network.InfinityStellarisAiErasurePacket;
import QWQ.QingYi.annihilationblade.network.InfinityStellarisAiRestorePacket;
import QWQ.QingYi.annihilationblade.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Annihilationblade.MODID, value = Dist.CLIENT)
public final class InfinityStellarisAiErasureKeyHandler {
   private static final String KEY_NAME = "key.annihilationblade.toggle_infinity_ai_erasure";
   private static final String KEY_RESTORE_NAME = "key.annihilationblade.toggle_infinity_ai_restore";
   private static final String KEY_CATEGORY = "key.categories.annihilationblade";
   private static final String MESSAGE_TOGGLE = "message.annihilationblade.infinity_ai_erasure_toggle";
   private static final String MESSAGE_RESTORE_TOGGLE = "message.annihilationblade.infinity_ai_restore_toggle";
   private static final String MESSAGE_RESTORE_WARNING = "message.annihilationblade.infinity_ai_restore_warning";
   private static final String MESSAGE_STATE_ON = "message.annihilationblade.infinity_ai_erasure.on";
   private static final String MESSAGE_STATE_OFF = "message.annihilationblade.infinity_ai_erasure.off";
   private static final KeyMapping TOGGLE_AI_ERASURE = new KeyMapping(
      KEY_NAME, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, KEY_CATEGORY
   );
   private static final KeyMapping TOGGLE_AI_RESTORE = new KeyMapping(
      KEY_RESTORE_NAME, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, KEY_CATEGORY
   );
   private static boolean aiErasureEnabled;
   private static boolean aiRestoreEnabled = true;

   private InfinityStellarisAiErasureKeyHandler() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase != Phase.END) {
         return;
      }

      Minecraft minecraft = Minecraft.getInstance();
      Player player = minecraft.player;
      if (player == null) {
         return;
      }

      if (!InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
         while (TOGGLE_AI_ERASURE.consumeClick()) {
         }

         while (TOGGLE_AI_RESTORE.consumeClick()) {
         }

         aiErasureEnabled = false;
         return;
      }

      while (TOGGLE_AI_ERASURE.consumeClick()) {
         aiErasureEnabled = !aiErasureEnabled;
         ModNetwork.CHANNEL.sendToServer(new InfinityStellarisAiErasurePacket(aiErasureEnabled));
         player.displayClientMessage(Component.translatable(MESSAGE_TOGGLE, Component.translatable(aiErasureEnabled ? MESSAGE_STATE_ON : MESSAGE_STATE_OFF)), true);
      }

      while (TOGGLE_AI_RESTORE.consumeClick()) {
         aiRestoreEnabled = !aiRestoreEnabled;
         ModNetwork.CHANNEL.sendToServer(new InfinityStellarisAiRestorePacket(aiRestoreEnabled));
         player.displayClientMessage(
            Component.translatable(MESSAGE_RESTORE_TOGGLE, Component.translatable(aiRestoreEnabled ? MESSAGE_STATE_ON : MESSAGE_STATE_OFF))
               .append(Component.literal("  "))
               .append(Component.translatable(MESSAGE_RESTORE_WARNING)),
            true
         );
      }
   }

   @SubscribeEvent
   public static void onLoggingOut(LoggingOut event) {
      aiErasureEnabled = false;
      aiRestoreEnabled = true;
   }

   @EventBusSubscriber(modid = Annihilationblade.MODID, bus = Bus.MOD, value = Dist.CLIENT)
   public static final class ModBusEvents {
      private ModBusEvents() {
      }

      @SubscribeEvent
      public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
         event.register(TOGGLE_AI_ERASURE);
         event.register(TOGGLE_AI_RESTORE);
      }
   }
}
