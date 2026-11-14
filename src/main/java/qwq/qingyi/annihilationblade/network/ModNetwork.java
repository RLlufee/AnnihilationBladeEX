package qwq.qingyi.annihilationblade.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * 模组双端网络通道注册与服务端发送入口。
 * 客户端发送与 S2C 接收器位于 {@code client.ModClientNetwork}，避免在服务端加载客户端类。
 */
public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(BloodPrisonDomainPacket.TYPE, BloodPrisonDomainPacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DankongBlinkModePacket.TYPE, DankongBlinkModePacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InfinityStellarisAiErasurePacket.TYPE, InfinityStellarisAiErasurePacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InfinityStellarisAiRestorePacket.TYPE, InfinityStellarisAiRestorePacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NightfallDragonModeSwitchPacket.TYPE, NightfallDragonModeSwitchPacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DankongBlinkModePacket.TYPE, (payload, context) ->
                context.server().execute(() -> payload.handleOnServer(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(InfinityStellarisAiErasurePacket.TYPE, (payload, context) ->
                context.server().execute(() -> payload.handleOnServer(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(InfinityStellarisAiRestorePacket.TYPE, (payload, context) ->
                context.server().execute(() -> payload.handleOnServer(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(NightfallDragonModeSwitchPacket.TYPE, (payload, context) ->
                context.server().execute(() -> payload.handleOnServer(context.player())));
    }

    public static void sendBloodPrisonDomain(ServerPlayer player, int ticks) {
        ServerPlayNetworking.send(player, new BloodPrisonDomainPacket(ticks));
    }
}
