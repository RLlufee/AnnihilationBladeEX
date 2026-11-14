package qwq.qingyi.annihilationblade.client.infinity_stellaris;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import qwq.qingyi.annihilationblade.client.AnnihilationBladeexClient;
import qwq.qingyi.annihilationblade.client.network.ModClientNetwork;
import qwq.qingyi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;

/**
 * 无尽星空 AI 擦除与复原开关按键处理。
 */
public final class InfinityStellarisAiErasureKeyHandler {
    private static final String KEY_NAME = "key.annihilationbladeex.toggle_infinity_ai_erasure";
    private static final String KEY_RESTORE_NAME = "key.annihilationbladeex.toggle_infinity_ai_restore";
    private static final String MESSAGE_TOGGLE = "message.annihilationbladeex.infinity_ai_erasure_toggle";
    private static final String MESSAGE_RESTORE_TOGGLE = "message.annihilationbladeex.infinity_ai_restore_toggle";
    private static final String MESSAGE_RESTORE_WARNING = "message.annihilationbladeex.infinity_ai_restore_warning";
    private static final String MESSAGE_STATE_ON = "message.annihilationbladeex.infinity_ai_erasure.on";
    private static final String MESSAGE_STATE_OFF = "message.annihilationbladeex.infinity_ai_erasure.off";

    private static final KeyMapping TOGGLE_AI_ERASURE = new KeyMapping(
            KEY_NAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            AnnihilationBladeexClient.KEY_CATEGORY
    );
    private static final KeyMapping TOGGLE_AI_RESTORE = new KeyMapping(
            KEY_RESTORE_NAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            AnnihilationBladeexClient.KEY_CATEGORY
    );
    private static boolean aiErasureEnabled;
    private static boolean aiRestoreEnabled = true;

    private InfinityStellarisAiErasureKeyHandler() {
    }

    public static void register() {
        KeyMappingHelper.registerKeyMapping(TOGGLE_AI_ERASURE);
        KeyMappingHelper.registerKeyMapping(TOGGLE_AI_RESTORE);
        ClientTickEvents.END_CLIENT_TICK.register(InfinityStellarisAiErasureKeyHandler::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, minecraft) -> {
            aiErasureEnabled = false;
            aiRestoreEnabled = true;
        });
    }

    private static void onClientTick(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        if (!InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
            while (TOGGLE_AI_ERASURE.consumeClick()) {
            }
            while (TOGGLE_AI_RESTORE.consumeClick()) {
            }
            aiErasureEnabled = false;
            return;
        }

        while (TOGGLE_AI_ERASURE.consumeClick()) {
            aiErasureEnabled = !aiErasureEnabled;
            ModClientNetwork.sendInfinityStellarisAiErasure(aiErasureEnabled);
            player.sendOverlayMessage(Component.translatable(
                    MESSAGE_TOGGLE,
                    Component.translatable(aiErasureEnabled ? MESSAGE_STATE_ON : MESSAGE_STATE_OFF)
            ));
        }

        while (TOGGLE_AI_RESTORE.consumeClick()) {
            aiRestoreEnabled = !aiRestoreEnabled;
            ModClientNetwork.sendInfinityStellarisAiRestore(aiRestoreEnabled);
            player.sendOverlayMessage(
                    Component.translatable(
                                    MESSAGE_RESTORE_TOGGLE,
                                    Component.translatable(aiRestoreEnabled ? MESSAGE_STATE_ON : MESSAGE_STATE_OFF)
                            )
                            .append(Component.literal("  "))
                            .append(Component.translatable(MESSAGE_RESTORE_WARNING))
            );
        }
    }
}
