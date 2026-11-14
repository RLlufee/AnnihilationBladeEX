package qwq.qingyi.annihilationblade.client.nightfall_dragon;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.client.render.CustomTooltipDispatcher;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;

/**
 * 永夜魔龙自定义 Tooltip 调度入口。
 */
public final class NightfallDragonTooltipEvents {
    private NightfallDragonTooltipEvents() {
    }

    public static boolean shouldHandle(ItemStack stack) {
        return ModConfig.CLIENT.clientTooltips.enableNightfallDragonRenderer.getValue()
                && NightfallDragonDefinitions.isNightfallDragon(stack);
    }

    public static boolean tryRender(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        if (!shouldHandle(stack)) {
            return false;
        }
        NightfallDragonTooltipRenderer.render(
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
