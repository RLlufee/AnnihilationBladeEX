package QWQ.QingYi.annihilationblade.network;

import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

public record NightfallDragonModeSwitchPacket() {
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
         NightfallDragonDefinitions.toggleForm(stack);
      });
      ctx.setPacketHandled(true);
   }
}
