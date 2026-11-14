package qwq.qingyi.annihilationblade.client.infinity_stellaris;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.client.render.GuiRenderHelper;
import qwq.qingyi.annihilationblade.client.render.GuiRenderHelper.BladeStats;
import qwq.qingyi.annihilationblade.client.render.GuiRenderHelper.EnchantmentLine;
import qwq.qingyi.annihilationblade.client.render.GuiRenderHelper.Point2;

import java.util.List;

/**
 * 无尽星空专属星空星图幻彩 Tooltip 渲染器（适配 MC 26.1.2 GuiGraphicsExtractor）。
 */
public final class InfinityStellarisTooltipRenderer {
    private static final Identifier BLACK_HOLE_BACKGROUND =
            AnnihilationBladeex.prefix("textures/gui/infinity_stellaris_black_hole.png");
    private static final Identifier STAR_MAP_OVERLAY =
            AnnihilationBladeex.prefix("textures/gui/infinity_stellaris_star_map.png");
    private static final int BLACK_HOLE_TEXTURE_WIDTH = 1280;
    private static final int BLACK_HOLE_TEXTURE_HEIGHT = 853;
    private static final int STAR_MAP_TEXTURE_WIDTH = 1734;
    private static final int STAR_MAP_TEXTURE_HEIGHT = 907;
    private static final int WIDTH = 320;
    private static final int MIN_WIDTH = 238;
    private static final int[] COSMIC_SPECTRUM = new int[]{0x28F7FF, 0xF2FEFF, 0x8D7CFF, 0xFF4FD8, 0xFFE27A, 0x58B7FF};
    private static final long TITLE_CYCLE_MILLIS = 4200L;
    private static final String FINAL_WEAPON_KEY = "item.annihilationbladeex.infinity_stellaris.tooltip.final_weapon";

    private static final AuthorityChip[] AUTHORITY_CHIPS = new AuthorityChip[]{
            new AuthorityChip("item.annihilationbladeex.infinity_stellaris.tooltip.chip.flight", 0x58B7FF),
            new AuthorityChip("item.annihilationbladeex.infinity_stellaris.tooltip.chip.invulnerable", 0xF2FEFF),
            new AuthorityChip("item.annihilationbladeex.infinity_stellaris.tooltip.chip.kill_fallback", 0xFF4FD8),
            new AuthorityChip("item.annihilationbladeex.infinity_stellaris.tooltip.chip.no_recipe", 0xFFE27A),
            new AuthorityChip("item.annihilationbladeex.infinity_stellaris.tooltip.chip.fullbright", 0x28F7FF)
    };
    private static final EffectChip[] EFFECT_CHIPS = new EffectChip[]{
            new EffectChip("se.annihilationbladeex.entropy_dissolution", 0x28F7FF),
            new EffectChip("se.annihilationbladeex.cosmic_string_cut", 0xF2FEFF),
            new EffectChip("se.annihilationbladeex.curvature_rupture", 0x8D7CFF),
            new EffectChip("se.annihilationbladeex.gamma_thunderburst", 0xFF4FD8),
            new EffectChip("slash_art.annihilationbladeex.vacuum_decay_collapse", 0xFFE27A)
    };

    private InfinityStellarisTooltipRenderer() {
    }

    public static void render(
            GuiGraphicsExtractor graphics,
            Font font,
            ItemStack stack,
            List<Component> vanillaLines,
            int mouseX,
            int mouseY
    ) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int width = Math.min(WIDTH, Math.max(MIN_WIDTH, screenWidth - 18));
        int contentWidth = width - 24;
        List<EnchantmentLine> enchantments = GuiRenderHelper.collectEnchantments(stack);
        int enchantRows = (enchantments.size() + 1) / 2;
        int enchantRowHeight = screenHeight < 300 ? 9 : 10;
        int height = 208 + Math.max(1, enchantRows) * enchantRowHeight;
        height = Math.min(height, Math.max(210, screenHeight - 12));

        int x = mouseX + 12;
        int y = mouseY - 14;
        if (x + width > screenWidth - 6) {
            x = mouseX - width - 18;
        }
        x = Mth.clamp(x, 6, Math.max(6, screenWidth - width - 6));
        if (y + height > screenHeight - 6) {
            y = screenHeight - height - 6;
        }
        y = Mth.clamp(y, 6, Math.max(6, screenHeight - height - 6));

        float time = (float) (System.currentTimeMillis() % 120000L) / 1000.0F;
        renderFrame(graphics, x, y, width, height, time);
        graphics.nextStratum();
        renderContent(graphics, font, stack, vanillaLines, enchantments, x, y, width, height, contentWidth, enchantRowHeight, time);
    }

    private static void renderFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        int outerPadX = 28;
        int outerPadY = 26;
        int backdropMargin = 14;
        int outerX = x - outerPadX;
        int outerY = y - outerPadY;
        int outerWidth = width + outerPadX * 2;
        int outerHeight = height + outerPadY * 2;
        int backdropX = outerX - backdropMargin;
        int backdropY = outerY - backdropMargin;
        int backdropWidth = outerWidth + backdropMargin * 2;
        int backdropHeight = outerHeight + backdropMargin * 2;

        graphics.enableScissor(backdropX, backdropY, backdropX + backdropWidth, backdropY + backdropHeight);
        GuiRenderHelper.blitCover(
                graphics,
                BLACK_HOLE_BACKGROUND,
                backdropX,
                backdropY,
                backdropWidth,
                backdropHeight,
                BLACK_HOLE_TEXTURE_WIDTH,
                BLACK_HOLE_TEXTURE_HEIGHT,
                -10.0F,
                time * 0.28F,
                0.86F
        );
        graphics.nextStratum();
        graphics.fillGradient(backdropX, backdropY, backdropX + backdropWidth, backdropY + backdropHeight, 0x32000514, 0x70000514);
        graphics.disableScissor();

        graphics.nextStratum();
        drawOuterMagicCircle(graphics, backdropX, backdropY, backdropWidth, backdropHeight, time);
        renderWhiteOrbitBorder(graphics, outerX, outerY, outerWidth, outerHeight, time);
        drawOrbitingSigils(graphics, outerX, outerY, outerWidth, outerHeight, time);

        graphics.nextStratum();
        graphics.fillGradient(x, y, x + width, y + height, 0xB204091A, 0xC0111630);
        graphics.nextStratum();
        graphics.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);
        graphics.fillGradient(x + 2, y + 2, x + width - 2, y + height - 2, 0x9A020615, 0xD40A1024);
        graphics.nextStratum();
        GuiRenderHelper.blitCover(
                graphics,
                STAR_MAP_OVERLAY,
                x,
                y,
                width,
                height,
                STAR_MAP_TEXTURE_WIDTH,
                STAR_MAP_TEXTURE_HEIGHT,
                10.0F,
                time * 0.45F,
                0.16F
        );
        graphics.nextStratum();
        graphics.fillGradient(x + 4, y + 4, x + width - 4, y + 45, 0x8205091A, 0x2605091A);
        graphics.fillGradient(x + 5, y + 142, x + width - 5, y + height - 5, 0x4605091A, 0xB705091A);
        drawMovingAccents(graphics, x, y, width, height, time);
        graphics.disableScissor();

        graphics.nextStratum();
        renderGradientBorder(graphics, x, y, width, height, time);
    }

    private static void renderContent(
            GuiGraphicsExtractor graphics,
            Font font,
            ItemStack stack,
            List<Component> vanillaLines,
            List<EnchantmentLine> enchantments,
            int x,
            int y,
            int width,
            int height,
            int contentWidth,
            int enchantRowHeight,
            float time
    ) {
        int center = x + width / 2;
        Component itemName = vanillaLines.isEmpty() ? stack.getHoverName() : vanillaLines.get(0);
        GuiRenderHelper.drawGlowText(graphics, font, itemName, center - font.width(itemName) / 2, y + 8, 0xFFF2FEFF, 0x28F7FF, 0.45F);
        MutableComponent title = buildSpectralTitle(time);
        GuiRenderHelper.drawGlowText(graphics, font, title, center - font.width(title) / 2, y + 20, 0xFFF2FEFF, 0xFF4FD8, 0.45F);
        graphics.centeredText(
                font,
                Component.translatable("item.annihilationbladeex.infinity_stellaris.tooltip.subtitle"),
                center,
                y + 34,
                0xFFA8D9FF
        );
        graphics.item(stack, x + 13, y + 13);

        int cursorY = y + 52;
        drawSectionTitle(graphics, font, x + 12, cursorY, contentWidth,
                Component.translatable("item.annihilationbladeex.infinity_stellaris.tooltip.section.authority"), time);
        cursorY += 13;
        drawAuthorityChips(graphics, font, x + 12, cursorY, contentWidth, time);
        cursorY += 26;
        drawSectionTitle(graphics, font, x + 12, cursorY, contentWidth,
                Component.translatable("item.annihilationbladeex.infinity_stellaris.tooltip.section.attributes"), time + 0.4F);
        cursorY += 13;
        drawStats(graphics, font, stack, x + 12, cursorY, contentWidth, time);
        cursorY += 28;
        drawSectionTitle(graphics, font, x + 12, cursorY, contentWidth,
                Component.translatable("item.annihilationbladeex.infinity_stellaris.tooltip.section.effects"), time + 0.8F);
        cursorY += 13;
        drawEffectChips(graphics, font, x + 12, cursorY, contentWidth, time);
        cursorY += 40;
        drawSectionTitle(graphics, font, x + 12, cursorY, contentWidth,
                Component.translatable("item.annihilationbladeex.infinity_stellaris.tooltip.section.enchantments"), time + 1.2F);
        cursorY += 13;
        drawEnchantments(graphics, font, x + 12, cursorY, contentWidth, height - (cursorY - y) - 10, enchantRowHeight, enchantments, time);

        int pulse = (int) ((contentWidth - 2) * (0.5F + 0.5F * Mth.sin(time * 2.1F)));
        graphics.fill(x + 13, y + height - 7, x + width - 13, y + height - 6, 0x5528F7FF);
        graphics.fill(x + 13, y + height - 7, x + 13 + pulse, y + height - 6, 0xCCF2FEFF);
    }

    private static void drawSectionTitle(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, Component title, float phase) {
        int accent = sampleSpectrum(phase * 1.2F);
        int railWidth = Math.max(42, Math.round(width * 0.62F));
        float pulsePhase = 0.5F + 0.5F * Mth.sin(phase * 2.3F);
        int pulseWidth = Math.max(18, Math.round(railWidth * (0.22F + pulsePhase * 0.34F)));
        graphics.fill(x, y + 10, x + railWidth, y + 11, GuiRenderHelper.withAlpha(accent, 0.28F));
        graphics.fill(x, y + 10, x + pulseWidth, y + 11, GuiRenderHelper.withAlpha(accent, 0.82F));
        graphics.text(font, title, x + 2, y, GuiRenderHelper.withAlpha(0xF2FEFF, 0.95F), false);
    }

    private static void drawAuthorityChips(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, float time) {
        int gap = 3;
        int chipWidth = (width - gap * (AUTHORITY_CHIPS.length - 1)) / AUTHORITY_CHIPS.length;
        for (int i = 0; i < AUTHORITY_CHIPS.length; i++) {
            AuthorityChip chip = AUTHORITY_CHIPS[i];
            drawSmallChip(graphics, font, x + i * (chipWidth + gap), y, chipWidth, 12, Component.translatable(chip.key()), chip.color(), time + i * 0.31F);
        }
    }

    private static void drawStats(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y, int width, float time) {
        BladeStats stats = GuiRenderHelper.readBladeStats(stack);
        StatCell[] cells = new StatCell[]{
                new StatCell("item.annihilationbladeex.infinity_stellaris.tooltip.stat.kills", GuiRenderHelper.formatNumber(stats.killCount()), 0x28F7FF),
                new StatCell("item.annihilationbladeex.infinity_stellaris.tooltip.stat.souls", GuiRenderHelper.formatNumber(stats.proudSoul()), 0xFFE27A),
                new StatCell("item.annihilationbladeex.infinity_stellaris.tooltip.stat.refine", GuiRenderHelper.formatNumber(stats.refine()), 0xFF4FD8),
                new StatCell("item.annihilationbladeex.infinity_stellaris.tooltip.stat.attack", "1,000,000", 0xF2FEFF),
                new StatCell("item.annihilationbladeex.infinity_stellaris.tooltip.stat.durability", "2147483647", 0x8D7CFF)
        };
        int gap = 3;
        int cellWidth = (width - gap * (cells.length - 1)) / cells.length;
        for (int i = 0; i < cells.length; i++) {
            drawStatCell(graphics, font, x + i * (cellWidth + gap), y, cellWidth, 20, cells[i], time + i * 0.27F);
        }
    }

    private static void drawEffectChips(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, float time) {
        int columns = 3;
        int gap = 4;
        int chipWidth = (width - gap * (columns - 1)) / columns;
        for (int i = 0; i < EFFECT_CHIPS.length; i++) {
            int col = i % columns;
            int row = i / columns;
            EffectChip chip = EFFECT_CHIPS[i];
            drawSmallChip(graphics, font, x + col * (chipWidth + gap), y + row * 15, chipWidth, 12, Component.translatable(chip.key()), chip.color(), time + i * 0.42F);
        }
    }

    private static void drawEnchantments(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            int rowHeight,
            List<EnchantmentLine> enchantments,
            float time
    ) {
        if (enchantments.isEmpty() || height <= 8) {
            return;
        }
        int columns = 2;
        int gap = 5;
        int colWidth = (width - gap) / columns;
        int rows = Math.min((enchantments.size() + 1) / 2, Math.max(1, height / rowHeight));
        graphics.fill(x, y - 2, x + width, y + rows * rowHeight + 4, 0x78030A1C);
        graphics.fill(x, y - 2, x + width, y - 1, GuiRenderHelper.withAlpha(sampleSpectrum(time * 0.8F), 0.78F));
        for (int i = 0; i < enchantments.size() && i < rows * columns; i++) {
            int col = i % columns;
            int row = i / columns;
            EnchantmentLine line = enchantments.get(i);
            int entryX = x + col * (colWidth + gap);
            int entryY = y + row * rowHeight;
            int color = sampleSpectrum(time * 0.6F + i * 0.37F);
            int railWidth = Math.max(28, Math.round(colWidth * 0.66F));
            int pulseWidth = Math.max(16, Math.round(railWidth * (0.28F + 0.36F * (0.5F + 0.5F * Mth.sin(time * 2.0F + i * 0.31F)))));
            graphics.fill(entryX, entryY + rowHeight - 2, entryX + railWidth, entryY + rowHeight - 1, GuiRenderHelper.withAlpha(color, 0.18F));
            graphics.fill(entryX, entryY + rowHeight - 2, entryX + pulseWidth, entryY + rowHeight - 1, GuiRenderHelper.withAlpha(color, 0.42F));
            graphics.text(font, GuiRenderHelper.trimToWidth(font, line.text(), colWidth - 4), entryX + 2, entryY, 0xFFE8F6FF, false);
        }
    }

    private static void drawSmallChip(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component text,
            int rgb,
            float phase
    ) {
        graphics.fill(x, y, x + width, y + height, 0x8A061024);
        graphics.fill(x, y, x + width, y + 1, GuiRenderHelper.withAlpha(rgb, 0.78F));
        int railWidth = Math.max(14, Math.round((width - 4) * 0.58F));
        int pulse = Math.max(8, Math.round(railWidth * (0.28F + 0.38F * (0.5F + 0.5F * Mth.sin(phase * 2.6F)))));
        graphics.fill(x + 2, y + height - 2, x + 2 + railWidth, y + height - 1, GuiRenderHelper.withAlpha(rgb, 0.2F));
        graphics.fill(x + 2, y + height - 2, x + 2 + pulse, y + height - 1, GuiRenderHelper.withAlpha(rgb, 0.72F));
        graphics.text(font, GuiRenderHelper.trimToWidth(font, text.getString(), width - 7), x + 4, y + 2, 0xFFF2FEFF, false);
    }

    private static void drawStatCell(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            StatCell cell,
            float phase
    ) {
        int accent = cell.color();
        graphics.fill(x, y, x + width, y + height, 0x8F050B1C);
        graphics.fill(x, y, x + width, y + 1, GuiRenderHelper.withAlpha(accent, 0.72F));
        int railWidth = Math.max(14, Math.round((width - 4) * 0.56F));
        int pulse = Math.max(8, Math.round(railWidth * (0.28F + 0.34F * (0.5F + 0.5F * Mth.sin(phase * 2.2F)))));
        graphics.fill(x + 2, y + height - 3, x + 2 + railWidth, y + height - 2, GuiRenderHelper.withAlpha(accent, 0.18F));
        graphics.fill(x + 2, y + height - 3, x + 2 + pulse, y + height - 2, GuiRenderHelper.withAlpha(accent, 0.68F));
        graphics.text(font, GuiRenderHelper.trimToWidth(font, Component.translatable(cell.labelKey()).getString(), width - 4), x + 3, y + 2, 0xFFA8D9FF, false);
        graphics.text(font, GuiRenderHelper.trimToWidth(font, cell.value(), width - 4), x + 3, y + 11, 0xFFF2FEFF, false);
    }

    private static MutableComponent buildSpectralTitle(float time) {
        String text = ChatFormatting.stripFormatting(I18n.get(FINAL_WEAPON_KEY));
        if (text == null || text.isEmpty()) {
            text = "Infinity Stellaris";
        }

        MutableComponent title = Component.empty();
        float drift = ((System.currentTimeMillis() % TITLE_CYCLE_MILLIS) / (float) TITLE_CYCLE_MILLIS) * COSMIC_SPECTRUM.length;
        int visualIndex = 0;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            String glyph = new String(Character.toChars(codePoint));
            int color = sampleSpectrum(visualIndex * 0.42F - drift + Mth.sin(time * 0.8F) * 0.12F);
            title.append(Component.literal(glyph).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true)));
            offset += Character.charCount(codePoint);
            if (!Character.isWhitespace(codePoint)) {
                visualIndex++;
            }
        }
        return title;
    }

    private static void drawMovingAccents(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        for (int i = 0; i < 8; i++) {
            float phase = (time * 0.09F + i * 0.137F) % 1.0F;
            float sx = x - 34.0F + phase * (width + 68.0F);
            int colorA = GuiRenderHelper.withAlpha(sampleSpectrum(i * 0.7F + time * 0.2F), 0.16F);
            int colorB = GuiRenderHelper.withAlpha(0xF2FEFF, 0.08F);
            GuiRenderHelper.drawBeam(graphics, sx, y + height + 12.0F, sx + 62.0F, y - 16.0F, 1.0F, colorA, colorB);
        }

        for (int i = 0; i < 36; i++) {
            float px = x + 8.0F + GuiRenderHelper.noise(i * 18.73F) * (width - 16.0F);
            float py = y + 8.0F + GuiRenderHelper.noise(i * 8.39F + 3.0F) * (height - 16.0F);
            float pulse = 0.42F + 0.58F * Mth.sin(time * 1.9F + i * 0.61F);
            int color = GuiRenderHelper.withAlpha(i % 5 == 0 ? 0xF2FEFF : 0x58B7FF, 0.12F + pulse * 0.16F);
            GuiRenderHelper.drawQuad(graphics, px, py, px + 1.0F + pulse, py + 1.0F + pulse, color);
        }
    }

    private static void drawOuterMagicCircle(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        float cx = x + width / 2.0F;
        float cy = y + height / 2.0F;
        float radius = Math.max(width, height) * 0.53F + 12.0F;
        float spin = time * 0.18F;
        float counterSpin = -time * 0.13F;
        int ghost = GuiRenderHelper.withAlpha(0xF2FEFF, 0.08F);
        int soft = GuiRenderHelper.withAlpha(0xF2FEFF, 0.18F + 0.04F * (0.5F + 0.5F * Mth.sin(time * 1.3F)));
        int bright = GuiRenderHelper.withAlpha(0xFFFFFF, 0.36F + 0.08F * (0.5F + 0.5F * Mth.sin(time * 1.8F)));

        GuiRenderHelper.drawCircle(graphics, cx, cy, radius, spin, 64, 0.65F, soft);
        GuiRenderHelper.drawCircle(graphics, cx, cy, radius * 0.93F, counterSpin, 64, 0.45F, ghost);
        GuiRenderHelper.drawCircle(graphics, cx, cy, radius * 0.78F, spin * 1.45F, 48, 0.55F, GuiRenderHelper.withAlpha(0xDFFBFF, 0.14F));
        GuiRenderHelper.drawPolygon(graphics, cx, cy, radius * 0.82F, 6, spin + Mth.PI / 6.0F, 0.62F, soft);
        GuiRenderHelper.drawPolygon(graphics, cx, cy, radius * 0.66F, 12, counterSpin, 0.42F, GuiRenderHelper.withAlpha(0xFFFFFF, 0.12F));
        GuiRenderHelper.drawSteppedStar(graphics, cx, cy, radius * 0.72F, 12, 5, spin * 0.9F, 0.5F, GuiRenderHelper.withAlpha(0xFFFFFF, 0.16F));
        GuiRenderHelper.drawSteppedStar(graphics, cx, cy, radius * 0.52F, 9, 4, counterSpin * 1.4F, 0.44F, GuiRenderHelper.withAlpha(0xF2FEFF, 0.13F));

        for (int i = 0; i < 32; i++) {
            float angle = spin + i * Mth.PI * 2.0F / 32.0F;
            float inner = radius * (i % 4 == 0 ? 0.83F : 0.9F);
            float outer = radius * (i % 4 == 0 ? 1.03F : 0.99F);
            Point2 a = GuiRenderHelper.radialPoint(cx, cy, inner, angle);
            Point2 b = GuiRenderHelper.radialPoint(cx, cy, outer, angle);
            int color = i % 4 == 0 ? bright : GuiRenderHelper.withAlpha(0xF2FEFF, 0.16F);
            GuiRenderHelper.drawBeam(graphics, a.x(), a.y(), b.x(), b.y(), i % 4 == 0 ? 0.62F : 0.38F, color, color);
        }

        for (int i = 0; i < 8; i++) {
            float angle = counterSpin * 1.2F + i * Mth.PI * 2.0F / 8.0F;
            Point2 p = GuiRenderHelper.radialPoint(cx, cy, radius * 0.98F, angle);
            GuiRenderHelper.drawRotatingDiamond(graphics, p.x(), p.y(), 5.5F, spin * 2.0F + i, GuiRenderHelper.withAlpha(0xFFFFFF, 0.4F));
        }

        for (int i = 0; i < 4; i++) {
            float angle = spin * 0.7F + Mth.PI / 4.0F + i * Mth.HALF_PI;
            Point2 p = GuiRenderHelper.radialPoint(cx, cy, radius * 0.62F, angle);
            drawRotatingHexagram(graphics, p.x(), p.y(), 8.0F, counterSpin * 2.1F + i, GuiRenderHelper.withAlpha(0xFFFFFF, 0.22F));
        }
    }

    private static void renderWhiteOrbitBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        int glow = GuiRenderHelper.withAlpha(0xF2FEFF, 0.11F);
        int base = GuiRenderHelper.withAlpha(0xF2FEFF, 0.3F);
        GuiRenderHelper.drawBeam(graphics, x, y, x + width, y, 2.6F, glow, glow);
        GuiRenderHelper.drawBeam(graphics, x, y + height, x + width, y + height, 2.6F, glow, glow);
        GuiRenderHelper.drawBeam(graphics, x, y, x, y + height, 2.6F, glow, glow);
        GuiRenderHelper.drawBeam(graphics, x + width, y, x + width, y + height, 2.6F, glow, glow);
        GuiRenderHelper.drawBeam(graphics, x, y, x + width, y, 0.7F, base, base);
        GuiRenderHelper.drawBeam(graphics, x, y + height, x + width, y + height, 0.7F, base, base);
        GuiRenderHelper.drawBeam(graphics, x, y, x, y + height, 0.7F, base, base);
        GuiRenderHelper.drawBeam(graphics, x + width, y, x + width, y + height, 0.7F, base, base);

        for (int i = 0; i < 6; i++) {
            float start = (time * 0.055F + i / 6.0F) % 1.0F;
            float length = 0.045F + 0.015F * (0.5F + 0.5F * Mth.sin(time * 1.7F + i));
            int head = GuiRenderHelper.withAlpha(0xFFFFFF, 0.72F);
            int tail = GuiRenderHelper.withAlpha(i % 2 == 0 ? 0xDFFBFF : 0xF7F1FF, 0.12F);
            drawBorderTrail(graphics, x, y, width, height, start, length, 1.1F, head, tail);
        }
    }

    private static void drawBorderTrail(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float start,
            float length,
            float thickness,
            int headColor,
            int tailColor
    ) {
        int segments = 18;
        for (int i = 0; i < segments; i++) {
            float a = start + length * i / segments;
            float b = start + length * (i + 1) / segments;
            Point2 p1 = GuiRenderHelper.pointOnBorder(x, y, width, height, a);
            Point2 p2 = GuiRenderHelper.pointOnBorder(x, y, width, height, b);
            float blend = i / (float) Math.max(1, segments - 1);
            int colorA = GuiRenderHelper.lerpArgb(headColor, tailColor, blend);
            int colorB = GuiRenderHelper.lerpArgb(headColor, tailColor, Math.min(1.0F, blend + 1.0F / segments));
            GuiRenderHelper.drawBeam(graphics, p1.x(), p1.y(), p2.x(), p2.y(), thickness, colorA, colorB);
        }
    }

    private static void drawOrbitingSigils(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        for (int i = 0; i < 4; i++) {
            float progress = (time * 0.048F + i * 0.25F) % 1.0F;
            Point2 point = GuiRenderHelper.pointOnBorder(x, y, width, height, progress);
            float radius = 8.0F + (i % 2) * 1.5F;
            float rotation = time * (1.25F + i * 0.13F) + i * 0.9F;
            int glow = GuiRenderHelper.withAlpha(0xF2FEFF, 0.16F);
            int white = GuiRenderHelper.withAlpha(0xFFFFFF, 0.86F);
            drawRotatingHexagram(graphics, point.x(), point.y(), radius + 2.0F, rotation, glow);
            drawRotatingHexagram(graphics, point.x(), point.y(), radius, -rotation * 0.78F, white);
            GuiRenderHelper.drawRotatingDiamond(graphics, point.x(), point.y(), radius * 0.82F, rotation * 1.7F, GuiRenderHelper.withAlpha(0xDFFBFF, 0.7F));
        }
    }

    private static void drawRotatingHexagram(GuiGraphicsExtractor graphics, float cx, float cy, float radius, float rotation, int color) {
        float[] xs = new float[6];
        float[] ys = new float[6];
        for (int i = 0; i < 6; i++) {
            float angle = rotation + i * Mth.PI / 3.0F;
            xs[i] = cx + Mth.cos(angle) * radius;
            ys[i] = cy + Mth.sin(angle) * radius;
        }
        GuiRenderHelper.drawBeam(graphics, xs[0], ys[0], xs[2], ys[2], 0.45F, color, color);
        GuiRenderHelper.drawBeam(graphics, xs[2], ys[2], xs[4], ys[4], 0.45F, color, color);
        GuiRenderHelper.drawBeam(graphics, xs[4], ys[4], xs[0], ys[0], 0.45F, color, color);
        GuiRenderHelper.drawBeam(graphics, xs[1], ys[1], xs[3], ys[3], 0.45F, color, color);
        GuiRenderHelper.drawBeam(graphics, xs[3], ys[3], xs[5], ys[5], 0.45F, color, color);
        GuiRenderHelper.drawBeam(graphics, xs[5], ys[5], xs[1], ys[1], 0.45F, color, color);
    }

    private static void renderGradientBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        for (int layer = 4; layer >= 1; layer--) {
            float spread = layer * 1.9F;
            float alpha = 0.07F + (4 - layer) * 0.07F;
            drawSegmentedBorder(graphics, x, y, width, height, spread, 0.8F + layer * 0.15F, time * 0.08F + layer * 0.13F, alpha);
        }
        drawSegmentedBorder(graphics, x, y, width, height, 0.8F, 1.3F, time * 0.11F, 0.88F);
        drawCornerMarks(graphics, x, y, width, height, time);
    }

    private static void drawSegmentedBorder(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float spread,
            float thickness,
            float drift,
            float alpha
    ) {
        int segments = 18;
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;
            int c1 = GuiRenderHelper.withAlpha(sampleSpectrum(t1 * COSMIC_SPECTRUM.length + drift), alpha);
            int c2 = GuiRenderHelper.withAlpha(sampleSpectrum(t2 * COSMIC_SPECTRUM.length + drift), alpha);
            float sx = x + width * t1;
            float ex = x + width * t2;
            GuiRenderHelper.drawQuad(graphics, sx, y - spread, ex, y - spread, ex, y - spread + thickness, sx, y - spread + thickness, c1, c2, c2, c1);
            GuiRenderHelper.drawQuad(graphics, sx, y + height + spread, ex, y + height + spread, ex, y + height + spread - thickness, sx, y + height + spread - thickness, c2, c1, c1, c2);
            if (i < segments / 2) {
                float vt1 = (float) i / (segments / 2);
                float vt2 = (float) (i + 1) / (segments / 2);
                int v1 = GuiRenderHelper.withAlpha(sampleSpectrum(vt1 * COSMIC_SPECTRUM.length - drift), alpha * 0.9F);
                int v2 = GuiRenderHelper.withAlpha(sampleSpectrum(vt2 * COSMIC_SPECTRUM.length - drift), alpha * 0.9F);
                float sy = y + height * vt1;
                float ey = y + height * vt2;
                GuiRenderHelper.drawQuad(graphics, x - spread, sy, x - spread + thickness, sy, x - spread + thickness, ey, x - spread, ey, v1, v1, v2, v2);
                GuiRenderHelper.drawQuad(graphics, x + width + spread, sy, x + width + spread - thickness, sy, x + width + spread - thickness, ey, x + width + spread, ey, v2, v2, v1, v1);
            }
        }
    }

    private static void drawCornerMarks(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        int cyan = GuiRenderHelper.withAlpha(0x28F7FF, 0.64F + 0.18F * (0.5F + 0.5F * Mth.sin(time * 2.0F)));
        int white = GuiRenderHelper.withAlpha(0xF2FEFF, 0.55F);
        int gold = GuiRenderHelper.withAlpha(0xFFE27A, 0.5F);
        GuiRenderHelper.drawBeam(graphics, x + 7.0F, y + 18.0F, x + 26.0F, y + 7.0F, 1.4F, cyan, white);
        GuiRenderHelper.drawBeam(graphics, x + width - 7.0F, y + 18.0F, x + width - 26.0F, y + 7.0F, 1.4F, white, gold);
        GuiRenderHelper.drawBeam(graphics, x + 7.0F, y + height - 18.0F, x + 26.0F, y + height - 7.0F, 1.4F, gold, cyan);
        GuiRenderHelper.drawBeam(graphics, x + width - 7.0F, y + height - 26.0F, x + width - 26.0F, y + height - 7.0F, 1.4F, cyan, white);
    }

    private static int sampleSpectrum(float position) {
        return GuiRenderHelper.sampleSpectrum(COSMIC_SPECTRUM, position);
    }

    private record AuthorityChip(String key, int color) {
    }

    private record EffectChip(String key, int color) {
    }

    private record StatCell(String labelKey, String value, int color) {
    }
}
