package qwq.qingyi.annihilationblade.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

/**
 * 2D GUI 任意四边形与三角形渲染状态，兼容 MC 26.1.2 的延迟图层合并渲染管线。
 */
public record QuadGuiElementRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        float x0,
        float y0,
        float x1,
        float y1,
        float x2,
        float y2,
        float x3,
        float y3,
        int col0,
        int col1,
        int col2,
        int col3,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    @Nullable
    public static QuadGuiElementRenderState create(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2fc pose,
            float x0,
            float y0,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3,
            int col0,
            int col1,
            int col2,
            int col3,
            @Nullable ScreenRectangle scissorArea
    ) {
        ScreenRectangle bounds = computeBounds(x0, y0, x1, y1, x2, y2, x3, y3, pose, scissorArea);
        if (bounds == null) {
            return null;
        }

        // 将屏幕空间顺时针顶点统一翻转为与原版 ColoredRectangleRenderState 一致的绕序，防止被剔除
        float cross = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
        if (cross > 0.0F) {
            return new QuadGuiElementRenderState(
                    pipeline, textureSetup, pose,
                    x0, y0, x3, y3, x2, y2, x1, y1,
                    col0, col3, col2, col1,
                    scissorArea, bounds
            );
        }

        return new QuadGuiElementRenderState(
                pipeline, textureSetup, pose,
                x0, y0, x1, y1, x2, y2, x3, y3,
                col0, col1, col2, col3,
                scissorArea, bounds
        );
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        consumer.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(this.col0);
        consumer.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(this.col1);
        consumer.addVertexWith2DPose(this.pose, this.x2, this.y2).setColor(this.col2);
        consumer.addVertexWith2DPose(this.pose, this.x3, this.y3).setColor(this.col3);
    }

    @Nullable
    private static ScreenRectangle computeBounds(
            float x0,
            float y0,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3,
            Matrix3x2fc pose,
            @Nullable ScreenRectangle scissorArea
    ) {
        int minX = Mth.floor(Math.min(Math.min(x0, x1), Math.min(x2, x3)));
        int minY = Mth.floor(Math.min(Math.min(y0, y1), Math.min(y2, y3)));
        int maxX = Mth.ceil(Math.max(Math.max(x0, x1), Math.max(x2, x3)));
        int maxY = Mth.ceil(Math.max(Math.max(y0, y1), Math.max(y2, y3)));
        ScreenRectangle raw = new ScreenRectangle(
                minX,
                minY,
                Math.max(1, maxX - minX),
                Math.max(1, maxY - minY)
        ).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(raw) : raw;
    }
}
