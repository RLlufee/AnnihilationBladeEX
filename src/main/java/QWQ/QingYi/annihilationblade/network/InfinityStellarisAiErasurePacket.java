package QWQ.QingYi.annihilationblade.network;

import QWQ.QingYi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;
import QWQ.QingYi.annihilationblade.infinity_stellaris.logic.CurvatureRuptureLogic;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record InfinityStellarisAiErasurePacket(boolean enabled) {
   public static void encode(InfinityStellarisAiErasurePacket packet, FriendlyByteBuf buffer) {
      buffer.writeBoolean(packet.enabled);
   }

   public static InfinityStellarisAiErasurePacket decode(FriendlyByteBuf buffer) {
      return new InfinityStellarisAiErasurePacket(buffer.readBoolean());
   }

   public static void handle(InfinityStellarisAiErasurePacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null && InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
            CurvatureRuptureLogic.setAiErasureEnabled(player, packet.enabled);
         }
      });
      ctx.setPacketHandled(true);
   }
}
