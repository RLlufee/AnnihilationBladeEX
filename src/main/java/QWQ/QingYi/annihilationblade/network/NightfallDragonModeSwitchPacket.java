package QWQ.QingYi.annihilationblade.network;

import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public record NightfallDragonModeSwitchPacket() {
   private static final String MESSAGE_TOGGLE = "message.annihilationblade.nightfall_dragon.mode";
   private static final String MESSAGE_SEALED = "message.annihilationblade.nightfall_dragon.mode.sealed";
   private static final String MESSAGE_AWAKENED = "message.annihilationblade.nightfall_dragon.mode.awakened";

   public static void encode(NightfallDragonModeSwitchPacket packet, FriendlyByteBuf buffer) {
   }

   public static NightfallDragonModeSwitchPacket decode(FriendlyByteBuf buffer) {
      return new NightfallDragonModeSwitchPacket();
   }

   public static void handle(NightfallDragonModeSwitchPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player == null || !NightfallDragonItemSupport.isHoldingNightfallDragon(player)) {
            return;
         }

         ItemStack stack = NightfallDragonItemSupport.heldNightfallDragon(player);
         String form = NightfallDragonDefinitions.toggleForm(stack);
         player.displayClientMessage(
            Component.translatable(
               MESSAGE_TOGGLE,
               Component.translatable(NightfallDragonDefinitions.FORM_AWAKENED.equals(form) ? MESSAGE_AWAKENED : MESSAGE_SEALED)
            ),
            true
         );
      });
      ctx.setPacketHandled(true);
   }
}
