package qwq.qingyi.annihilationblade.client.nightfall_dragon;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.client.render.GuiRenderHelper;
import qwq.qingyi.annihilationblade.client.render.GuiRenderHelper.BladeStats;
import qwq.qingyi.annihilationblade.client.render.GuiRenderHelper.EnchantmentLine;
import qwq.qingyi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;

import java.util.List;

/**
 * 魔龙夜陨终极龙魂姿态与暗黑龙威旗舰级 Tooltip 渲染器（适配 MC 26.1.2 GuiGraphicsExtractor）。
 */
public final class NightfallDragonTooltipRenderer {
    private static final int WIDTH = 320;
    private static final int MIN_WIDTH = 240;

    private static final int[] SEALED_SPECTRUM = new int[]{0x9D4EDD, 0xE0AAFF, 0x7B2CBF, 0x5A189A, 0x3C096C, 0xC77DFF};
    private static final int[] AWAKENED_SPECTRUM = new int[]{0xFFB703, 0xFFD000, 0xFB8500, 0xD00000, 0xFF0000, 0xFFE600};
    private static final int[] FINAL_SPECTRUM = new int[]{0xFF0055, 0xFF2A85, 0x9000FF, 0x00FFFF, 0xFF4800, 0xD800FF};

    private static final DragonChip[] DRAGON_CHIPS = new DragonChip[]{
            new DragonChip("item.annihilationbladeex.nightfall_dragon.tooltip.chip.dragon_pressure", 0x9D4EDD),
            new DragonChip("item.annihilationbladeex.nightfall_dragon.tooltip.chip.demonic_blood", 0xFF0055),
            new DragonChip("item.annihilationbladeex.nightfall_dragon.tooltip.chip.reverse_scale", 0xFB8500),
            new DragonChip("item.annihilationbladeex.nightfall_dragon.tooltip.chip.outer_god", 0x00FFFF),
            new DragonChip("item.annihilationbladeex.nightfall_dragon.tooltip.chip.god_body", 0xFFB703),
            new DragonChip("item.annihilationbladeex.nightfall_dragon.tooltip.chip.scale_guard", 0x00FF88),
            new DragonChip("item.annihilationbladeex.nightfall_dragon.tooltip.chip.blade_storm", 0xE0AAFF),
            new DragonChip("item.annihilationbladeex.nightfall_dragon.tooltip.chip.world_cleaving", 0xFF4500),
            new DragonChip("item.annihilationbladeex.nightfall_dragon.tooltip.chip.absolute_domain", 0xD800FF)
    };

    private NightfallDragonTooltipRenderer() {
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
        int height = 236 + Math.max(1, enchantRows) * enchantRowHeight;
        height = Math.min(height, Math.max(235, screenHeight - 12));

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
        String form = NightfallDragonDefinitions.getForm(stack);

        renderFrame(graphics, x, y, width, height, time, form, mouseX, mouseY);
        graphics.nextStratum();
        renderContent(graphics, font, stack, enchantments, x, y, width, height, contentWidth, enchantRowHeight, time, form);
    }

    private static void renderFrame(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float time,
            String form,
            int mouseX,
            int mouseY
    ) {
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

        // 1. 背景暗黑深渊星云梯度与火花
        graphics.enableScissor(backdropX, backdropY, backdropX + backdropWidth, backdropY + backdropHeight);
        int backdropBg1 = "final".equals(form) ? 0x77140022 : "awakened".equals(form) ? 0x662A1400 : 0x660E0022;
        int backdropBg2 = "final".equals(form) ? 0xBB33003B : "awakened".equals(form) ? 0x99482200 : 0x991C003B;
        graphics.fillGradient(backdropX, backdropY, backdropX + backdropWidth, backdropY + backdropHeight, backdropBg1, backdropBg2);
        graphics.nextStratum();
        drawFloatingDragonSparks(graphics, backdropX, backdropY, backdropWidth, backdropHeight, time, form);
        graphics.disableScissor();

        // 2. 双重逆向旋转龙威魔法阵与外层流彩轨道框
        graphics.nextStratum();
        drawDualDragonMagicCircles(graphics, backdropX, backdropY, backdropWidth, backdropHeight, time, form);
        renderWhiteOrbitBorder(graphics, outerX, outerY, outerWidth, outerHeight, time, form);

        // 3. 主框体背景与科技叠加特效
        graphics.nextStratum();
        int frameBg1 = "final".equals(form) ? 0xE2200028 : "awakened".equals(form) ? 0xE22E1A00 : 0xE2140828;
        int frameBg2 = "final".equals(form) ? 0xF5380040 : "awakened".equals(form) ? 0xF54A2800 : 0xF5200C3C;
        graphics.fillGradient(x, y, x + width, y + height, frameBg1, frameBg2);

        graphics.nextStratum();
        graphics.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);
        graphics.fillGradient(x + 2, y + 2, x + width - 2, y + height - 2, 0xC40A0418, 0xEC1A0830);
        graphics.fillGradient(x + 4, y + 4, x + width - 4, y + 46, 0x981C0B36, 0x301C0B36);
        graphics.fillGradient(x + 5, y + 145, x + width - 5, y + height - 5, 0x541C0B36, 0xCD1C0B36);
        graphics.nextStratum();
        drawTechHexGrid(graphics, x + 2, y + 2, width - 4, height - 4, time, form);
        drawMouseSpotlight(graphics, x + 2, y + 2, width - 4, height - 4, mouseX, mouseY, time, form);
        drawHologramScanline(graphics, x + 2, y + 2, width - 4, height - 4, time, form);
        drawMovingDragonAccents(graphics, x, y, width, height, time, form);
        drawDragonSoulCore(graphics, x + width - 22.0F, y + 22.0F, 9.0F, time, form);
        drawOrbitRunes(graphics, x + width / 2.0F, y + height - 9.0F, width - 34.0F, time, form);
        drawMouseSlashTrails(graphics, x, y, width, height, mouseX, mouseY, time, form);
        graphics.disableScissor();

        // 4. 龙爪边角框与渐变脉冲边框
        graphics.nextStratum();
        drawDragonCornerClaws(graphics, x, y, width, height, time, form);
        renderGradientBorder(graphics, x, y, width, height, time, form);
    }

    private static void renderContent(
            GuiGraphicsExtractor graphics,
            Font font,
            ItemStack stack,
            List<EnchantmentLine> enchantments,
            int x,
            int y,
            int width,
            int height,
            int contentWidth,
            int enchantRowHeight,
            float time,
            String form
    ) {
        int center = x + width / 2;
        String titleKey = "final".equals(form)
                ? "item.annihilationbladeex.nightfall_dragon.tooltip.title.final"
                : "awakened".equals(form)
                        ? "item.annihilationbladeex.nightfall_dragon.tooltip.title.awakened"
                        : "item.annihilationbladeex.nightfall_dragon.tooltip.title.sealed";

        Component itemName = Component.translatable(titleKey);
        int primaryColor = "final".equals(form) ? 0xFF0055 : "awakened".equals(form) ? 0xFFB703 : 0xE0AAFF;
        int glowColor = "final".equals(form) ? 0x9000FF : "awakened".equals(form) ? 0xFB8500 : 0x7B2CBF;

        drawSpectralDragonTitle(graphics, font, itemName, center - font.width(itemName) / 2, y + 8, primaryColor, glowColor, time, form);

        String subtitle = I18n.get("item.annihilationbladeex.nightfall_dragon.tooltip.subtitle");
        GuiRenderHelper.drawGlowText(graphics, font, Component.literal(subtitle), center - font.width(subtitle) / 2, y + 20, 0xFFF2FEFF, glowColor, 0.35F);

        drawHorizontalSeparator(graphics, x + 12, y + 32, contentWidth, time, form);
        drawDragonEnergyMeter(graphics, x + 12, y + 35, contentWidth, 3, time, form);

        int currentY = y + 42;

        drawSectionHeader(graphics, font, x + 12, currentY, I18n.get("item.annihilationbladeex.nightfall_dragon.tooltip.section.form"), primaryColor);
        currentY += 12;
        String formTextKey = "final".equals(form)
                ? "item.annihilationbladeex.nightfall_dragon.tooltip.form.final"
                : "awakened".equals(form)
                        ? "item.annihilationbladeex.nightfall_dragon.tooltip.form.awakened"
                        : "item.annihilationbladeex.nightfall_dragon.tooltip.form.sealed";
        String rankTag = "final".equals(form) ? "[RANK: SSS / 龙皇灭世]" : "awakened".equals(form) ? "[RANK: SS / 龙魂觉醒]" : "[RANK: S / 暗龙封印]";
        drawFormBadgeCard(graphics, font, x + 12, currentY, contentWidth, 16, I18n.get(formTextKey), rankTag, primaryColor, 0x55000000);
        currentY += 20;

        drawSectionHeader(graphics, font, x + 12, currentY, I18n.get("item.annihilationbladeex.nightfall_dragon.tooltip.section.sa"), primaryColor);
        currentY += 12;
        String saName = "final".equals(form)
                ? "神陨降临 (Cosmic Nightfall Descent)"
                : "awakened".equals(form) ? "鳞之卫 (Scale Guard)" : "夜陨次元斩 (Nightfall Judgement Cut)";
        drawCard(graphics, font, x + 12, currentY, contentWidth, 16, saName, 0xF2FEFF, 0x55000000);
        currentY += 20;

        drawSectionHeader(graphics, font, x + 12, currentY, I18n.get("item.annihilationbladeex.nightfall_dragon.tooltip.section.effects"), primaryColor);
        currentY += 12;
        renderDragonChipGrid(graphics, font, x + 12, currentY, contentWidth);
        currentY += 40;

        drawSectionHeader(graphics, font, x + 12, currentY, I18n.get("item.annihilationbladeex.nightfall_dragon.tooltip.section.stats"), primaryColor);
        currentY += 12;
        BladeStats stats = GuiRenderHelper.readBladeStats(stack);
        renderStatGrid(graphics, font, x + 12, currentY, contentWidth, stats, form);
        currentY += 28;

        drawSectionHeader(graphics, font, x + 12, currentY, I18n.get("item.annihilationbladeex.nightfall_dragon.tooltip.section.enchantments"), primaryColor);
        currentY += 12;
        renderEnchantmentGrid(graphics, font, enchantments, x + 12, currentY, contentWidth, enchantRowHeight, primaryColor);
        drawStatusRibbon(graphics, font, x + 12, y + height - 15, contentWidth, time, form);
    }

    private static void drawDragonSoulCore(GuiGraphicsExtractor graphics, float cx, float cy, float radius, float time, String form) {
        int core = sampleFormSpectrum(form, time * 0.55F);
        float pulse = 0.82F + 0.18F * Mth.sin(time * 4.0F);
        GuiRenderHelper.drawCircle(graphics, cx, cy, radius + 3.0F + pulse, time * 0.9F, 18, 0.8F, GuiRenderHelper.withAlpha(core, 0.35F));
        GuiRenderHelper.drawCircle(graphics, cx, cy, radius + 1.0F, -time * 1.3F, 12, 1.0F, GuiRenderHelper.withAlpha(0xFFFFFF, 0.55F));
        GuiRenderHelper.drawCircle(graphics, cx, cy, radius * 0.52F, time * 1.8F, 8, 1.5F, GuiRenderHelper.withAlpha(core, 0.9F));
        GuiRenderHelper.drawQuad(graphics, cx - 2.0F, cy - 2.0F, cx + 2.0F, cy + 2.0F, GuiRenderHelper.withAlpha(0xFFFFFF, 0.95F));
    }

    private static void drawOrbitRunes(GuiGraphicsExtractor graphics, float cx, float cy, float width, float time, String form) {
        float half = width * 0.5F;
        int color = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.35F), 0.7F);
        GuiRenderHelper.drawBeam(graphics, cx - half, cy, cx + half, cy, 0.55F, GuiRenderHelper.withAlpha(color, 0.0F), color);
        GuiRenderHelper.drawBeam(graphics, cx + half, cy, cx - half, cy, 0.55F, color, GuiRenderHelper.withAlpha(color, 0.0F));
        for (int i = 0; i < 5; i++) {
            float px = cx - half + ((time * 28.0F + i * width * 0.23F) % width);
            float size = 1.2F + (i % 2) * 0.8F;
            int rune = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.2F + i), 0.8F);
            GuiRenderHelper.drawQuad(graphics, px - size, cy - size, px + size, cy + size, rune);
        }
    }

    private static void drawMouseSlashTrails(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY,
            float time,
            String form
    ) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return;
        }
        float sweep = (time * 1.8F) % (Mth.PI * 2.0F);
        float radius = Math.min(width, height) * 0.38F;
        int color = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.6F), 0.22F);
        for (int i = 0; i < 3; i++) {
            float angle = sweep + i * 0.22F;
            float x1 = mouseX + Mth.cos(angle) * (radius - i * 7.0F);
            float y1 = mouseY + Mth.sin(angle) * (radius - i * 7.0F);
            float x2 = mouseX + Mth.cos(angle + 0.34F) * (radius + 10.0F);
            float y2 = mouseY + Mth.sin(angle + 0.34F) * (radius + 10.0F);
            GuiRenderHelper.drawBeam(graphics, x1, y1, x2, y2, 0.7F + i * 0.35F, color, GuiRenderHelper.withAlpha(0xFFFFFF, 0.0F));
        }
    }

    private static void drawStatusRibbon(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, float time, String form) {
        int color = sampleFormSpectrum(form, time * 0.4F + 2.0F);
        String state = "final".equals(form)
                ? "NIGHTFALL // APOCALYPSE"
                : "awakened".equals(form) ? "NIGHTFALL // AWAKENED" : "NIGHTFALL // SEALED";
        graphics.text(font, state, x + 2, y, GuiRenderHelper.withAlpha(color, 0.72F), false);
        int ticks = 9;
        int start = x + width - ticks * 4;
        for (int i = 0; i < ticks; i++) {
            float phase = (time * 3.0F + i * 0.7F) % (Mth.PI * 2.0F);
            int barColor = GuiRenderHelper.withAlpha(
                    sampleFormSpectrum(form, i * 0.7F + time * 0.3F),
                    0.3F + 0.5F * (Mth.sin(phase) * 0.5F + 0.5F)
            );
            graphics.fill(start + i * 4, y + 1, start + i * 4 + 2, y + 5, barColor);
        }
    }

    private static void drawTechHexGrid(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time, String form) {
        int step = 14;
        int color = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.1F), 0.06F);
        for (int gx = x; gx < x + width; gx += step) {
            for (int gy = y; gy < y + height; gy += step) {
                GuiRenderHelper.drawQuad(graphics, gx, gy, gx + 2, gy + 2, color);
            }
        }
    }

    private static void drawMouseSpotlight(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY,
            float time,
            String form
    ) {
        if (mouseX < x - 20 || mouseX > x + width + 20 || mouseY < y - 20 || mouseY > y + height + 20) {
            return;
        }
        float radius = 55.0F;
        float alpha = 0.14F + (float) Math.sin(time * 4.0F) * 0.04F;
        int lightColor = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.3F), alpha);
        GuiRenderHelper.drawCircle(graphics, mouseX, mouseY, radius, 0.0F, 16, 2.0F, lightColor);
        GuiRenderHelper.drawCircle(graphics, mouseX, mouseY, radius * 0.5F, 0.0F, 12, 1.2F, lightColor);
    }

    private static void drawHologramScanline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time, String form) {
        float scanY = y + ((time * 70.0F) % height);
        int laserColor = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.4F), 0.45F);
        int trailColor = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.4F), 0.10F);

        GuiRenderHelper.drawQuad(graphics, x, scanY, x + width, scanY + 1.5F, laserColor);
        float trailTop = Math.max(y, scanY - 8.0F);
        GuiRenderHelper.drawQuad(graphics, x, trailTop, x + width, trailTop, x + width, scanY, x, scanY, trailColor, trailColor, laserColor, laserColor);
    }

    private static void drawFloatingDragonSparks(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time, String form) {
        int count = 22;
        for (int i = 0; i < count; i++) {
            float pseudoX = (float) Math.sin(i * 1.7F + 0.3F) * 0.5F + 0.5F;
            float speed = 8.0F + (i % 5) * 4.0F;
            float sy = (y + height) - ((time * speed + i * 22.0F) % (height + 20.0F));
            float sx = x + pseudoX * width;

            float alpha = Mth.clamp((float) Math.sin(time * 3.0F + i), 0.2F, 0.85F);
            int color = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, i * 0.5F + time * 0.2F), alpha);
            float size = 1.2F + (i % 3) * 0.6F;
            GuiRenderHelper.drawQuad(graphics, sx - size, sy - size, sx + size, sy + size, color);
        }
    }

    private static void drawDragonCornerClaws(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time, String form) {
        float clawLen = 12.0F;
        float clawThick = 2.2F;
        int color = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.3F), 1.0F);

        // 左上角
        GuiRenderHelper.drawQuad(graphics, x - 2, y - 2, x + clawLen, y - 2 + clawThick, color);
        GuiRenderHelper.drawQuad(graphics, x - 2, y - 2, x - 2 + clawThick, y + clawLen, color);

        // 右上角
        GuiRenderHelper.drawQuad(graphics, x + width + 2 - clawLen, y - 2, x + width + 2, y - 2 + clawThick, color);
        GuiRenderHelper.drawQuad(graphics, x + width + 2 - clawThick, y - 2, x + width + 2, y + clawLen, color);

        // 左下角
        GuiRenderHelper.drawQuad(graphics, x - 2, y + height + 2 - clawThick, x + clawLen, y + height + 2, color);
        GuiRenderHelper.drawQuad(graphics, x - 2, y + height + 2 - clawLen, x - 2 + clawThick, y + height + 2, color);

        // 右下角
        GuiRenderHelper.drawQuad(graphics, x + width + 2 - clawLen, y + height + 2 - clawThick, x + width + 2, y + height + 2, color);
        GuiRenderHelper.drawTriangle(
                graphics,
                x + width + 2 - clawThick, y + height + 2 - clawLen,
                x + width + 2, y + height + 2 - clawLen,
                x + width + 2, y + height + 2,
                color
        );
    }

    private static void drawDualDragonMagicCircles(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time, String form) {
        float cx = x + width / 2.0F;
        float cy = y + height / 2.0F;
        float r1 = Math.max(width, height) * 0.54F + 12.0F;
        float r2 = r1 * 0.72F;

        float spin1 = time * 0.2F;
        float spin2 = -time * 0.35F;

        int color1 = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.15F), 0.22F);
        int color2 = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.25F + 3.0F), 0.18F);

        GuiRenderHelper.drawCircle(graphics, cx, cy, r1, spin1, 36, 0.8F, color1);
        GuiRenderHelper.drawPolygon(graphics, cx, cy, r1 * 0.92F, 12, spin1 + Mth.PI / 12.0F, 0.5F, color1);

        GuiRenderHelper.drawCircle(graphics, cx, cy, r2, spin2, 24, 0.6F, color2);
        GuiRenderHelper.drawPolygon(graphics, cx, cy, r2, 6, spin2, 0.5F, color2);
    }

    private static void drawDragonEnergyMeter(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time, String form) {
        GuiRenderHelper.drawQuad(graphics, x, y, x + width, y + height, 0x55000000);
        int segments = 10;
        float segWidth = (float) width / segments;
        for (int i = 0; i < segments; i++) {
            float phase = (time * 2.0F + i * 0.4F) % (Mth.PI * 2.0F);
            float glow = (float) Math.sin(phase) * 0.5F + 0.5F;
            int color = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, i * 0.6F + time * 0.5F), 0.4F + glow * 0.6F);
            float sx = x + i * segWidth;
            float ex = sx + segWidth - 1.0F;
            GuiRenderHelper.drawQuad(graphics, sx, y, ex, y + height, color);
        }
    }

    private static void drawSpectralDragonTitle(
            GuiGraphicsExtractor graphics,
            Font font,
            Component title,
            int x,
            int y,
            int primaryColor,
            int glowColor,
            float time,
            String form
    ) {
        if ("final".equals(form)) {
            float glitchX = (float) Math.sin(time * 15.0F) * 1.2F;
            graphics.text(font, title, (int) (x + glitchX + 1), y, 0x8800FFFF, false);
            graphics.text(font, title, (int) (x - glitchX - 1), y, 0x88FF0055, false);
        }
        graphics.text(font, title, x + 2, y + 2, 0x88000000, false);
        GuiRenderHelper.drawGlowText(graphics, font, title, x, y, GuiRenderHelper.withAlpha(primaryColor, 1.0F), glowColor, 0.35F);
    }

    private static void drawFormBadgeCard(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            String formText,
            String rankTag,
            int color,
            int bgColor
    ) {
        graphics.fillGradient(x, y, x + width, y + height, bgColor, bgColor | 0xFF000000);
        int borderColor = GuiRenderHelper.withAlpha(color, 0.65F);
        GuiRenderHelper.drawQuad(graphics, x, y, x + width, y + 1, borderColor);
        GuiRenderHelper.drawQuad(graphics, x, y + height - 1, x + width, y + height, borderColor);

        int fullColor = GuiRenderHelper.withAlpha(color, 1.0F);
        graphics.text(font, formText, x + 6, y + (height - 8) / 2, fullColor, false);
        int rankWidth = font.width(rankTag);
        graphics.text(font, rankTag, x + width - rankWidth - 6, y + (height - 8) / 2, GuiRenderHelper.withAlpha(color, 0.85F), false);
    }

    private static void renderDragonChipGrid(GuiGraphicsExtractor graphics, Font font, int x, int y, int width) {
        int chipWidth = (width - 8) / 3;
        int chipHeight = 11;
        for (int i = 0; i < DRAGON_CHIPS.length; i++) {
            DragonChip chip = DRAGON_CHIPS[i];
            int row = i / 3;
            int col = i % 3;
            int cx = x + col * (chipWidth + 4);
            int cy = y + row * (chipHeight + 3);
            drawChip(graphics, font, cx, cy, chipWidth, chipHeight, I18n.get(chip.key()), chip.color());
        }
    }

    private static void renderStatGrid(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, BladeStats stats, String form) {
        int cellWidth = (width - 8) / 3;
        int cellHeight = 11;
        String formTitle = "final".equals(form) ? "神陨" : "awakened".equals(form) ? "觉醒" : "封印";
        StatCell[] cells = new StatCell[]{
                new StatCell("item.annihilationbladeex.infinity_stellaris.tooltip.stat.kills", String.valueOf(stats.killCount()), 0xFF0055),
                new StatCell("item.annihilationbladeex.infinity_stellaris.tooltip.stat.souls", String.valueOf(stats.proudSoul()), 0xFFB703),
                new StatCell("item.annihilationbladeex.infinity_stellaris.tooltip.stat.refine", String.valueOf(stats.refine()), 0x00FFFF),
                new StatCell("item.annihilationbladeex.infinity_stellaris.tooltip.stat.attack", "+22.0", 0xE0AAFF),
                new StatCell("item.annihilationbladeex.infinity_stellaris.tooltip.stat.durability", "∞ 无双", 0x7B2CBF),
                new StatCell("item.annihilationbladeex.nightfall_dragon.tooltip.stat.form_power", formTitle, "final".equals(form) ? 0xFF0055 : 0xFFB703)
        };

        for (int i = 0; i < cells.length; i++) {
            int row = i / 3;
            int col = i % 3;
            int cx = x + col * (cellWidth + 4);
            int cy = y + row * (cellHeight + 3);
            drawStatCell(graphics, font, cx, cy, cellWidth, cellHeight, cells[i]);
        }
    }

    private static void renderEnchantmentGrid(
            GuiGraphicsExtractor graphics,
            Font font,
            List<EnchantmentLine> enchantments,
            int x,
            int y,
            int width,
            int rowHeight,
            int primaryColor
    ) {
        if (enchantments.isEmpty()) {
            graphics.text(font, Component.literal("§7[无附魔回路]"), x, y, 0xFF888888, false);
            return;
        }

        int columnWidth = (width - 8) / 2;
        for (int i = 0; i < enchantments.size(); i++) {
            int row = i / 2;
            int col = i % 2;
            int ex = x + col * (columnWidth + 8);
            int ey = y + row * rowHeight;
            String text = font.plainSubstrByWidth(enchantments.get(i).text(), columnWidth);
            graphics.text(font, text, ex, ey, GuiRenderHelper.withAlpha(primaryColor, 0.9F), false);
        }
    }

    private static void drawCard(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height, String text, int color, int bgColor) {
        graphics.fillGradient(x, y, x + width, y + height, bgColor, bgColor | 0xFF000000);
        GuiRenderHelper.drawQuad(graphics, x, y, x + width, y + 1, GuiRenderHelper.withAlpha(color, 0.5F));
        GuiRenderHelper.drawQuad(graphics, x, y + height - 1, x + width, y + height, GuiRenderHelper.withAlpha(color, 0.5F));
        graphics.text(font, text, x + 6, y + (height - 8) / 2, GuiRenderHelper.withAlpha(color, 1.0F), false);
    }

    private static void drawChip(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height, String text, int color) {
        graphics.fill(x, y, x + width, y + height, 0x44000000);
        int fullColor = GuiRenderHelper.withAlpha(color, 1.0F);
        GuiRenderHelper.drawQuad(graphics, x, y, x + 2, y + height, fullColor);
        String trimmed = font.plainSubstrByWidth(text, width - 6);
        graphics.text(font, trimmed, x + 4, y + (height - 8) / 2, fullColor, false);
    }

    private static void drawStatCell(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height, StatCell cell) {
        graphics.fill(x, y, x + width, y + height, 0x33000000);
        String label = I18n.get(cell.labelKey());
        graphics.text(font, label, x + 3, y + (height - 8) / 2, 0xFFAAAAAA, false);
        int valWidth = font.width(cell.value());
        graphics.text(font, cell.value(), x + width - valWidth - 3, y + (height - 8) / 2, GuiRenderHelper.withAlpha(cell.color(), 1.0F), false);
    }

    private static void drawSectionHeader(GuiGraphicsExtractor graphics, Font font, int x, int y, String title, int color) {
        graphics.text(font, "▌ " + title, x, y, GuiRenderHelper.withAlpha(color, 1.0F), false);
    }

    private static void drawHorizontalSeparator(GuiGraphicsExtractor graphics, int x, int y, int width, float time, String form) {
        int colorA = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.2F), 1.0F);
        int colorB = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.2F + 2.0F), 1.0F);
        GuiRenderHelper.drawQuad(graphics, x, y, x + width, y, x + width, y + 1, x, y + 1, colorA, colorB, colorB, colorA);
    }

    private static void drawMovingDragonAccents(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time, String form) {
        for (int i = 0; i < 6; i++) {
            float phase = (time * 0.08F + i * 0.16F) % 1.0F;
            float sx = x - 28.0F + phase * (width + 56.0F);
            int colorA = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, i * 0.6F + time * 0.2F), 0.15F);
            int colorB = GuiRenderHelper.withAlpha(0xFFFFFF, 0.06F);
            GuiRenderHelper.drawBeam(graphics, sx, y + height + 10.0F, sx + 50.0F, y - 12.0F, 1.0F, colorA, colorB);
        }
    }

    private static void renderWhiteOrbitBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time, String form) {
        int glow = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.15F), 0.12F);
        int base = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, time * 0.15F), 0.35F);
        GuiRenderHelper.drawBeam(graphics, x, y, x + width, y, 2.4F, glow, glow);
        GuiRenderHelper.drawBeam(graphics, x, y + height, x + width, y + height, 2.4F, glow, glow);
        GuiRenderHelper.drawBeam(graphics, x, y, x, y + height, 2.4F, glow, glow);
        GuiRenderHelper.drawBeam(graphics, x + width, y, x + width, y + height, 2.4F, glow, glow);
        GuiRenderHelper.drawBeam(graphics, x, y, x + width, y, 0.7F, base, base);
        GuiRenderHelper.drawBeam(graphics, x, y + height, x + width, y + height, 0.7F, base, base);
        GuiRenderHelper.drawBeam(graphics, x, y, x, y + height, 0.7F, base, base);
        GuiRenderHelper.drawBeam(graphics, x + width, y, x + width, y + height, 0.7F, base, base);
    }

    private static void renderGradientBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time, String form) {
        int segments = 16;
        float spread = 0.8F;
        float thickness = 1.2F;
        float drift = time * 0.1F;
        float alpha = 0.85F;
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;
            int c1 = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, t1 * 6.0F + drift), alpha);
            int c2 = GuiRenderHelper.withAlpha(sampleFormSpectrum(form, t2 * 6.0F + drift), alpha);
            float sx = x + width * t1;
            float ex = x + width * t2;
            GuiRenderHelper.drawQuad(graphics, sx, y - spread, ex, y - spread, ex, y - spread + thickness, sx, y - spread + thickness, c1, c2, c2, c1);
            GuiRenderHelper.drawQuad(graphics, sx, y + height + spread, ex, y + height + spread, ex, y + height + spread - thickness, sx, y + height + spread - thickness, c2, c1, c1, c2);
        }
    }

    private static int sampleFormSpectrum(String form, float position) {
        int[] spectrum = "final".equals(form) ? FINAL_SPECTRUM : "awakened".equals(form) ? AWAKENED_SPECTRUM : SEALED_SPECTRUM;
        return GuiRenderHelper.sampleSpectrum(spectrum, position);
    }

    private record DragonChip(String key, int color) {
    }

    private record StatCell(String labelKey, String value, int color) {
    }
}
