package qwq.qingyi.annihilationblade.client.annihilation_blade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.annihilation_blade.AnnihilationBladeDefinitions;
import qwq.qingyi.annihilationblade.annihilation_blade.item.ItemAnnihilationBlade;
import qwq.qingyi.annihilationblade.client.render.GuiRenderHelper;
import qwq.qingyi.annihilationblade.client.render.GuiRenderHelper.BladeStats;
import qwq.qingyi.annihilationblade.common.SlashBladeStateSupport;
import qwq.qingyi.annihilationblade.registry.ModItems;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 湮灭之刃（终焉之契）专属宇宙黑洞视觉 Tooltip 渲染器（适配 MC 26.1.2 GuiGraphicsExtractor）。
 */
public final class TerminusTooltipRenderer {
    private static final Identifier BLACK_HOLE_BACKGROUND =
            AnnihilationBladeex.prefix("textures/gui/black_hole_asset_v1_cropped.png");
    private static final Identifier SPATIAL_FRACTURE_ID = AnnihilationBladeex.prefix("spatial_fracture");
    private static final List<Identifier> GOD_SPECIAL_EFFECTS = List.of(
            AnnihilationBladeex.prefix("dankong"),
            AnnihilationBladeex.prefix("world_rift"),
            AnnihilationBladeex.prefix("terminus_echo"),
            AnnihilationBladeex.prefix("void_dominion"),
            AnnihilationBladeex.prefix("causality_collapse"),
            AnnihilationBladeex.prefix("starless_judgement"),
            AnnihilationBladeex.prefix("phantom_judgement"),
            AnnihilationBladeex.prefix("abyssal_decree")
    );

    private static final int BLACK_HOLE_TEXTURE_WIDTH = 1600;
    private static final int BLACK_HOLE_TEXTURE_HEIGHT = 1000;
    private static final int VOID_BLACK = -267385318;
    private static final int RIFT_MAGENTA = -640598785;
    private static final int COSMIC_CYAN = -648484865;
    private static final int TERMINUS_GOLD = -385887379;
    private static final int HORIZON_ORANGE = -117459101;
    private static final int STARFALL_BLUE = -395449345;
    private static final int PALE_STAR = -252121089;
    private static final int[] BORDER_GRADIENT = new int[]{
            HORIZON_ORANGE, PALE_STAR, STARFALL_BLUE, RIFT_MAGENTA, TERMINUS_GOLD, COSMIC_CYAN
    };
    private static final int WIDTH = 286;
    private static final int MIN_WIDTH = 190;
    private static final String[] DESCRIPTION_KEYS = new String[]{
            "item.annihilationbladeex.desc.line1",
            "item.annihilationbladeex.desc.line2",
            "item.annihilationbladeex.desc.line3",
            "item.annihilationbladeex.desc.line4"
    };
    private static final int[] EFFECT_COLOR_POOL = new int[]{
            RIFT_MAGENTA, COSMIC_CYAN, TERMINUS_GOLD, HORIZON_ORANGE, STARFALL_BLUE, PALE_STAR
    };

    private TerminusTooltipRenderer() {
    }

    public static boolean shouldRender(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof ItemAnnihilationBlade || stack.is(ModItems.ANNIHILATION_BLADE)) {
            return true;
        }
        if (AnnihilationBladeDefinitions.ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return true;
        }

        Optional<ISlashBladeState> stateOpt = SlashBladeStateSupport.state(stack);
        if (stateOpt.isEmpty()) {
            return false;
        }
        ISlashBladeState state = stateOpt.get();
        if (AnnihilationBladeDefinitions.DESCRIPTION_ID.equals(state.getTranslationKey())) {
            return true;
        }
        if (SPATIAL_FRACTURE_ID.equals(state.getSlashArtsKey())) {
            return true;
        }
        return state.getSpecialEffects().stream().anyMatch(GOD_SPECIAL_EFFECTS::contains);
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

        List<FormattedCharSequence> descriptionLines = wrapDescription(font, contentWidth);
        List<FormattedCharSequence> enchantmentLines = wrapEnchantments(font, stack, contentWidth);
        BladeStats bladeStats = GuiRenderHelper.readBladeStats(stack);
        List<Identifier> activeEffects = getActiveSpecialEffects(stack);
        int seCount = activeEffects.size();
        int columns = Math.max(1, (width - 24) / 84);
        if (seCount > 0 && seCount < columns) {
            columns = seCount;
        }

        int rows = seCount == 0 ? 0 : (seCount + columns - 1) / columns;
        int rowHeight = 16;
        int totalChipHeight = seCount > 0 ? rows * rowHeight - 4 : 0;
        int descriptionHeight = descriptionLines.size() * 10;
        int enchantmentHeight = enchantmentLines.isEmpty() ? 0 : 14 + enchantmentLines.size() * 10;
        int enchantYOffset = 161 + totalChipHeight + (seCount > 0 ? 10 : 0);
        int height = enchantYOffset + enchantmentHeight + descriptionHeight + 10;

        int x = mouseX + 12;
        int y = mouseY - 14;
        if (x + width > screenWidth - 6) {
            x = mouseX - width - 18;
        }
        if (x < 6) {
            x = 6;
        }
        if (y + height > screenHeight - 6) {
            y = screenHeight - height - 6;
        }
        if (y < 6) {
            y = 6;
        }

        float time = (float) (System.currentTimeMillis() % 120000L) / 1000.0F;
        renderFrame(graphics, x, y, width, height, time);
        graphics.nextStratum();
        renderTerminusSeals(graphics, x, y, width, height, time);
        graphics.nextStratum();
        renderContent(graphics, font, stack, vanillaLines, descriptionLines, enchantmentLines, bladeStats, activeEffects, x, y, width, height, time);
    }

    private static List<Identifier> getActiveSpecialEffects(ItemStack stack) {
        Collection<Identifier> rawEffects = SlashBladeStateSupport.state(stack)
                .map(ISlashBladeState::getSpecialEffects)
                .orElse(List.of());
        List<Identifier> effects = new ArrayList<>();
        for (Identifier id : rawEffects) {
            if (GOD_SPECIAL_EFFECTS.contains(id)) {
                effects.add(id);
            }
        }
        return effects.isEmpty() ? GOD_SPECIAL_EFFECTS : effects;
    }

    private static Component getEffectDisplayName(Identifier effectId) {
        return Component.translatable("se." + effectId.getNamespace() + "." + effectId.getPath());
    }

    private static List<FormattedCharSequence> wrapDescription(Font font, int contentWidth) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String key : DESCRIPTION_KEYS) {
            lines.addAll(font.split(Component.translatable(key), contentWidth));
        }
        return lines;
    }

    private static List<FormattedCharSequence> wrapEnchantments(Font font, ItemStack stack, int contentWidth) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) {
            return List.of();
        }

        List<Object2IntMap.Entry<Holder<Enchantment>>> entries = new ArrayList<>(enchantments.entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().getRegisteredName()));
        List<FormattedCharSequence> lines = new ArrayList<>();
        MutableComponent current = Component.empty();
        int currentWidth = 0;

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : entries) {
            Component name = Enchantment.getFullname(entry.getKey(), entry.getIntValue());
            int separatorWidth = currentWidth == 0 ? 0 : font.width(" / ");
            int nameWidth = font.width(name);
            if (currentWidth > 0 && currentWidth + separatorWidth + nameWidth > contentWidth) {
                lines.add(current.getVisualOrderText());
                current = Component.empty();
                currentWidth = 0;
                separatorWidth = 0;
            }

            if (currentWidth > 0) {
                current.append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY));
                currentWidth += separatorWidth;
            }

            current.append(name.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            currentWidth += nameWidth;
        }

        if (currentWidth > 0) {
            lines.add(current.getVisualOrderText());
        }
        return lines;
    }

    private static void renderFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        graphics.fillGradient(x, y, x + width, y + height, VOID_BLACK, -200932848);
        graphics.nextStratum();
        renderBlackHoleArtwork(graphics, x, y, width, height, time);
        graphics.nextStratum();
        graphics.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);

        for (int i = 0; i < 9; i++) {
            float phase = (time * 0.11F + i * 0.137F) % 1.0F;
            float sx = x - 28.0F + phase * (width + 56.0F);
            float alpha = 0.18F + 0.12F * Mth.sin(time * 1.7F + i);
            GuiRenderHelper.drawBeam(
                    graphics,
                    sx, y + height + 14.0F,
                    sx + 70.0F, y - 18.0F,
                    1.4F,
                    GuiRenderHelper.scaleAlpha(COSMIC_CYAN, alpha),
                    GuiRenderHelper.scaleAlpha(RIFT_MAGENTA, alpha * 0.65F)
            );
        }

        for (int i = 0; i < 34; i++) {
            float seed = i * 12.9898F;
            float px = x + 8.0F + noise(seed) * (width - 16.0F);
            float py = y + 8.0F + noise(seed + 8.0F) * (height - 16.0F);
            float pulse = 0.45F + 0.55F * Mth.sin(time * 2.3F + i * 0.71F);
            int color = i % 5 == 0 ? TERMINUS_GOLD : (i % 2 == 0 ? COSMIC_CYAN : RIFT_MAGENTA);
            GuiRenderHelper.drawQuad(
                    graphics,
                    px, py,
                    px + 1.0F + pulse, py + 1.0F + pulse,
                    GuiRenderHelper.scaleAlpha(color, 0.18F + pulse * 0.22F)
            );
        }

        graphics.disableScissor();
        graphics.nextStratum();
        renderExpandingGradientBorder(graphics, x, y, width, height, time);
    }

    private static void renderBlackHoleArtwork(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        float aspect = 1.6F;
        float imageWidthF = Math.min(graphics.guiWidth() - 4.0F, width * 1.5F);
        float imageHeightF = imageWidthF / aspect;
        if (imageHeightF > graphics.guiHeight() - 4.0F) {
            imageHeightF = graphics.guiHeight() - 4.0F;
            imageWidthF = imageHeightF * aspect;
        }

        int imageWidth = Math.round(imageWidthF);
        int imageHeight = Math.round(imageHeightF);
        int imageX = x - (imageWidth - width) / 2 + (int) (Mth.sin(time * 0.22F) * 3.0F);
        int imageY = y + Math.round(height * 0.03F) - Math.round(imageHeight * 0.2F) + (int) (Mth.cos(time * 0.19F) * 3.0F);
        imageX = Mth.clamp(imageX, 2, Math.max(2, graphics.guiWidth() - imageWidth - 2));
        imageY = Mth.clamp(imageY, 2, Math.max(2, graphics.guiHeight() - imageHeight - 2));

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BLACK_HOLE_BACKGROUND,
                imageX,
                imageY,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                BLACK_HOLE_TEXTURE_WIDTH,
                BLACK_HOLE_TEXTURE_HEIGHT,
                BLACK_HOLE_TEXTURE_WIDTH,
                BLACK_HOLE_TEXTURE_HEIGHT,
                ARGB.white(0.74F)
        );
        graphics.nextStratum();
        graphics.fillGradient(x + 3, y + 3, x + width - 3, y + height - 3, 402981135, -939130352);
        graphics.fillGradient(x + 5, y + height / 2, x + width - 5, y + height - 5, 939524096, -1207959546);
    }

    private static void renderExpandingGradientBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        int segments = 22;
        int layers = 7;
        float drift = time * 0.085F;

        for (int layer = layers; layer >= 1; layer--) {
            float t = (float) layer / layers;
            float spread = 2.0F + layer * 2.6F;
            float thickness = 1.4F + layer * 0.48F;
            float alpha = 0.05F + (1.0F - t) * 0.34F;
            drawSegmentedBorder(graphics, x, y, width, height, spread, thickness, segments, drift + layer * 0.037F, alpha);
        }

        drawSegmentedBorder(graphics, x, y, width, height, 1.2F, 1.7F, segments, drift + 0.28F, 0.92F);
        drawSegmentedBorder(graphics, x, y, width, height, 4.2F, 1.1F, segments, drift + 0.52F, 0.48F);
        drawCornerGlyphs(graphics, x, y, width, height, time);
        drawBorderCornerBloom(graphics, x, y, width, height, time);
    }

    private static void drawSegmentedBorder(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float spread,
            float thickness,
            int segments,
            float drift,
            float alpha
    ) {
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;
            int topA = gradientColor(t1 + drift, alpha);
            int topB = gradientColor(t2 + drift, alpha);
            int bottomA = gradientColor(0.53F - t1 + drift, alpha * 0.94F);
            int bottomB = gradientColor(0.53F - t2 + drift, alpha * 0.94F);
            float sx = x + width * t1;
            float ex = x + width * t2;
            GuiRenderHelper.drawQuad(
                    graphics, sx, y - spread, ex, y - spread, ex, y - spread + thickness, sx, y - spread + thickness, topA, topB, topB, topA
            );
            GuiRenderHelper.drawQuad(
                    graphics, sx, y + height + spread - thickness, ex, y + height + spread - thickness,
                    ex, y + height + spread, sx, y + height + spread, bottomB, bottomA, bottomA, bottomB
            );
        }

        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;
            int leftA = gradientColor(0.24F + t1 + drift, alpha * 0.82F);
            int leftB = gradientColor(0.24F + t2 + drift, alpha * 0.82F);
            int rightA = gradientColor(0.77F - t1 + drift, alpha * 0.88F);
            int rightB = gradientColor(0.77F - t2 + drift, alpha * 0.88F);
            float sy = y + height * t1;
            float ey = y + height * t2;
            GuiRenderHelper.drawQuad(
                    graphics, x - spread, sy, x - spread + thickness, sy, x - spread + thickness, ey, x - spread, ey, leftA, leftA, leftB, leftB
            );
            GuiRenderHelper.drawQuad(
                    graphics, x + width + spread - thickness, sy, x + width + spread, sy,
                    x + width + spread, ey, x + width + spread - thickness, ey, rightA, rightA, rightB, rightB
            );
        }
    }

    private static void drawBorderCornerBloom(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        float pulse = 0.55F + 0.45F * Mth.sin(time * 2.1F);
        float outer = 11.0F + pulse * 4.0F;
        float inner = 5.0F + pulse * 2.0F;
        drawCornerBloom(graphics, x, y, outer, inner, time, 0.05F);
        drawCornerBloom(graphics, x + width, y, outer, inner, time, 0.3F);
        drawCornerBloom(graphics, x, y + height, outer, inner, time, 0.58F);
        drawCornerBloom(graphics, x + width, y + height, outer, inner, time, 0.82F);
    }

    private static void drawCornerBloom(GuiGraphicsExtractor graphics, float cx, float cy, float outer, float inner, float time, float phase) {
        int core = gradientColor(phase + time * 0.07F, 0.88F);
        int glow = gradientColor(phase + 0.18F + time * 0.05F, 0.25F);
        int spark = gradientColor(phase + 0.37F + time * 0.09F, 0.56F);
        GuiRenderHelper.drawDiamond(graphics, cx, cy, outer, glow);
        GuiRenderHelper.drawDiamond(graphics, cx, cy, inner, core);
        GuiRenderHelper.drawBeam(graphics, cx - outer * 1.25F, cy, cx - inner * 0.35F, cy, 0.9F, GuiRenderHelper.scaleAlpha(glow, 0.7F), spark);
        GuiRenderHelper.drawBeam(graphics, cx + inner * 0.35F, cy, cx + outer * 1.25F, cy, 0.9F, spark, GuiRenderHelper.scaleAlpha(glow, 0.7F));
        GuiRenderHelper.drawBeam(graphics, cx, cy - outer * 1.25F, cx, cy - inner * 0.35F, 0.9F, GuiRenderHelper.scaleAlpha(glow, 0.7F), spark);
        GuiRenderHelper.drawBeam(graphics, cx, cy + inner * 0.35F, cx, cy + outer * 1.25F, 0.9F, spark, GuiRenderHelper.scaleAlpha(glow, 0.7F));
    }

    private static void renderTerminusSeals(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        float mainCx = x + width - 58.0F;
        float mainCy = y + 58.0F;
        float minorCx = x + 45.0F;
        float minorCy = y + 55.0F;
        drawRing(graphics, mainCx, mainCy, 42.0F, 1.1F, GuiRenderHelper.scaleAlpha(RIFT_MAGENTA, 0.38F), time * 0.35F, 48, 0);
        drawRing(graphics, mainCx, mainCy, 35.0F, 0.9F, GuiRenderHelper.scaleAlpha(COSMIC_CYAN, 0.35F), -time * 0.48F, 36, 1);
        drawRotatingHexagram(graphics, mainCx, mainCy, 31.0F, time * 0.82F, 1.25F,
                GuiRenderHelper.scaleAlpha(TERMINUS_GOLD, 0.62F), GuiRenderHelper.scaleAlpha(RIFT_MAGENTA, 0.46F));
        drawRotatingHexagram(graphics, mainCx, mainCy, 21.0F, -time * 1.18F, 0.9F,
                GuiRenderHelper.scaleAlpha(PALE_STAR, 0.4F), GuiRenderHelper.scaleAlpha(COSMIC_CYAN, 0.48F));
        drawOrbitingRunes(graphics, mainCx, mainCy, 39.0F, time, 12);
        drawRing(graphics, minorCx, minorCy, 22.0F, 0.9F, GuiRenderHelper.scaleAlpha(COSMIC_CYAN, 0.36F), -time * 0.65F, 30, 0);
        drawRotatingHexagram(graphics, minorCx, minorCy, 17.0F, -time * 1.05F, 0.95F,
                GuiRenderHelper.scaleAlpha(RIFT_MAGENTA, 0.55F), GuiRenderHelper.scaleAlpha(COSMIC_CYAN, 0.38F));
        drawOblivionCrown(graphics, x + width * 0.5F, y + 63.0F, width * 0.34F, time);
        drawCausalityThreads(graphics, x, y, width, height, time);
    }

    private static void renderContent(
            GuiGraphicsExtractor graphics,
            Font font,
            ItemStack stack,
            List<Component> vanillaLines,
            List<FormattedCharSequence> descriptionLines,
            List<FormattedCharSequence> enchantmentLines,
            BladeStats bladeStats,
            List<Identifier> activeEffects,
            int x,
            int y,
            int width,
            int height,
            float time
    ) {
        int center = x + width / 2;
        Component title = vanillaLines.isEmpty() ? stack.getHoverName() : vanillaLines.get(0);
        drawGlowText(graphics, font, title, center - font.width(title) / 2, y + 10, PALE_STAR, RIFT_MAGENTA);
        graphics.centeredText(
                font,
                Component.literal("TERMINUS // SPACE  CAUSALITY  VOID").withStyle(ChatFormatting.DARK_GRAY),
                center,
                y + 23,
                -1869644377
        );
        graphics.item(stack, x + 12, y + 13);

        int infoY = y + 86;
        drawTag(graphics, font, x + 12, infoY, "SA", Component.translatable("slash_art.annihilationbladeex.spatial_fracture"), -1723133073, COSMIC_CYAN);
        drawTag(graphics, font, x + 12, infoY + 15, "AUTH", Component.translatable("item.annihilationbladeex.passive"), -1721877998, TERMINUS_GOLD);
        drawTag(graphics, font, x + 12, infoY + 30, "DMG", Component.translatable("item.annihilationbladeex.infinite_damage"), -1723594168, RIFT_MAGENTA);

        int statsY = infoY + 45;
        drawBladeStats(graphics, font, x + 12, statsY, width - 24, bladeStats, time);

        int chipY = statsY + 30;
        int seCount = activeEffects.size();
        int columns = Math.max(1, (width - 24) / 84);
        if (seCount > 0 && seCount < columns) {
            columns = seCount;
        }
        int rows = seCount == 0 ? 0 : (seCount + columns - 1) / columns;
        int rowHeight = 16;
        int chipWidth = seCount == 0 ? 0 : (width - 24 - (columns - 1) * 4) / columns;
        int totalChipHeight = seCount > 0 ? rows * rowHeight - 4 : 0;

        for (int i = 0; i < seCount; i++) {
            int col = i % columns;
            int row = i / columns;
            int chipX = x + 12 + col * (chipWidth + 4);
            int currentChipY = chipY + row * rowHeight;
            int color = EFFECT_COLOR_POOL[i % EFFECT_COLOR_POOL.length];
            float phaseOffset = (float) ((i * 2.399F) % (Math.PI * 2.0D)) + noise(i * 17.3F) * 2.0F;
            drawSpecialEffectChip(graphics, font, chipX, currentChipY, chipWidth, getEffectDisplayName(activeEffects.get(i)), color, time + phaseOffset);
        }

        int enchantY = chipY + totalChipHeight + (seCount > 0 ? 10 : 0);
        int enchantmentHeight = drawEnchantments(graphics, font, x + 12, enchantY, width - 24, enchantmentLines);
        int descY = enchantY + (enchantmentHeight == 0 ? 4 : enchantmentHeight + 7);
        graphics.fill(x + 12, descY - 5, x + width - 12, descY - 4, 1430129522);

        for (FormattedCharSequence line : descriptionLines) {
            graphics.text(font, line, x + 13, descY, -1463306018, false);
            descY += 10;
        }

        int pulse = (int) ((width - 26) * (0.45F + 0.55F * (0.5F + 0.5F * Mth.sin(time * 3.4F))));
        graphics.fill(x + 13, y + height - 8, x + width - 13, y + height - 7, 1144916850);
        graphics.fill(x + 13, y + height - 8, x + 13 + pulse, y + height - 7, -654322835);
    }

    private static void drawBladeStats(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, BladeStats stats, float time) {
        int gap = 5;
        int cellWidth = (width - gap * 2) / 3;
        drawStatCell(graphics, font, x, y, cellWidth, "杀敌", GuiRenderHelper.formatNumber(stats.killCount()), COSMIC_CYAN, time);
        drawStatCell(graphics, font, x + cellWidth + gap, y, cellWidth, "耀魂", GuiRenderHelper.formatNumber(stats.proudSoul()), TERMINUS_GOLD, time + 0.7F);
        drawStatCell(graphics, font, x + (cellWidth + gap) * 2, y, width - (cellWidth + gap) * 2, "精炼", GuiRenderHelper.formatNumber(stats.refine()), RIFT_MAGENTA, time + 1.4F);
    }

    private static void drawStatCell(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, String label, String value, int accent, float phase) {
        graphics.fill(x, y, x + width, y + 20, 2115373602);
        graphics.fill(x, y, x + width, y + 1, GuiRenderHelper.scaleAlpha(accent, 0.7F));
        int pulseWidth = 4 + (int) ((width - 8) * (0.45F + 0.55F * (0.5F + 0.5F * Mth.sin(phase * 2.4F))));
        graphics.fill(x + 2, y + 17, x + Math.min(width - 2, pulseWidth), y + 18, GuiRenderHelper.scaleAlpha(accent, 0.72F));
        graphics.text(font, label, x + 4, y + 3, -4676152, false);
        graphics.text(font, value, x + width - font.width(value) - 4, y + 10, -726529, false);
    }

    private static int drawEnchantments(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, List<FormattedCharSequence> enchantmentLines) {
        if (enchantmentLines.isEmpty()) {
            return 0;
        }
        int height = 14 + enchantmentLines.size() * 10;
        graphics.fill(x, y, x + width, y + height, 1746274850);
        graphics.fill(x, y, x + width, y + 1, -1996500115);
        graphics.fill(x, y + height - 1, x + width, y + height, 1717102591);
        graphics.text(font, "附魔回路", x + 5, y + 3, TERMINUS_GOLD, false);
        int lineY = y + 14;
        for (FormattedCharSequence line : enchantmentLines) {
            graphics.text(font, line, x + 5, lineY, -1582849, false);
            lineY += 10;
        }
        return height;
    }

    private static void drawTag(GuiGraphicsExtractor graphics, Font font, int x, int y, String label, Component value, int background, int accent) {
        graphics.fill(x, y, x + 30, y + 11, background);
        graphics.fill(x, y, x + 2, y + 11, accent);
        graphics.text(font, label, x + 5, y + 2, -1253889, false);
        graphics.text(font, value, x + 36, y + 1, -2504214, true);
    }

    private static void drawSpecialEffectChip(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, Component text, int color, float phase) {
        graphics.fill(x, y, x + width, y + 12, -2145383887);
        graphics.fill(x, y, x + width, y + 1, GuiRenderHelper.scaleAlpha(color, 0.54F));
        graphics.fill(x, y + 11, x + width, y + 12, 1428162618);
        float mainPulse = 0.5F + 0.5F * Mth.sin(phase * 2.7F);
        float microPulse = 0.15F * Mth.sin(phase * 8.3F + 1.2F);
        float finalPulse = Mth.clamp(mainPulse + microPulse, 0.0F, 1.0F);
        int fill = 2 + (int) ((width - 4) * finalPulse);
        graphics.fill(x + 2, y + 9, x + Math.min(width - 2, fill), y + 10, GuiRenderHelper.scaleAlpha(color, 0.78F));
        graphics.text(font, text, x + 5, y + 2, -1187073, false);
    }

    private static void drawGlowText(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color, int glow) {
        graphics.text(font, text, x - 1, y, GuiRenderHelper.scaleAlpha(glow, 0.45F), false);
        graphics.text(font, text, x + 1, y, GuiRenderHelper.scaleAlpha(glow, 0.45F), false);
        graphics.text(font, text, x, y - 1, GuiRenderHelper.scaleAlpha(COSMIC_CYAN, 0.34F), false);
        graphics.text(font, text, x, y + 1, GuiRenderHelper.scaleAlpha(TERMINUS_GOLD, 0.28F), false);
        graphics.text(font, text, x, y, color, true);
    }

    private static void drawCornerGlyphs(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        float pulse = 0.35F + 0.35F * (0.5F + 0.5F * Mth.sin(time * 2.0F));
        int purple = GuiRenderHelper.scaleAlpha(RIFT_MAGENTA, pulse);
        int cyan = GuiRenderHelper.scaleAlpha(COSMIC_CYAN, pulse);
        int gold = GuiRenderHelper.scaleAlpha(TERMINUS_GOLD, pulse + 0.18F);
        GuiRenderHelper.drawBeam(graphics, x + 8.0F, y + 15.0F, x + 24.0F, y + 6.0F, 1.5F, purple, cyan);
        GuiRenderHelper.drawBeam(graphics, x + width - 8.0F, y + 15.0F, x + width - 24.0F, y + 6.0F, 1.5F, cyan, gold);
        GuiRenderHelper.drawBeam(graphics, x + 8.0F, y + height - 15.0F, x + 24.0F, y + height - 6.0F, 1.5F, gold, purple);
        GuiRenderHelper.drawBeam(graphics, x + width - 8.0F, y + height - 15.0F, x + width - 24.0F, y + height - 6.0F, 1.5F, purple, cyan);
    }

    private static void drawRotatingHexagram(
            GuiGraphicsExtractor graphics,
            float cx,
            float cy,
            float radius,
            float rotation,
            float width,
            int colorA,
            int colorB
    ) {
        float[] xs = new float[6];
        float[] ys = new float[6];
        for (int i = 0; i < 6; i++) {
            float angle = rotation + (float) (Math.PI / 6.0D + i * Math.PI / 3.0D);
            xs[i] = cx + Mth.cos(angle) * radius;
            ys[i] = cy + Mth.sin(angle) * radius;
        }

        GuiRenderHelper.drawTriangle(graphics, xs[0], ys[0], xs[2], ys[2], xs[4], ys[4], GuiRenderHelper.scaleAlpha(colorA, 0.13F));
        GuiRenderHelper.drawTriangle(graphics, xs[1], ys[1], xs[3], ys[3], xs[5], ys[5], GuiRenderHelper.scaleAlpha(colorB, 0.11F));
        GuiRenderHelper.drawBeam(graphics, xs[0], ys[0], xs[2], ys[2], width, colorA, colorB);
        GuiRenderHelper.drawBeam(graphics, xs[2], ys[2], xs[4], ys[4], width, colorB, colorA);
        GuiRenderHelper.drawBeam(graphics, xs[4], ys[4], xs[0], ys[0], width, colorA, colorB);
        GuiRenderHelper.drawBeam(graphics, xs[1], ys[1], xs[3], ys[3], width, colorB, colorA);
        GuiRenderHelper.drawBeam(graphics, xs[3], ys[3], xs[5], ys[5], width, colorA, colorB);
        GuiRenderHelper.drawBeam(graphics, xs[5], ys[5], xs[1], ys[1], width, colorB, colorA);
        GuiRenderHelper.drawDiamond(graphics, cx, cy, Math.max(2.0F, width * 2.1F), GuiRenderHelper.scaleAlpha(PALE_STAR, 0.55F));
    }

    private static void drawRing(
            GuiGraphicsExtractor graphics,
            float cx,
            float cy,
            float radius,
            float width,
            int color,
            float rotation,
            int segments,
            int gapMode
    ) {
        for (int i = 0; i < segments; i++) {
            if ((gapMode != 1 || i % 5 != 2) && (gapMode != 2 || i % 4 != 1)) {
                float a1 = rotation + (float) (Math.PI * 2.0D * i / segments);
                float a2 = rotation + (float) (Math.PI * 2.0D * (i + 0.68F) / segments);
                GuiRenderHelper.drawBeam(
                        graphics,
                        cx + Mth.cos(a1) * radius, cy + Mth.sin(a1) * radius,
                        cx + Mth.cos(a2) * radius, cy + Mth.sin(a2) * radius,
                        width, color, color
                );
            }
        }
    }

    private static void drawOrbitingRunes(GuiGraphicsExtractor graphics, float cx, float cy, float radius, float time, int count) {
        for (int i = 0; i < count; i++) {
            float angle = time * 0.75F + (float) (Math.PI * 2.0D * i / count);
            float pulse = 0.55F + 0.45F * Mth.sin(time * 2.2F + i * 0.9F);
            int color = i % 3 == 0 ? TERMINUS_GOLD : (i % 3 == 1 ? COSMIC_CYAN : RIFT_MAGENTA);
            float px = cx + Mth.cos(angle) * radius;
            float py = cy + Mth.sin(angle) * radius;
            GuiRenderHelper.drawDiamond(graphics, px, py, 1.7F + pulse * 1.4F, GuiRenderHelper.scaleAlpha(color, 0.35F + pulse * 0.35F));
            if (i % 2 == 0) {
                float inner = radius - 8.0F;
                GuiRenderHelper.drawBeam(
                        graphics,
                        cx + Mth.cos(angle) * inner, cy + Mth.sin(angle) * inner,
                        px, py, 0.65F,
                        GuiRenderHelper.scaleAlpha(color, 0.25F),
                        GuiRenderHelper.scaleAlpha(PALE_STAR, 0.36F)
                );
            }
        }
    }

    private static void drawOblivionCrown(GuiGraphicsExtractor graphics, float cx, float cy, float radius, float time) {
        for (int i = 0; i < 9; i++) {
            float t = (i - 4) / 4.0F;
            float x1 = cx + t * radius;
            float crownHeight = 8.0F + 6.0F * (0.5F + 0.5F * Mth.sin(time * 2.6F + i));
            int color = i % 2 == 0
                    ? GuiRenderHelper.scaleAlpha(TERMINUS_GOLD, 0.42F)
                    : GuiRenderHelper.scaleAlpha(RIFT_MAGENTA, 0.36F);
            GuiRenderHelper.drawBeam(graphics, x1 - 4.0F, cy + 7.0F, x1, cy - crownHeight, 1.0F, color, GuiRenderHelper.scaleAlpha(PALE_STAR, 0.3F));
            GuiRenderHelper.drawBeam(graphics, x1, cy - crownHeight, x1 + 4.0F, cy + 7.0F, 1.0F, GuiRenderHelper.scaleAlpha(PALE_STAR, 0.3F), color);
        }
    }

    private static void drawCausalityThreads(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float time) {
        for (int i = 0; i < 8; i++) {
            float seed = i * 17.17F;
            float px = x + 20.0F + noise(seed) * (width - 40.0F);
            float py = y + 30.0F + noise(seed + 5.0F) * (height - 62.0F);
            float slash = 8.0F + noise(seed + 9.0F) * 13.0F;
            float sway = Mth.sin(time * 1.7F + i) * 2.8F;
            int color = i % 3 == 0
                    ? GuiRenderHelper.scaleAlpha(COSMIC_CYAN, 0.28F)
                    : (i % 3 == 1 ? GuiRenderHelper.scaleAlpha(RIFT_MAGENTA, 0.24F) : GuiRenderHelper.scaleAlpha(TERMINUS_GOLD, 0.22F));
            GuiRenderHelper.drawBeam(
                    graphics,
                    px - slash, py + sway,
                    px + slash * 0.72F, py - slash * 0.48F + sway,
                    0.8F, color, GuiRenderHelper.scaleAlpha(PALE_STAR, 0.2F)
            );
        }
    }

    private static int gradientColor(float progress, float alpha) {
        float wrapped = Mth.frac(progress);
        float scaled = wrapped * BORDER_GRADIENT.length;
        int index = (int) scaled;
        float local = scaled - index;
        int from = BORDER_GRADIENT[index % BORDER_GRADIENT.length];
        int to = BORDER_GRADIENT[(index + 1) % BORDER_GRADIENT.length];
        return GuiRenderHelper.withAlpha(GuiRenderHelper.lerpRgb(from, to, local), alpha);
    }

    private static float noise(float value) {
        return Mth.frac(Mth.sin(value) * 43758.547F);
    }
}
