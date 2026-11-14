package qwq.qingyi.annihilationblade.client.infinity_stellaris;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.client.render.CustomTooltipDispatcher;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;

/**
 * 无尽星空自定义 Tooltip 调度入口。
 */
public final class InfinityStellarisTooltipEvents {
    private InfinityStellarisTooltipEvents() {
    }

    public static boolean shouldHandle(ItemStack stack) {
        return ModConfig.CLIENT.clientTooltips.enableInfinityStellarisRenderer.getValue()
                && InfinityStellarisItemSupport.isInfinityStellaris(stack);
    }

    public static boolean tryRender(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        if (!shouldHandle(stack)) {
            return false;
        }
        InfinityStellarisTooltipRenderer.render(
                graphics,
                font,
                stack,
                CustomTooltipDispatcher.collectVanillaLines(stack),
                x,
                y
        );
        return true;
    }
}
