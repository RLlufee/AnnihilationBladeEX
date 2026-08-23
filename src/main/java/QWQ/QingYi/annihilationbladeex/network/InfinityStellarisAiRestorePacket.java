package QWQ.QingYi.annihilationbladeex.network;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.item.InfinityStellarisItemSupport;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.logic.CurvatureRuptureLogic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record InfinityStellarisAiRestorePacket(boolean enabled) implements CustomPacketPayload {
   public static final Type<InfinityStellarisAiRestorePacket> TYPE = new Type<>(AnnihilationBladeEX.prefix("infinity_ai_restore"));
   public static final StreamCodec<RegistryFriendlyByteBuf, InfinityStellarisAiRestorePacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      InfinityStellarisAiRestorePacket::enabled,
      InfinityStellarisAiRestorePacket::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(InfinityStellarisAiRestorePacket packet, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player && InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
            CurvatureRuptureLogic.setAiRestoreEnabled(player, packet.enabled());
         }
      });
   }
}
