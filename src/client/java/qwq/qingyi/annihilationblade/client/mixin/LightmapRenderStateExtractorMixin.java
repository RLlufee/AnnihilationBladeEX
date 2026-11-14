package qwq.qingyi.annihilationblade.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qwq.qingyi.annihilationblade.client.annihilation_blade.ClientBladeVision;

/**
 * 在局部光照状态提取阶段为持有湮灭之刃（或星穹无界）的玩家提供夜视级亮度，
 * 避免向实体写入真实药水效果而弹出状态图标。
 */
@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V"
            )
    )
    private void annihilationbladeex$applyBladeNightVision(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        if (this.minecraft.player != null && ClientBladeVision.hasBladeInInventory()) {
            state.nightVisionEffectIntensity = Math.max(state.nightVisionEffectIntensity, 1.0F);
        }
    }
}
