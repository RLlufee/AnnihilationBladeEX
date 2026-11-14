package qwq.qingyi.annihilationblade.client.nightfall_dragon;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import qwq.qingyi.annihilationblade.client.AnnihilationBladeexClient;
import qwq.qingyi.annihilationblade.client.network.ModClientNetwork;
import qwq.qingyi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;

/**
 * 永夜魔龙形态切换按键处理。
 */
public final class NightfallDragonModeKeyHandler {
    private static final String KEY_NAME = "key.annihilationbladeex.switch_nightfall_dragon_mode";
    private static final KeyMapping SWITCH_MODE = new KeyMapping(
            KEY_NAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            AnnihilationBladeexClient.KEY_CATEGORY
    );

    private NightfallDragonModeKeyHandler() {
    }

    public static void register() {
        KeyMappingHelper.registerKeyMapping(SWITCH_MODE);
        ClientTickEvents.END_CLIENT_TICK.register(NightfallDragonModeKeyHandler::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        while (SWITCH_MODE.consumeClick()) {
            if (NightfallDragonItemSupport.isHoldingNightfallDragon(player)) {
                ModClientNetwork.sendNightfallDragonModeSwitch("");
            }
        }
    }
}
