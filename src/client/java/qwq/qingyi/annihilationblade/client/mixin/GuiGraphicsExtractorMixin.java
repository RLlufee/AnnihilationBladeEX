package qwq.qingyi.annihilationblade.client.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qwq.qingyi.annihilationblade.client.render.CustomTooltipDispatcher;
import qwq.qingyi.annihilationblade.client.render.GuiGraphicsExtractorAccess;

/**
 * 拦截原版基于 {@link ItemStack} 的 Tooltip 调度，使模组自定义面板能够接入原版延迟渲染层。
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin implements GuiGraphicsExtractorAccess {
    @Shadow
    @Nullable
    private Runnable deferredTooltip;

    @Override
    public void annihilationbladeex$setDeferredTooltip(Runnable runnable) {
        this.deferredTooltip = runnable;
    }

    @Inject(
            method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void annihilationbladeex$interceptItemTooltip(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (CustomTooltipDispatcher.scheduleTooltip((GuiGraphicsExtractor) (Object) this, font, stack, x, y)) {
            ci.cancel();
        }
    }
}
