package QWQ.QingYi.annihilationbladeex.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(BloodPrisonDomainPacket.TYPE, BloodPrisonDomainPacket.STREAM_CODEC, BloodPrisonDomainPacket::handle)
                .playToServer(DankongBlinkModePacket.TYPE, DankongBlinkModePacket.STREAM_CODEC, DankongBlinkModePacket::handle)
                .playToServer(InfinityStellarisAiErasurePacket.TYPE, InfinityStellarisAiErasurePacket.STREAM_CODEC, InfinityStellarisAiErasurePacket::handle)
                .playToServer(InfinityStellarisAiRestorePacket.TYPE, InfinityStellarisAiRestorePacket.STREAM_CODEC, InfinityStellarisAiRestorePacket::handle)
                .playToServer(NightfallDragonModeSwitchPacket.TYPE, NightfallDragonModeSwitchPacket.STREAM_CODEC, NightfallDragonModeSwitchPacket::handle);
    }

    public static void sendBloodPrisonDomain(ServerPlayer player, int ticks) {
        PacketDistributor.sendToPlayer(player, new BloodPrisonDomainPacket(ticks));
    }

    public static void sendInfinityStellarisAiErasure(boolean enabled) {
        PacketDistributor.sendToServer(new InfinityStellarisAiErasurePacket(enabled));
    }

    public static void sendInfinityStellarisAiRestore(boolean enabled) {
        PacketDistributor.sendToServer(new InfinityStellarisAiRestorePacket(enabled));
    }

    public static void sendNightfallDragonModeSwitch(String requestedForm) {
        PacketDistributor.sendToServer(new NightfallDragonModeSwitchPacket(requestedForm == null ? "" : requestedForm));
    }
}
