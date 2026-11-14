package qwq.qingyi.annihilationblade.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;

/**
 * 血狱领域剩余刻数同步包（S2C），客户端接收处理器位于 {@code ModClientNetwork}。
 */
public record BloodPrisonDomainPacket(int ticks) implements CustomPacketPayload {
    public static final Type<BloodPrisonDomainPacket> TYPE =
            new Type<>(AnnihilationBladeex.prefix("blood_prison_domain"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BloodPrisonDomainPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, BloodPrisonDomainPacket::ticks, BloodPrisonDomainPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
