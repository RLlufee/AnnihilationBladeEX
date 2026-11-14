package qwq.qingyi.annihilationblade.client.annihilation_blade;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.client.render.CustomTooltipDispatcher;
import qwq.qingyi.annihilationblade.config.ModConfig;

/**
 * 湮灭之刃自定义 Tooltip 调度入口。
 */
public final class TerminusTooltipEvents {
    private TerminusTooltipEvents() {
    }

    public static boolean shouldHandle(ItemStack stack) {
        return ModConfig.CLIENT.clientTooltips.enableAnnihilationBladeRenderer.getValue()
                && TerminusTooltipRenderer.shouldRender(stack);
    }

    public static boolean tryRender(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        if (!shouldHandle(stack)) {
            return false;
        }
        TerminusTooltipRenderer.render(graphics, font, stack, CustomTooltipDispatcher.collectVanillaLines(stack), x, y);
        return true;
    }
}
