package qwq.qingyi.annihilationblade.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import qwq.qingyi.annihilationblade.client.blood_prison.BloodPrisonClientEffects;
import qwq.qingyi.annihilationblade.network.BloodPrisonDomainPacket;
import qwq.qingyi.annihilationblade.network.DankongBlinkModePacket;
import qwq.qingyi.annihilationblade.network.InfinityStellarisAiErasurePacket;
import qwq.qingyi.annihilationblade.network.InfinityStellarisAiRestorePacket;
import qwq.qingyi.annihilationblade.network.NightfallDragonModeSwitchPacket;

/**
 * 客户端网络收发入口：注册 S2C 数据包处理器并向服务端发送控制请求。
 */
public final class ModClientNetwork {
    private ModClientNetwork() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(BloodPrisonDomainPacket.TYPE, (payload, context) ->
                context.client().execute(() -> BloodPrisonClientEffects.setDomainTicks(payload.ticks())));
    }

    public static void sendDankongBlinkMode(boolean enabled) {
        if (ClientPlayNetworking.canSend(DankongBlinkModePacket.TYPE)) {
            ClientPlayNetworking.send(new DankongBlinkModePacket(enabled));
        }
    }

    public static void sendInfinityStellarisAiErasure(boolean enabled) {
        if (ClientPlayNetworking.canSend(InfinityStellarisAiErasurePacket.TYPE)) {
            ClientPlayNetworking.send(new InfinityStellarisAiErasurePacket(enabled));
        }
    }

    public static void sendInfinityStellarisAiRestore(boolean enabled) {
        if (ClientPlayNetworking.canSend(InfinityStellarisAiRestorePacket.TYPE)) {
            ClientPlayNetworking.send(new InfinityStellarisAiRestorePacket(enabled));
        }
    }

    public static void sendNightfallDragonModeSwitch(String requestedForm) {
        if (ClientPlayNetworking.canSend(NightfallDragonModeSwitchPacket.TYPE)) {
            ClientPlayNetworking.send(new NightfallDragonModeSwitchPacket(requestedForm));
        }
    }
}
