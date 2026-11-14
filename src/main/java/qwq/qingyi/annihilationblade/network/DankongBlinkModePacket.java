package qwq.qingyi.annihilationblade.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.Dankong;
import qwq.qingyi.annihilationblade.common.AnnihilationBladeItemSupport;

/**
 * 断空瞬步开关状态同步包（C2S）。
 */
public record DankongBlinkModePacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<DankongBlinkModePacket> TYPE =
            new Type<>(AnnihilationBladeex.prefix("dankong_blink_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DankongBlinkModePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, DankongBlinkModePacket::enabled, DankongBlinkModePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleOnServer(ServerPlayer player) {
        if (AnnihilationBladeItemSupport.isHoldingAnnihilationBlade(player)) {
            Dankong.setBlinkEnabled(player, this.enabled);
        }
    }
}
