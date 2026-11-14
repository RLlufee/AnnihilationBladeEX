package qwq.qingyi.annihilationblade.client.blood_prison;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;

/**
 * 血狱领域全屏红色滤镜客户端计时与 HUD 叠加绘制逻辑。
 */
public final class BloodPrisonClientEffects {
    private static final Identifier RED_OVERLAY = AnnihilationBladeex.prefix("textures/screens/red.png");
    private static int domainTicks;
    private static int domainAgeTicks;

    private BloodPrisonClientEffects() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (minecraft.isPaused()) {
                return;
            }
            if (domainTicks > 0) {
                domainTicks--;
                domainAgeTicks++;
            } else {
                domainAgeTicks = 0;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, minecraft) -> {
            domainTicks = 0;
            domainAgeTicks = 0;
        });
    }

    public static void setDomainTicks(int ticks) {
        domainTicks = Math.max(0, ticks);
        domainAgeTicks = 0;
    }

    public static void renderOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (domainTicks <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        float enter = Math.min(1.0F, (domainAgeTicks + partialTick) / 10.0F);
        float exit = Math.min(1.0F, (domainTicks + partialTick) / 26.0F);
        float fade = Math.min(enter, exit);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                RED_OVERLAY,
                0,
                0,
                0.0F,
                0.0F,
                screenWidth,
                screenHeight,
                16,
                16,
                16,
                16,
                ARGB.white(0.92F * fade)
        );
    }
}
