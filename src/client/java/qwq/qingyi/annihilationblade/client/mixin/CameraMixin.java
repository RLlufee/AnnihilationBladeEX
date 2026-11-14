package qwq.qingyi.annihilationblade.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qwq.qingyi.annihilationblade.client.nightfall_dragon.NightfallDragonClientRuntimeEvents;

/**
 * 相机震动注入，在 alignWithEntity 结束后叠加夜龙震动偏移与翻滚角。
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    @Final
    private static Vector3f FORWARDS;

    @Shadow
    @Final
    private static Vector3f UP;

    @Shadow
    @Final
    private static Vector3f LEFT;

    @Shadow
    @Final
    private Vector3f forwards;

    @Shadow
    @Final
    private Vector3f up;

    @Shadow
    @Final
    private Vector3f left;

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity(F)V", at = @At("TAIL"))
    private void annihilationbladeex$applyNightfallCameraShake(float partialTick, CallbackInfo ci) {
        NightfallDragonClientRuntimeEvents.ShakeOffset offset = NightfallDragonClientRuntimeEvents.sampleCameraShake(partialTick);
        if (offset == null) {
            return;
        }
        this.setRotation(this.yRot + offset.yawDelta(), this.xRot + offset.pitchDelta());
        if (Math.abs(offset.rollDelta()) > 1.0E-4F) {
            this.rotation.rotateZ(offset.rollDelta() * Mth.DEG_TO_RAD);
            FORWARDS.rotate(this.rotation, this.forwards);
            UP.rotate(this.rotation, this.up);
            LEFT.rotate(this.rotation, this.left);
        }
    }
}
