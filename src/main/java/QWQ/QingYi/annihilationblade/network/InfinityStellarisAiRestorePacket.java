package QWQ.QingYi.annihilationblade.network;

import QWQ.QingYi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;
import QWQ.QingYi.annihilationblade.infinity_stellaris.logic.CurvatureRuptureLogic;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record InfinityStellarisAiRestorePacket(boolean enabled) {
   public static void encode(InfinityStellarisAiRestorePacket packet, FriendlyByteBuf buffer) {
      buffer.writeBoolean(packet.enabled);
   }

   public static InfinityStellarisAiRestorePacket decode(FriendlyByteBuf buffer) {
      return new InfinityStellarisAiRestorePacket(buffer.readBoolean());
   }

   public static void handle(InfinityStellarisAiRestorePacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null && InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
            CurvatureRuptureLogic.setAiRestoreEnabled(player, packet.enabled);
         }
      });
      ctx.setPacketHandled(true);
   }
}
