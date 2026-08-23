package QWQ.QingYi.annihilationbladeex.infinity_stellaris.client;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.item.InfinityStellarisItemSupport;
import QWQ.QingYi.annihilationbladeex.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 无尽星空 AI 擦除按键处理类 (1.21.1 NeoForge)
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = AnnihilationBladeEX.MODID, value = Dist.CLIENT)
public final class InfinityStellarisAiErasureKeyHandler {
    private static final String KEY_NAME = "key.annihilationbladeex.toggle_infinity_ai_erasure";
    private static final String KEY_RESTORE_NAME = "key.annihilationbladeex.toggle_infinity_ai_restore";
    private static final String KEY_CATEGORY = "key.categories.annihilationbladeex";
    private static final String MESSAGE_TOGGLE = "message.annihilationbladeex.infinity_ai_erasure_toggle";
    private static final String MESSAGE_RESTORE_TOGGLE = "message.annihilationbladeex.infinity_ai_restore_toggle";
    private static final String MESSAGE_RESTORE_WARNING = "message.annihilationbladeex.infinity_ai_restore_warning";
    private static final String MESSAGE_STATE_ON = "message.annihilationbladeex.infinity_ai_erasure.on";
    private static final String MESSAGE_STATE_OFF = "message.annihilationbladeex.infinity_ai_erasure.off";

    private static final KeyMapping TOGGLE_AI_ERASURE = new KeyMapping(
        KEY_NAME, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, KEY_CATEGORY
    );
    private static final KeyMapping TOGGLE_AI_RESTORE = new KeyMapping(
        KEY_RESTORE_NAME, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, KEY_CATEGORY
    );
    private static boolean aiErasureEnabled;
    private static boolean aiRestoreEnabled = true;

    private InfinityStellarisAiErasureKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
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
            ModNetwork.sendInfinityStellarisAiErasure(aiErasureEnabled);
            player.displayClientMessage(
                Component.translatable(MESSAGE_TOGGLE, Component.translatable(aiErasureEnabled ? MESSAGE_STATE_ON : MESSAGE_STATE_OFF)),
                true
            );
        }

        while (TOGGLE_AI_RESTORE.consumeClick()) {
            aiRestoreEnabled = !aiRestoreEnabled;
            ModNetwork.sendInfinityStellarisAiRestore(aiRestoreEnabled);
            player.displayClientMessage(
                Component.translatable(MESSAGE_RESTORE_TOGGLE, Component.translatable(aiRestoreEnabled ? MESSAGE_STATE_ON : MESSAGE_STATE_OFF))
                    .append(Component.literal("  "))
                    .append(Component.translatable(MESSAGE_RESTORE_WARNING)),
                true
            );
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        aiErasureEnabled = false;
        aiRestoreEnabled = true;
    }

    @EventBusSubscriber(modid = AnnihilationBladeEX.MODID, bus = Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_AI_ERASURE);
            event.register(TOGGLE_AI_RESTORE);
        }
    }
}
