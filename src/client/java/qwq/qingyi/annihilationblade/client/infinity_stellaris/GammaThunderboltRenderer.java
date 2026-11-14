package qwq.qingyi.annihilationblade.client.infinity_stellaris;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4fc;
import qwq.qingyi.annihilationblade.infinity_stellaris.entity.GammaThunderboltEntity;

/**
 * 伽马天雷实体渲染器（适配 MC 26.1.2 EntityRenderState + SubmitNodeCollector 管线）。
 */
public class GammaThunderboltRenderer extends EntityRenderer<GammaThunderboltEntity, GammaThunderboltRenderState> {
    public GammaThunderboltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public GammaThunderboltRenderState createRenderState() {
        return new GammaThunderboltRenderState();
    }

    @Override
    public void extractRenderState(GammaThunderboltEntity bolt, GammaThunderboltRenderState state, float partialTick) {
        super.extractRenderState(bolt, state, partialTick);
        state.seed = bolt.seed;
        state.fixedColor = bolt.getFixedColor();
    }

    @Override
    protected boolean affectedByCulling(GammaThunderboltEntity entity) {
        return false;
    }

    @Override
    public void submit(
            GammaThunderboltRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        float[] xOffsets = new float[8];
        float[] zOffsets = new float[8];
        float endX = 0.0F;
        float endZ = 0.0F;
        RandomSource offsetRandom = RandomSource.create(state.seed);

        for (int index = 7; index >= 0; --index) {
            xOffsets[index] = endX;
            zOffsets[index] = endZ;
            endX += (float) (offsetRandom.nextInt(11) - 5);
            endZ += (float) (offsetRandom.nextInt(11) - 5);
        }

        final float anchorX = endX;
        final float anchorZ = endZ;
        final long boltSeed = state.seed;
        final int fixedColor = state.fixedColor;

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, buffer) -> {
            Matrix4fc matrix = pose.pose();
            RandomSource colorRandom = RandomSource.create(boltSeed ^ 0x6D9F_2B31_5A77L);
            for (int layer = 0; layer < 4; ++layer) {
                RandomSource branchRandom = RandomSource.create(boltSeed);
                for (int branch = 0; branch < 3; ++branch) {
                    int start = 7;
                    int end = 0;
                    if (branch > 0) {
                        start = 7 - branch;
                        end = start - 2;
                    }

                    float currentX = xOffsets[start] - anchorX;
                    float currentZ = zOffsets[start] - anchorZ;
                    for (int segment = start; segment >= end; --segment) {
                        float previousX = currentX;
                        float previousZ = currentZ;
                        if (branch == 0) {
                            currentX += (float) (branchRandom.nextInt(11) - 5);
                            currentZ += (float) (branchRandom.nextInt(11) - 5);
                        } else {
                            currentX += (float) (branchRandom.nextInt(31) - 15);
                            currentZ += (float) (branchRandom.nextInt(31) - 15);
                        }

                        float inner = 0.1F + layer * 0.2F;
                        if (branch == 0) {
                            inner *= segment * 0.1F + 1.0F;
                        }

                        float outer = 0.1F + layer * 0.2F;
                        if (branch == 0) {
                            outer *= (segment - 1.0F) * 0.1F + 1.0F;
                        }

                        float red = fixedColor >= 0 ? ((fixedColor >> 16) & 0xFF) / 255.0F : colorRandom.nextFloat();
                        float green = fixedColor >= 0 ? ((fixedColor >> 8) & 0xFF) / 255.0F : colorRandom.nextFloat();
                        float blue = fixedColor >= 0 ? (fixedColor & 0xFF) / 255.0F : colorRandom.nextFloat();
                        float alpha = 0.48F;
                        quad(matrix, buffer, currentX, currentZ, segment, previousX, previousZ, red, green, blue, alpha, inner, outer, false, false, true, false);
                        quad(matrix, buffer, currentX, currentZ, segment, previousX, previousZ, red, green, blue, alpha, inner, outer, true, false, true, true);
                        quad(matrix, buffer, currentX, currentZ, segment, previousX, previousZ, red, green, blue, alpha, inner, outer, true, true, false, true);
                        quad(matrix, buffer, currentX, currentZ, segment, previousX, previousZ, red, green, blue, alpha, inner, outer, false, true, false, false);
                    }
                }
            }
        });
    }

    private static void quad(
            Matrix4fc matrix,
            VertexConsumer consumer,
            float x,
            float z,
            int segment,
            float previousX,
            float previousZ,
            float red,
            float green,
            float blue,
            float alpha,
            float inner,
            float outer,
            boolean xPositive,
            boolean zPositive,
            boolean previousXPositive,
            boolean previousZPositive
    ) {
        int r = (int) (red * 255.0F);
        int g = (int) (green * 255.0F);
        int b = (int) (blue * 255.0F);
        int a = (int) (alpha * 255.0F);
        consumer.addVertex(matrix, x + (xPositive ? outer : -outer), (float) (segment * 16), z + (zPositive ? outer : -outer)).setColor(r, g, b, a);
        consumer.addVertex(matrix, previousX + (xPositive ? inner : -inner), (float) ((segment + 1) * 16), previousZ + (zPositive ? inner : -inner)).setColor(r, g, b, a);
        consumer.addVertex(matrix, previousX + (previousXPositive ? inner : -inner), (float) ((segment + 1) * 16), previousZ + (previousZPositive ? inner : -inner)).setColor(r, g, b, a);
        consumer.addVertex(matrix, x + (previousXPositive ? outer : -outer), (float) (segment * 16), z + (previousZPositive ? outer : -outer)).setColor(r, g, b, a);
    }
}
