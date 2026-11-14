package qwq.qingyi.annihilationblade.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import qwq.qingyi.annihilationblade.client.annihilation_blade.TerminusTooltipEvents;
import qwq.qingyi.annihilationblade.client.infinity_stellaris.InfinityStellarisTooltipEvents;
import qwq.qingyi.annihilationblade.client.nightfall_dragon.NightfallDragonTooltipEvents;

import java.util.List;

/**
 * 统一调度模组自定义 Tooltip 渲染器，并在延迟渲染阶段完成绘制。
 */
public final class CustomTooltipDispatcher {
    private CustomTooltipDispatcher() {
    }

    public static boolean scheduleTooltip(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        if (stack.isEmpty() || !shouldIntercept(stack)) {
            return false;
        }

        ItemStack snapshot = stack.copy();
        ((GuiGraphicsExtractorAccess) graphics).annihilationbladeex$setDeferredTooltip(() -> {
            graphics.nextStratum();
            render(graphics, font, snapshot, x, y);
        });
        return true;
    }

    public static List<Component> collectVanillaLines(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        TooltipFlag tooltipFlag = minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
        Item.TooltipContext context = minecraft.level == null
                ? Item.TooltipContext.EMPTY
                : Item.TooltipContext.of(minecraft.level);
        return stack.getTooltipLines(context, minecraft.player, tooltipFlag);
    }

    private static boolean shouldIntercept(ItemStack stack) {
        return InfinityStellarisTooltipEvents.shouldHandle(stack)
                || NightfallDragonTooltipEvents.shouldHandle(stack)
                || TerminusTooltipEvents.shouldHandle(stack);
    }

    private static void render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        if (InfinityStellarisTooltipEvents.tryRender(graphics, font, stack, x, y)) {
            return;
        }
        if (NightfallDragonTooltipEvents.tryRender(graphics, font, stack, x, y)) {
            return;
        }
        TerminusTooltipEvents.tryRender(graphics, font, stack, x, y);
    }
}
