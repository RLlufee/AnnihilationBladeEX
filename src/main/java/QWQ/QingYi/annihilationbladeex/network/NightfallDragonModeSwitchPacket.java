package QWQ.QingYi.annihilationbladeex.network;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.item.NightfallDragonItemSupport;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NightfallDragonModeSwitchPacket(String requestedForm) implements CustomPacketPayload {
   public static final Type<NightfallDragonModeSwitchPacket> TYPE = new Type<>(AnnihilationBladeEX.prefix("nightfall_dragon_mode_switch"));
   public static final StreamCodec<RegistryFriendlyByteBuf, NightfallDragonModeSwitchPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      NightfallDragonModeSwitchPacket::requestedForm,
      NightfallDragonModeSwitchPacket::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(NightfallDragonModeSwitchPacket packet, IPayloadContext context) {
      context.enqueueWork(() -> {
         if (context.player() instanceof ServerPlayer player && NightfallDragonItemSupport.isHoldingNightfallDragon(player)) {
            ItemStack stack = NightfallDragonItemSupport.heldNightfallDragon(player);
            if (!stack.isEmpty()) {
               if (packet.requestedForm() != null && !packet.requestedForm().isEmpty()) {
                  NightfallDragonDefinitions.applyForm(stack, packet.requestedForm());
               } else {
                  NightfallDragonDefinitions.toggleForm(stack);
               }
            }
         }
      });
   }
}
