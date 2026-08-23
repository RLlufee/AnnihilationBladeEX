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

/**
 * 无尽星空 AI 擦除开关数据包 (1.21.1 NeoForge 规范)
 */
public record InfinityStellarisAiErasurePacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<InfinityStellarisAiErasurePacket> TYPE = new Type<>(AnnihilationBladeEX.prefix("infinity_ai_erasure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfinityStellarisAiErasurePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        InfinityStellarisAiErasurePacket::enabled,
        InfinityStellarisAiErasurePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(InfinityStellarisAiErasurePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
                CurvatureRuptureLogic.setAiErasureEnabled(player, packet.enabled());
            }
        });
    }
}
