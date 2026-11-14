package qwq.qingyi.annihilationblade.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qwq.qingyi.annihilationblade.client.render.CustomTooltipDispatcher;

/**
 * 拦截容器与创造模式物品栏中的悬停 Tooltip，交由模组自定义 Tooltip 渲染器绘制。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    protected Slot hoveredSlot;

    @Inject(
            method = "extractTooltip(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/Identifier;)V"
            ),
            cancellable = true
    )
    private void annihilationbladeex$interceptContainerTooltip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            if (CustomTooltipDispatcher.scheduleTooltip(graphics, Minecraft.getInstance().font, stack, mouseX, mouseY)) {
                ci.cancel();
            }
        }
    }
}
