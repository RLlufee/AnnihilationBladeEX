package qwq.qingyi.annihilationblade.client.nightfall_dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.client.renderer.special.state.BladeItemRenderState;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import qwq.qingyi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;

/**
 * 永夜魔龙拔刀剑 3D 模型与光效渲染事件重写（适配 SlashBlade-Refabricated 26.1.2 SubmitNodeCollector 管线）。
 */
public final class NightfallDragonRenderEvents {
    private static final int FLOW_FRAMES = 32;
    private static final ThreadLocal<Boolean> RENDERING_EXTRA_LAYER = ThreadLocal.withInitial(() -> false);

    private NightfallDragonRenderEvents() {
    }

    public static void register() {
        RenderOverrideEvent.CALLBACK.register(NightfallDragonRenderEvents::onSlashBladeRenderOverride);
    }

    private static void onSlashBladeRenderOverride(RenderOverrideEvent event) {
        BladeItemRenderState state = event.getRenderState();
        if (state == null) {
            return;
        }
        boolean isNightfall = NightfallDragonDefinitions.BLADE_MODEL.equals(state.modelLocation)
                || NightfallDragonDefinitions.BLADE_TEXTURE.equals(state.textureLocation)
                || NightfallDragonDefinitions.BLADE_TEXTURE.equals(event.getTexture());
        if (!isNightfall) {
            return;
        }

        String target = event.getTarget();
        if (target == null || RENDERING_EXTRA_LAYER.get()) {
            return;
        }

        long time = Util.getMillis();
        String lowerTarget = target.toLowerCase();

        try {
            RENDERING_EXTRA_LAYER.set(true);

            if (lowerTarget.contains("sheath") || lowerTarget.contains("scabbard")) {
                renderEnhancedSheathMotionAndGlow(event, state, lowerTarget, time);
            } else if (lowerTarget.contains("luminous")) {
                renderEnhancedBladeLuminous(event, state, time);
            }
        } finally {
            RENDERING_EXTRA_LAYER.set(false);
        }
    }

    private static void renderEnhancedSheathMotionAndGlow(
            RenderOverrideEvent event,
            BladeItemRenderState state,
            String lowerTarget,
            long time
    ) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        try {
            float pulseScale = 1.0F + Mth.sin((float) (time * 0.004D)) * 0.018F;
            float floatY = Mth.sin((float) (time * 0.003D)) * 0.012F;
            float subtleRotZ = Mth.cos((float) (time * 0.0025D)) * 0.6F;
            float subtleRotX = Mth.sin((float) (time * 0.0035D)) * 0.4F;

            poseStack.translate(0.0D, floatY, 0.0D);
            poseStack.scale(pulseScale, pulseScale, pulseScale);
            poseStack.mulPose(Axis.ZP.rotationDegrees(subtleRotZ));
            poseStack.mulPose(Axis.XP.rotationDegrees(subtleRotX));

            float colorSine = (Mth.sin((float) (time * 0.0035D)) + 1.0F) * 0.5F;
            int r = (int) Mth.lerp(colorSine, 176.0F, 255.0F);
            int g = (int) Mth.lerp(colorSine, 38.0F, 77.0F);
            int b = 255;
            int alpha = 230;
            int primaryColor = (alpha << 24) | (r << 16) | (g << 8) | b;

            if (lowerTarget.contains("luminous") || lowerTarget.contains("effect")) {
                BladeRenderState.setCol(primaryColor, true);
                BladeRenderState.renderOverridedLuminous(
                        state,
                        event.getModel(),
                        event.getTarget(),
                        event.getTexture(),
                        poseStack,
                        event.getSubmitNodeCollector(),
                        BladeRenderState.MAX_LIGHT
                );

                int frame = (int) ((time / 40L) % FLOW_FRAMES);
                BladeRenderState.setCol(0xFFCC3AFF, true);
                BladeRenderState.renderChargeEffect(
                        state,
                        frame,
                        event.getModel(),
                        event.getTarget(),
                        event.getTexture(),
                        poseStack,
                        event.getSubmitNodeCollector(),
                        BladeRenderState.MAX_LIGHT
                );

                BladeRenderState.setCol(0xAA6A00FF, true);
                BladeRenderState.renderOverridedReverseLuminous(
                        state,
                        event.getModel(),
                        event.getTarget(),
                        event.getTexture(),
                        poseStack,
                        event.getSubmitNodeCollector(),
                        BladeRenderState.MAX_LIGHT
                );
            } else {
                int subFrame = (int) ((time / 60L) % FLOW_FRAMES);
                BladeRenderState.setCol(0x80B026FF, true);
                BladeRenderState.renderChargeEffect(
                        state,
                        subFrame,
                        event.getModel(),
                        event.getTarget(),
                        event.getTexture(),
                        poseStack,
                        event.getSubmitNodeCollector(),
                        BladeRenderState.MAX_LIGHT
                );
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static void renderEnhancedBladeLuminous(
            RenderOverrideEvent event,
            BladeItemRenderState state,
            long time
    ) {
        int frame = (int) ((time / 50L) % FLOW_FRAMES);
        float pulse = (Mth.sin((float) (time * 0.005D)) + 1.0F) * 0.5F;
        int r = (int) Mth.lerp(pulse, 200.0F, 255.0F);
        int g = (int) Mth.lerp(pulse, 50.0F, 120.0F);
        int b = 255;
        int glowColor = (240 << 24) | (r << 16) | (g << 8) | b;

        BladeRenderState.setCol(glowColor, true);
        BladeRenderState.renderChargeEffect(
                state,
                frame,
                event.getModel(),
                event.getTarget(),
                event.getTexture(),
                event.getPoseStack(),
                event.getSubmitNodeCollector(),
                BladeRenderState.MAX_LIGHT
        );
    }
}
