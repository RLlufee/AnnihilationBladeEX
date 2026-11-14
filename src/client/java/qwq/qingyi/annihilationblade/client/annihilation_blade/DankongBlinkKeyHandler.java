package qwq.qingyi.annihilationblade.client.annihilation_blade;

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
import qwq.qingyi.annihilationblade.common.AnnihilationBladeItemSupport;

/**
 * 斩空瞬移模式切换按键处理。
 */
public final class DankongBlinkKeyHandler {
    private static final String KEY_NAME = "key.annihilationbladeex.toggle_dankong_blink";
    private static final String MESSAGE_TOGGLE = "message.annihilationbladeex.dankong_blink_toggle";
    private static final String MESSAGE_STATE_ON = "message.annihilationbladeex.dankong_blink.on";
    private static final String MESSAGE_STATE_OFF = "message.annihilationbladeex.dankong_blink.off";

    private static final KeyMapping TOGGLE_DANKONG_BLINK = new KeyMapping(
            KEY_NAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            AnnihilationBladeexClient.KEY_CATEGORY
    );
    private static boolean blinkEnabled = true;

    private DankongBlinkKeyHandler() {
    }

    public static void register() {
        KeyMappingHelper.registerKeyMapping(TOGGLE_DANKONG_BLINK);
        ClientTickEvents.END_CLIENT_TICK.register(DankongBlinkKeyHandler::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, minecraft) -> resetBlinkMode());
    }

    private static void onClientTick(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        while (consumeToggleClick()) {
            if (!AnnihilationBladeItemSupport.isHoldingAnnihilationBlade(player)) {
                continue;
            }

            blinkEnabled = !blinkEnabled;
            ModClientNetwork.sendDankongBlinkMode(blinkEnabled);
            player.sendOverlayMessage(Component.translatable(
                    MESSAGE_TOGGLE,
                    Component.translatable(blinkEnabled ? MESSAGE_STATE_ON : MESSAGE_STATE_OFF)
            ));
        }
    }

    public static boolean consumeToggleClick() {
        return TOGGLE_DANKONG_BLINK.consumeClick();
    }

    public static void resetBlinkMode() {
        blinkEnabled = true;
    }
}
