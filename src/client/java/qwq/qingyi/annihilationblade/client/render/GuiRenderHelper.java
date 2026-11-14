package qwq.qingyi.annihilationblade.client.render;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.joml.Matrix3x2f;
import qwq.qingyi.annihilationblade.common.SlashBladeStateSupport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 客户端 2D Tooltip 与特效通用矢量绘图、色彩插值与文字发光工具。
 */
public final class GuiRenderHelper {
    private GuiRenderHelper() {
    }

    public static void drawQuad(GuiGraphicsExtractor graphics, float x1, float y1, float x2, float y2, int color) {
        drawQuad(graphics, x1, y1, x2, y1, x2, y2, x1, y2, color, color, color, color);
    }

    public static void drawQuad(
            GuiGraphicsExtractor graphics,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float x4, float y4,
            int color
    ) {
        drawQuad(graphics, x1, y1, x2, y2, x3, y3, x4, y4, color, color, color, color);
    }

    public static void drawQuad(
            GuiGraphicsExtractor graphics,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            float x4, float y4,
            int color1, int color2, int color3, int color4
    ) {
        QuadGuiElementRenderState state = QuadGuiElementRenderState.create(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                new Matrix3x2f(graphics.pose()),
                x1, y1, x2, y2, x3, y3, x4, y4,
                color1, color2, color3, color4,
                graphics.scissorStack.peek()
        );
        if (state != null) {
            graphics.guiRenderState.addGuiElement(state);
        }
    }

    public static void drawTriangle(
            GuiGraphicsExtractor graphics,
            float x1, float y1,
            float x2, float y2,
            float x3, float y3,
            int color
    ) {
        drawQuad(graphics, x1, y1, x2, y2, x3, y3, x3, y3, color, color, color, color);
    }

    public static void drawBeam(
            GuiGraphicsExtractor graphics,
            float x1, float y1,
            float x2, float y2,
            float halfWidth,
            int colorA,
            int colorB
    ) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 0.001F) {
            return;
        }
        float nx = -dy / length * halfWidth;
        float ny = dx / length * halfWidth;
        drawQuad(
                graphics,
                x1 - nx, y1 - ny,
                x1 + nx, y1 + ny,
                x2 + nx, y2 + ny,
                x2 - nx, y2 - ny,
                colorA, colorA, colorB, colorB
        );
    }

    public static void drawCircle(
            GuiGraphicsExtractor graphics,
            float cx, float cy,
            float radius,
            float rotation,
            int segments,
            float thickness,
            int color
    ) {
        float prevX = cx + Mth.cos(rotation) * radius;
        float prevY = cy + Mth.sin(rotation) * radius;
        for (int i = 1; i <= segments; i++) {
            float angle = rotation + i * Mth.PI * 2.0F / segments;
            float currX = cx + Mth.cos(angle) * radius;
            float currY = cy + Mth.sin(angle) * radius;
            drawBeam(graphics, prevX, prevY, currX, currY, thickness, color, color);
            prevX = currX;
            prevY = currY;
        }
    }

    public static void drawPolygon(
            GuiGraphicsExtractor graphics,
            float cx, float cy,
            float radius,
            int points,
            float rotation,
            float thickness,
            int color
    ) {
        drawCircle(graphics, cx, cy, radius, rotation, points, thickness, color);
    }

    public static void drawGlowText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int x,
            int y,
            int color,
            int glowColor,
            float glowAlpha
    ) {
        int glowArgb = withAlpha(glowColor, glowAlpha);
        graphics.text(font, text, x - 1, y, glowArgb, false);
        graphics.text(font, text, x + 1, y, glowArgb, false);
        graphics.text(font, text, x, y - 1, glowArgb, false);
        graphics.text(font, text, x, y + 1, glowArgb, false);
        graphics.text(font, text, x, y, color, true);
    }

    public static int withAlpha(int rgbOrArgb, float alpha) {
        int a = Mth.clamp(Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F), 0, 255);
        return (rgbOrArgb & 0x00FFFFFF) | (a << 24);
    }

    public static int scaleAlpha(int argb, float factor) {
        int a = Mth.clamp((int) (((argb >>> 24) & 0xFF) * Mth.clamp(factor, 0.0F, 1.0F)), 0, 255);
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    public static int lerpRgb(int from, int to, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        int r = Mth.clamp(Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t), 0, 255);
        int g = Mth.clamp(Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t), 0, 255);
        int b = Mth.clamp(Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    public static int lerpArgb(int from, int to, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        int a = Mth.clamp(Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t), 0, 255);
        return (lerpRgb(from, to, t) & 0x00FFFFFF) | (a << 24);
    }

    public static int sampleSpectrum(int[] spectrum, float position) {
        int length = spectrum.length;
        float wrapped = position - (float) Math.floor(position / length) * length;
        if (wrapped >= length) {
            wrapped = 0.0F;
        }
        int index = Math.min(length - 1, Math.max(0, (int) Math.floor(wrapped)));
        int next = index == length - 1 ? 0 : index + 1;
        float blend = wrapped - index;
        blend = blend * blend * (3.0F - 2.0F * blend);
        return lerpRgb(spectrum[index], spectrum[next], blend);
    }

    public static float noise(float seed) {
        return Mth.frac(Mth.sin(seed) * 43758.545F);
    }

    public static String formatNumber(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    public static String trimToWidth(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String ellipsis = "...";
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ellipsis))) + ellipsis;
    }

    public static BladeStats readBladeStats(ItemStack stack) {
        Optional<ISlashBladeState> state = SlashBladeStateSupport.state(stack);
        if (state.isPresent()) {
            ISlashBladeState bladeState = state.get();
            return new BladeStats(bladeState.getProudSoulCount(), bladeState.getKillCount(), bladeState.getRefine());
        }
        return new BladeStats(
                SlashBladeStateSupport.proudSoul(stack),
                SlashBladeStateSupport.killCount(stack),
                SlashBladeStateSupport.refine(stack)
        );
    }

    public static List<EnchantmentLine> collectEnchantments(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            return List.of();
        }
        List<EnchantmentLine> lines = new ArrayList<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            Identifier id = enchantment.unwrapKey().map(ResourceKey::identifier).orElse(null);
            String sortKey = id == null ? "unknown" : id.toString();
            lines.add(new EnchantmentLine(sortKey, Enchantment.getFullname(enchantment, level).getString()));
        }
        lines.sort(Comparator.comparing(EnchantmentLine::sortKey));
        return lines;
    }

    public static Point2 radialPoint(float cx, float cy, float radius, float angle) {
        return new Point2(cx + Mth.cos(angle) * radius, cy + Mth.sin(angle) * radius);
    }

    public static Point2 pointOnBorder(int x, int y, int width, int height, float progress) {
        float p = progress - (float) Math.floor(progress);
        float perimeter = (width + height) * 2.0F;
        float distance = p * perimeter;
        if (distance < width) {
            return new Point2(x + distance, y);
        }
        distance -= width;
        if (distance < height) {
            return new Point2(x + width, y + distance);
        }
        distance -= height;
        if (distance < width) {
            return new Point2(x + width - distance, y + height);
        }
        distance -= width;
        return new Point2(x, y + height - distance);
    }

    public static void drawDiamond(GuiGraphicsExtractor graphics, float cx, float cy, float radius, int color) {
        drawQuad(graphics, cx, cy - radius, cx + radius, cy, cx, cy + radius, cx - radius, cy, color);
    }

    public static void drawRotatingDiamond(GuiGraphicsExtractor graphics, float cx, float cy, float radius, float rotation, int color) {
        float x1 = cx + Mth.cos(rotation) * radius;
        float y1 = cy + Mth.sin(rotation) * radius;
        float x2 = cx + Mth.cos(rotation + Mth.HALF_PI) * radius * 0.62F;
        float y2 = cy + Mth.sin(rotation + Mth.HALF_PI) * radius * 0.62F;
        float x3 = cx + Mth.cos(rotation + Mth.PI) * radius;
        float y3 = cy + Mth.sin(rotation + Mth.PI) * radius;
        float x4 = cx + Mth.cos(rotation + Mth.PI + Mth.HALF_PI) * radius * 0.62F;
        float y4 = cy + Mth.sin(rotation + Mth.PI + Mth.HALF_PI) * radius * 0.62F;
        drawBeam(graphics, x1, y1, x2, y2, 0.55F, color, color);
        drawBeam(graphics, x2, y2, x3, y3, 0.55F, color, color);
        drawBeam(graphics, x3, y3, x4, y4, 0.55F, color, color);
        drawBeam(graphics, x4, y4, x1, y1, 0.55F, color, color);
    }

    public static void drawSteppedStar(
            GuiGraphicsExtractor graphics,
            float cx,
            float cy,
            float radius,
            int points,
            int step,
            float rotation,
            float thickness,
            int color
    ) {
        for (int i = 0; i < points; i++) {
            Point2 a = radialPoint(cx, cy, radius, rotation + i * Mth.PI * 2.0F / points);
            Point2 b = radialPoint(cx, cy, radius, rotation + ((i + step) % points) * Mth.PI * 2.0F / points);
            drawBeam(graphics, a.x(), a.y(), b.x(), b.y(), thickness, color, color);
        }
    }

    public static void blitCover(
            GuiGraphicsExtractor graphics,
            Identifier texture,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight,
            float biasX,
            float time,
            float alpha
    ) {
        float imageAspect = (float) textureWidth / textureHeight;
        float targetAspect = (float) width / height;
        int imageWidth;
        int imageHeight;
        if (imageAspect > targetAspect) {
            imageHeight = height;
            imageWidth = Math.round(height * imageAspect);
        } else {
            imageWidth = width;
            imageHeight = Math.round(width / imageAspect);
        }

        int imageX = x - (imageWidth - width) / 2 + Math.round(biasX + Mth.sin(time * 0.18F) * 3.0F);
        int imageY = y - (imageHeight - height) / 2 + Math.round(Mth.cos(time * 0.16F) * 3.0F);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                imageX,
                imageY,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                textureWidth,
                textureHeight,
                textureWidth,
                textureHeight,
                ARGB.white(alpha)
        );
    }

    public record BladeStats(int proudSoul, int killCount, int refine) {
    }

    public record EnchantmentLine(String sortKey, String text) {
    }

    public record Point2(float x, float y) {
    }
}
