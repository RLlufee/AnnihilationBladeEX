package qwq.qingyi.annihilationblade.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.CurvatureRuptureLogic;

/**
 * 无尽星空 AI 恢复开关数据包（C2S）。
 */
public record InfinityStellarisAiRestorePacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<InfinityStellarisAiRestorePacket> TYPE =
            new Type<>(AnnihilationBladeex.prefix("infinity_ai_restore"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfinityStellarisAiRestorePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, InfinityStellarisAiRestorePacket::enabled, InfinityStellarisAiRestorePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleOnServer(ServerPlayer player) {
        if (InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
            CurvatureRuptureLogic.setAiRestoreEnabled(player, this.enabled);
        }
    }
}
