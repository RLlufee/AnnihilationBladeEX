package QWQ.QingYi.annihilationblade.nightfall_dragon.client;

import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

/**
 * 魔龙夜陨 - 终极龙魂姿态与暗黑龙威旗舰级 Tooltip 渲染器 (Ultra High-Tech Fantasy Overhaul)
 * 
 * 视觉震撼再突破：
 * 1. 动态 3x3 龙威权能全效果芯片组 (9 核心龙术芯片)。
 * 2. 交互式鼠标聚光灯追踪 (Mouse Spotlight Interactivity)。
 * 3. 动态全息激光扫描线 (Cyberpunk Hologram Scanline)。
 * 4. 蜂窝科技矩阵背景 (Hexagonal Tech Matrix Grid)。
 * 5. 龙瞳能量核心 (Pulsing Dragon Eye Core)。
 * 6. 神陨形态故障错位特效 (Chromatic Aberration Glitch Shift)。
 * 7. 动态双重逆向旋转龙威魔法阵 & 升腾火花粒子 & 龙爪角框。
 */
public final class NightfallDragonTooltipRenderer {
   private static final int WIDTH = 320;
   private static final int MIN_WIDTH = 240;

   // 封印形态 - 暗紫星夜色系
   private static final int[] SEALED_SPECTRUM = new int[] { 0x9D4EDD, 0xE0AAFF, 0x7B2CBF, 0x5A189A, 0x3C096C, 0xC77DFF };
   // 觉醒形态 - 炽金龙炎色系
   private static final int[] AWAKENED_SPECTRUM = new int[] { 0xFFB703, 0xFFD000, 0xFB8500, 0xD00000, 0xFF0000, 0xFFE600 };
   // 神陨形态 - 灭世绯紫与龙皇极光色系
   private static final int[] FINAL_SPECTRUM = new int[] { 0xFF0055, 0xFF2A85, 0x9000FF, 0x00FFFF, 0xFF4800, 0xD800FF };

   private static final DragonChip[] DRAGON_CHIPS = new DragonChip[] {
         new DragonChip("item.annihilationblade.nightfall_dragon.tooltip.chip.dragon_pressure", 0x9D4EDD),
         new DragonChip("item.annihilationblade.nightfall_dragon.tooltip.chip.demonic_blood", 0xFF0055),
         new DragonChip("item.annihilationblade.nightfall_dragon.tooltip.chip.reverse_scale", 0xFB8500),
         new DragonChip("item.annihilationblade.nightfall_dragon.tooltip.chip.outer_god", 0x00FFFF),
         new DragonChip("item.annihilationblade.nightfall_dragon.tooltip.chip.god_body", 0xFFB703),
         new DragonChip("item.annihilationblade.nightfall_dragon.tooltip.chip.scale_guard", 0x00FF88),
         new DragonChip("item.annihilationblade.nightfall_dragon.tooltip.chip.blade_storm", 0xE0AAFF),
         new DragonChip("item.annihilationblade.nightfall_dragon.tooltip.chip.world_cleaving", 0xFF4500),
         new DragonChip("item.annihilationblade.nightfall_dragon.tooltip.chip.absolute_domain", 0xD800FF)
   };

   private static ItemStack cachedEnchantStack = ItemStack.EMPTY;
   private static CompoundTag cachedEnchantTag = null;
   private static List<EnchantmentLine> cachedEnchantmentLines = null;

   private NightfallDragonTooltipRenderer() {
   }

   public static void render(GuiGraphics graphics, Font font, ItemStack stack, List<Component> vanillaLines, int mouseX, int mouseY) {
      int screenWidth = graphics.guiWidth();
      int screenHeight = graphics.guiHeight();

      int width = Math.min(WIDTH, Math.max(MIN_WIDTH, screenWidth - 18));
      int contentWidth = width - 24;

      List<EnchantmentLine> enchantments = getEnchantments(stack);
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

      graphics.pose().pushPose();
      graphics.pose().translate(0.0F, 0.0F, 760.0F);

      renderFrame(graphics, x, y, width, height, time, form, mouseX, mouseY);
      renderContent(graphics, font, stack, vanillaLines, enchantments, x, y, width, height, contentWidth, enchantRowHeight, time, form, mouseX, mouseY);

      graphics.pose().popPose();
   }

   private static void renderFrame(GuiGraphics graphics, int x, int y, int width, int height, float time, String form, int mouseX, int mouseY) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();

      Matrix4f matrix = graphics.pose().last().pose();
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

      // 1. 背景暗黑深渊星云梯度
      graphics.enableScissor(backdropX, backdropY, backdropX + backdropWidth, backdropY + backdropHeight);
      int backdropBg1 = "final".equals(form) ? 0x77140022 : "awakened".equals(form) ? 0x662A1400 : 0x660E0022;
      int backdropBg2 = "final".equals(form) ? 0xBB33003B : "awakened".equals(form) ? 0x99482200 : 0x991C003B;
      graphics.fillGradient(backdropX, backdropY, backdropX + backdropWidth, backdropY + backdropHeight, backdropBg1, backdropBg2);

      // 2. 浮动龙魂火花粒子
      drawFloatingDragonSparks(matrix, backdropX, backdropY, backdropWidth, backdropHeight, time, form);
      graphics.disableScissor();

      // 3. 双重逆向旋转龙威魔法阵
      drawDualDragonMagicCircles(matrix, backdropX, backdropY, backdropWidth, backdropHeight, time, form);

      // 4. 外层轨道流彩线框
      renderWhiteOrbitBorder(matrix, outerX, outerY, outerWidth, outerHeight, time, form);

      // 5. 主框体背景
      int frameBg1 = "final".equals(form) ? 0xE2200028 : "awakened".equals(form) ? 0xE22E1A00 : 0xE2140828;
      int frameBg2 = "final".equals(form) ? 0xF5380040 : "awakened".equals(form) ? 0xF54A2800 : 0xF5200C3C;
      graphics.fillGradient(x, y, x + width, y + height, frameBg1, frameBg2);

      graphics.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      graphics.fillGradient(x + 2, y + 2, x + width - 2, y + height - 2, 0xC40A0418, 0xEC1A0830);
      graphics.fillGradient(x + 4, y + 4, x + width - 4, y + 46, 0x981C0B36, 0x301C0B36);
      graphics.fillGradient(x + 5, y + 145, x + width - 5, y + height - 5, 0x541C0B36, 0xCD1C0B36);

      // 5.1 蜂窝/矩阵科技背景网格
      drawTechHexGrid(matrix, x + 2, y + 2, width - 4, height - 4, time, form);

      // 5.2 鼠标动态聚光灯追踪
      drawMouseSpotlight(matrix, x + 2, y + 2, width - 4, height - 4, mouseX, mouseY, time, form);

      // 5.3 全息激光扫描线
      drawHologramScanline(matrix, x + 2, y + 2, width - 4, height - 4, time, form);

      drawMovingDragonAccents(matrix, x, y, width, height, time, form);
      // 顶部龙魂核心与轨道符文：让 Tooltip 在静止时也有明确的视觉焦点。
      drawDragonSoulCore(matrix, x + width - 22.0F, y + 22.0F, 9.0F, time, form);
      drawOrbitRunes(matrix, x + width / 2.0F, y + height - 9.0F, width - 34.0F, time, form);
      drawMouseSlashTrails(matrix, x, y, width, height, mouseX, mouseY, time, form);
      graphics.disableScissor();

      // 6. 龙爪/龙角立体边角框 (Dragon Corner Claws)
      drawDragonCornerClaws(matrix, x, y, width, height, time, form);

      // 7. 渐变脉冲边框
      renderGradientBorder(matrix, x, y, width, height, time, form);
      RenderSystem.disableBlend();
   }

   private static void renderContent(GuiGraphics graphics, Font font, ItemStack stack, List<Component> vanillaLines,
         List<EnchantmentLine> enchantments, int x, int y, int width, int height, int contentWidth,
         int enchantRowHeight, float time, String form, int mouseX, int mouseY) {
      int center = x + width / 2;
      String titleKey = "final".equals(form)
            ? "item.annihilationblade.nightfall_dragon.tooltip.title.final"
            : "awakened".equals(form)
                  ? "item.annihilationblade.nightfall_dragon.tooltip.title.awakened"
                  : "item.annihilationblade.nightfall_dragon.tooltip.title.sealed";

      Component itemName = Component.translatable(titleKey);
      int primaryColor = "final".equals(form) ? 0xFF0055 : "awakened".equals(form) ? 0xFFB703 : 0xE0AAFF;
      int glowColor = "final".equals(form) ? 0x9000FF : "awakened".equals(form) ? 0xFB8500 : 0x7B2CBF;

      // 标题多重炫彩发光与神陨错位
      drawSpectralDragonTitle(graphics, font, itemName, center - font.width(itemName) / 2, y + 8, primaryColor, glowColor, time, form);

      String subtitle = I18n.get("item.annihilationblade.nightfall_dragon.tooltip.subtitle");
      drawGlowText(graphics, font, Component.literal(subtitle), center - font.width(subtitle) / 2, y + 20, 0xF2FEFF, glowColor);

      // 分隔线
      drawHorizontalSeparator(graphics.pose().last().pose(), x + 12, y + 32, contentWidth, time, form);

      // 龙魂能量流动槽 (Liquid Dragon Energy Meter)
      drawDragonEnergyMeter(graphics.pose().last().pose(), x + 12, y + 35, contentWidth, 3, time, form);

      int currentY = y + 42;

      // 1. 形态印记卡片
      drawSectionHeader(graphics, font, x + 12, currentY, I18n.get("item.annihilationblade.nightfall_dragon.tooltip.section.form"), primaryColor);
      currentY += 12;
      String formTextKey = "final".equals(form)
            ? "item.annihilationblade.nightfall_dragon.tooltip.form.final"
            : "awakened".equals(form)
                  ? "item.annihilationblade.nightfall_dragon.tooltip.form.awakened"
                  : "item.annihilationblade.nightfall_dragon.tooltip.form.sealed";
      String rankTag = "final".equals(form) ? "[RANK: SSS / 龙皇灭世]" : "awakened".equals(form) ? "[RANK: SS / 龙魂觉醒]" : "[RANK: S / 暗龙封印]";
      drawFormBadgeCard(graphics, font, x + 12, currentY, contentWidth, 16, I18n.get(formTextKey), rankTag, primaryColor, 0x55000000, time, form);
      currentY += 20;

      // 2. 绑定奥义卡片 (SA)
      drawSectionHeader(graphics, font, x + 12, currentY, I18n.get("item.annihilationblade.nightfall_dragon.tooltip.section.sa"), primaryColor);
      currentY += 12;
      String saName = "final".equals(form) ? "神陨降临 (Cosmic Nightfall Descent)" : "awakened".equals(form) ? "鳞之卫 (Scale Guard)" : "夜陨次元斩 (Nightfall Judgement Cut)";
      drawCard(graphics, font, x + 12, currentY, contentWidth, 16, saName, 0xF2FEFF, 0x55000000);
      currentY += 20;

      // 3. 龙威权能 SE 芯片组 (9大芯片 3x3)
      drawSectionHeader(graphics, font, x + 12, currentY, I18n.get("item.annihilationblade.nightfall_dragon.tooltip.section.effects"), primaryColor);
      currentY += 12;
      renderDragonChipGrid(graphics, font, x + 12, currentY, contentWidth, time);
      currentY += 40;

      // 4. 刀体数据面板
      drawSectionHeader(graphics, font, x + 12, currentY, I18n.get("item.annihilationblade.nightfall_dragon.tooltip.section.stats"), primaryColor);
      currentY += 12;
      BladeStats stats = readBladeStats(stack);
      renderStatGrid(graphics, font, x + 12, currentY, contentWidth, stats, form);
      currentY += 28;

      // 5. 附魔回路
      drawSectionHeader(graphics, font, x + 12, currentY, I18n.get("item.annihilationblade.nightfall_dragon.tooltip.section.enchantments"), primaryColor);
      currentY += 12;
      renderEnchantmentGrid(graphics, font, enchantments, x + 12, currentY, contentWidth, enchantRowHeight, primaryColor);
      drawStatusRibbon(graphics, font, x + 12, y + height - 15, contentWidth, time, form);
   }

   private static void drawDragonSoulCore(Matrix4f matrix, float cx, float cy, float radius, float time, String form) {
      int core = sampleFormSpectrum(form, time * 0.55F);
      float pulse = 0.82F + 0.18F * Mth.sin(time * 4.0F);
      drawCircle(matrix, cx, cy, radius + 3.0F + pulse, time * 0.9F, 18, 0.8F, withAlpha(core, 0.35F));
      drawCircle(matrix, cx, cy, radius + 1.0F, -time * 1.3F, 12, 1.0F, withAlpha(0xFFFFFF, 0.55F));
      drawCircle(matrix, cx, cy, radius * 0.52F, time * 1.8F, 8, 1.5F, withAlpha(core, 0.9F));
      drawQuad(matrix, cx - 2.0F, cy - 2.0F, cx + 2.0F, cy - 2.0F, cx + 2.0F, cy + 2.0F, cx - 2.0F, cy + 2.0F, withAlpha(0xFFFFFF, 0.95F));
   }

   private static void drawOrbitRunes(Matrix4f matrix, float cx, float cy, float width, float time, String form) {
      float half = width * 0.5F;
      int color = withAlpha(sampleFormSpectrum(form, time * 0.35F), 0.7F);
      drawBeam(matrix, cx - half, cy, cx + half, cy, 0.55F, withAlpha(color, 0.0F), color);
      drawBeam(matrix, cx + half, cy, cx - half, cy, 0.55F, color, withAlpha(color, 0.0F));
      for (int i = 0; i < 5; i++) {
         float px = cx - half + ((time * 28.0F + i * width * 0.23F) % width);
         float size = 1.2F + (i % 2) * 0.8F;
         int rune = withAlpha(sampleFormSpectrum(form, time * 0.2F + i), 0.8F);
         drawQuad(matrix, px - size, cy - size, px + size, cy - size, px + size, cy + size, px - size, cy + size, rune);
      }
   }

   private static void drawMouseSlashTrails(Matrix4f matrix, int x, int y, int width, int height, int mouseX, int mouseY, float time, String form) {
      if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
         return;
      }
      float sweep = (time * 1.8F) % (Mth.PI * 2.0F);
      float radius = Math.min(width, height) * 0.38F;
      int color = withAlpha(sampleFormSpectrum(form, time * 0.6F), 0.22F);
      for (int i = 0; i < 3; i++) {
         float angle = sweep + i * 0.22F;
         float x1 = mouseX + Mth.cos(angle) * (radius - i * 7.0F);
         float y1 = mouseY + Mth.sin(angle) * (radius - i * 7.0F);
         float x2 = mouseX + Mth.cos(angle + 0.34F) * (radius + 10.0F);
         float y2 = mouseY + Mth.sin(angle + 0.34F) * (radius + 10.0F);
         drawBeam(matrix, x1, y1, x2, y2, 0.7F + i * 0.35F, color, withAlpha(0xFFFFFF, 0.0F));
      }
   }

   private static void drawStatusRibbon(GuiGraphics graphics, Font font, int x, int y, int width, float time, String form) {
      int color = sampleFormSpectrum(form, time * 0.4F + 2.0F);
      String state = "final".equals(form) ? "NIGHTFALL // APOCALYPSE" : "awakened".equals(form) ? "NIGHTFALL // AWAKENED" : "NIGHTFALL // SEALED";
      graphics.drawString(font, state, x + 2, y, withAlpha(color, 0.72F), false);
      int ticks = 9;
      int start = x + width - ticks * 4;
      for (int i = 0; i < ticks; i++) {
         float phase = (time * 3.0F + i * 0.7F) % (Mth.PI * 2.0F);
         int barColor = withAlpha(sampleFormSpectrum(form, i * 0.7F + time * 0.3F), 0.3F + 0.5F * (Mth.sin(phase) * 0.5F + 0.5F));
         graphics.fill(start + i * 4, y + 1, start + i * 4 + 2, y + 5, barColor);
      }
   }

   private static void drawTechHexGrid(Matrix4f matrix, int x, int y, int width, int height, float time, String form) {
      int step = 14;
      int color = withAlpha(sampleFormSpectrum(form, time * 0.1F), 0.06F);
      for (int gx = x; gx < x + width; gx += step) {
         for (int gy = y; gy < y + height; gy += step) {
            drawQuad(matrix, gx, gy, gx + 2, gy, gx + 2, gy + 2, gx, gy + 2, color);
         }
      }
   }

   private static void drawMouseSpotlight(Matrix4f matrix, int x, int y, int width, int height, int mouseX, int mouseY, float time, String form) {
      if (mouseX < x - 20 || mouseX > x + width + 20 || mouseY < y - 20 || mouseY > y + height + 20) {
         return;
      }
      float radius = 55.0F;
      float alpha = 0.14F + (float) Math.sin(time * 4.0F) * 0.04F;
      int lightColor = withAlpha(sampleFormSpectrum(form, time * 0.3F), alpha);
      drawCircle(matrix, mouseX, mouseY, radius, 0.0F, 16, 2.0F, lightColor);
      drawCircle(matrix, mouseX, mouseY, radius * 0.5F, 0.0F, 12, 1.2F, lightColor);
   }

   private static void drawHologramScanline(Matrix4f matrix, int x, int y, int width, int height, float time, String form) {
      float scanY = y + ((time * 70.0F) % height);
      int laserColor = withAlpha(sampleFormSpectrum(form, time * 0.4F), 0.45F);
      int trailColor = withAlpha(sampleFormSpectrum(form, time * 0.4F), 0.10F);

      drawQuad(matrix, x, scanY, x + width, scanY, x + width, scanY + 1.5F, x, scanY + 1.5F, laserColor);
      drawQuad(matrix, x, Math.max(y, scanY - 8.0F), x + width, Math.max(y, scanY - 8.0F), x + width, scanY, x, scanY, trailColor, trailColor, laserColor, laserColor);
   }

   private static void drawFloatingDragonSparks(Matrix4f matrix, int x, int y, int width, int height, float time, String form) {
      int count = 22;
      for (int i = 0; i < count; i++) {
         float pseudoX = (float) Math.sin(i * 1.7F + 0.3F) * 0.5F + 0.5F;
         float speed = 8.0F + (i % 5) * 4.0F;
         float sy = (y + height) - ((time * speed + i * 22.0F) % (height + 20.0F));
         float sx = x + pseudoX * width;

         float alpha = Mth.clamp((float) Math.sin(time * 3.0F + i), 0.2F, 0.85F);
         int color = withAlpha(sampleFormSpectrum(form, i * 0.5F + time * 0.2F), alpha);

         float size = 1.2F + (i % 3) * 0.6F;
         drawQuad(matrix, sx - size, sy - size, sx + size, sy - size, sx + size, sy + size, sx - size, sy + size, color);
      }
   }

   private static void drawDragonCornerClaws(Matrix4f matrix, int x, int y, int width, int height, float time, String form) {
      float clawLen = 12.0F;
      float clawThick = 2.2F;
      int color = sampleFormSpectrum(form, time * 0.3F);

      // Top-Left
      drawQuad(matrix, x - 2, y - 2, x + clawLen, y - 2, x + clawLen, y - 2 + clawThick, x - 2, y - 2 + clawThick, color);
      drawQuad(matrix, x - 2, y - 2, x - 2 + clawThick, y - 2, x - 2 + clawThick, y + clawLen, x - 2, y + clawLen, color);

      // Top-Right
      drawQuad(matrix, x + width + 2 - clawLen, y - 2, x + width + 2, y - 2, x + width + 2, y - 2 + clawThick, x + width + 2 - clawLen, y - 2 + clawThick, color);
      drawQuad(matrix, x + width + 2 - clawThick, y - 2, x + width + 2, y - 2, x + width + 2, y + clawLen, x + width + 2 - clawThick, y + clawLen, color);

      // Bottom-Left
      drawQuad(matrix, x - 2, y + height + 2 - clawThick, x + clawLen, y + height + 2 - clawThick, x + clawLen, y + height + 2, x - 2, y + height + 2, color);
      drawQuad(matrix, x - 2, y + height + 2 - clawLen, x - 2 + clawThick, y + height + 2 - clawLen, x - 2 + clawThick, y + height + 2, x - 2, y + height + 2, color);

      // Bottom-Right
      drawQuad(matrix, x + width + 2 - clawLen, y + height + 2 - clawThick, x + width + 2, y + height + 2 - clawThick, x + width + 2, y + height + 2, x + width + 2 - clawLen, y + height + 2, color);
      drawQuad(matrix, x + width + 2 - clawThick, y + height + 2 - clawLen, x + width + 2, y + height + 2 - clawLen, x + width + 2, y + height + 2, x + width + 2, y + height + 2, color);
   }

   private static void drawDualDragonMagicCircles(Matrix4f matrix, int x, int y, int width, int height, float time, String form) {
      float cx = x + width / 2.0F;
      float cy = y + height / 2.0F;
      float r1 = Math.max(width, height) * 0.54F + 12.0F;
      float r2 = r1 * 0.72F;

      float spin1 = time * 0.2F;
      float spin2 = -time * 0.35F;

      int color1 = withAlpha(sampleFormSpectrum(form, time * 0.15F), 0.22F);
      int color2 = withAlpha(sampleFormSpectrum(form, time * 0.25F + 3.0F), 0.18F);

      drawCircle(matrix, cx, cy, r1, spin1, 36, 0.8F, color1);
      drawPolygon(matrix, cx, cy, r1 * 0.92F, 12, spin1 + Mth.PI / 12.0F, 0.5F, color1);

      drawCircle(matrix, cx, cy, r2, spin2, 24, 0.6F, color2);
      drawPolygon(matrix, cx, cy, r2, 6, spin2, 0.5F, color2);
   }

   private static void drawDragonEnergyMeter(Matrix4f matrix, int x, int y, int width, int height, float time, String form) {
      drawQuad(matrix, x, y, x + width, y + height, 0x55000000);
      int segments = 10;
      float segWidth = (float) width / segments;
      for (int i = 0; i < segments; i++) {
         float phase = (time * 2.0F + i * 0.4F) % (Mth.PI * 2.0F);
         float glow = (float) Math.sin(phase) * 0.5F + 0.5F;
         int color = withAlpha(sampleFormSpectrum(form, i * 0.6F + time * 0.5F), 0.4F + glow * 0.6F);
         float sx = x + i * segWidth;
         float ex = sx + segWidth - 1.0F;
         drawQuad(matrix, sx, y, ex, y, ex, y + height, sx, y + height, color);
      }
   }

   private static void drawSpectralDragonTitle(GuiGraphics graphics, Font font, Component title, int x, int y, int primaryColor, int glowColor, float time, String form) {
      if ("final".equals(form)) {
         float glitchX = (float) Math.sin(time * 15.0F) * 1.2F;
         graphics.drawString(font, title, (int) (x + glitchX + 1), y, 0x8800FFFF, false);
         graphics.drawString(font, title, (int) (x - glitchX - 1), y, 0x88FF0055, false);
      }
      int dropShadow = 0x88000000;
      graphics.drawString(font, title, x + 2, y + 2, dropShadow, false);
      drawGlowText(graphics, font, title, x, y, primaryColor, glowColor);
   }

   private static void drawFormBadgeCard(GuiGraphics graphics, Font font, int x, int y, int width, int height, String formText, String rankTag, int color, int bgColor, float time, String form) {
      graphics.fillGradient(x, y, x + width, y + height, bgColor, bgColor | 0xFF000000);
      Matrix4f matrix = graphics.pose().last().pose();
      int borderColor = withAlpha(color, 0.65F);
      drawQuad(matrix, x, y, x + width, y, x + width, y + 1, x, y + 1, borderColor);
      drawQuad(matrix, x, y + height - 1, x + width, y + height - 1, x + width, y + height, x, y + height, borderColor);

      graphics.drawString(font, formText, x + 6, y + (height - 8) / 2, color, false);
      int rankWidth = font.width(rankTag);
      graphics.drawString(font, rankTag, x + width - rankWidth - 6, y + (height - 8) / 2, withAlpha(color, 0.85F), false);
   }

   private static void renderDragonChipGrid(GuiGraphics graphics, Font font, int x, int y, int width, float time) {
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

   private static void renderStatGrid(GuiGraphics graphics, Font font, int x, int y, int width, BladeStats stats, String form) {
      int cellWidth = (width - 8) / 3;
      int cellHeight = 11;
      String formTitle = "final".equals(form) ? "神陨" : "awakened".equals(form) ? "觉醒" : "封印";
      StatCell[] cells = new StatCell[] {
            new StatCell("item.annihilationblade.infinity_stellaris.tooltip.stat.kills", String.valueOf(stats.killCount()), 0xFF0055),
            new StatCell("item.annihilationblade.infinity_stellaris.tooltip.stat.souls", String.valueOf(stats.proudSoul()), 0xFFB703),
            new StatCell("item.annihilationblade.infinity_stellaris.tooltip.stat.refine", String.valueOf(stats.refine()), 0x00FFFF),
            new StatCell("item.annihilationblade.infinity_stellaris.tooltip.stat.attack", "+22.0", 0xE0AAFF),
            new StatCell("item.annihilationblade.infinity_stellaris.tooltip.stat.durability", "∞ 无双", 0x7B2CBF),
            new StatCell("item.annihilationblade.nightfall_dragon.tooltip.stat.form_power", formTitle, "final".equals(form) ? 0xFF0055 : 0xFFB703)
      };

      for (int i = 0; i < cells.length; i++) {
         StatCell cell = cells[i];
         int row = i / 3;
         int col = i % 3;
         int cx = x + col * (cellWidth + 4);
         int cy = y + row * (cellHeight + 3);
         drawStatCell(graphics, font, cx, cy, cellWidth, cellHeight, cell);
      }
   }

   private static void renderEnchantmentGrid(GuiGraphics graphics, Font font, List<EnchantmentLine> enchantments, int x,
         int y, int width, int rowHeight, int primaryColor) {
      if (enchantments.isEmpty()) {
         graphics.drawString(font, Component.literal("§7[无附魔回路]"), x, y, 0x888888, false);
         return;
      }

      int columnWidth = (width - 8) / 2;
      for (int i = 0; i < enchantments.size(); i++) {
         int row = i / 2;
         int col = i % 2;
         int ex = x + col * (columnWidth + 8);
         int ey = y + row * rowHeight;
         String text = font.plainSubstrByWidth(enchantments.get(i).text(), columnWidth);
         graphics.drawString(font, text, ex, ey, withAlpha(primaryColor, 0.9F), false);
      }
   }

   private static void drawCard(GuiGraphics graphics, Font font, int x, int y, int width, int height, String text, int color, int bgColor) {
      graphics.fillGradient(x, y, x + width, y + height, bgColor, bgColor | 0xFF000000);
      Matrix4f matrix = graphics.pose().last().pose();
      drawQuad(matrix, x, y, x + width, y, x + width, y + 1, x, y + 1, withAlpha(color, 0.5F));
      drawQuad(matrix, x, y + height - 1, x + width, y + height - 1, x + width, y + height, x, y + height, withAlpha(color, 0.5F));
      graphics.drawString(font, text, x + 6, y + (height - 8) / 2, color, false);
   }

   private static void drawChip(GuiGraphics graphics, Font font, int x, int y, int width, int height, String text, int color) {
      graphics.fill(x, y, x + width, y + height, 0x44000000);
      Matrix4f matrix = graphics.pose().last().pose();
      drawQuad(matrix, x, y, x + 2, y, x + 2, y + height, x, y + height, color);
      String trimmed = font.plainSubstrByWidth(text, width - 6);
      graphics.drawString(font, trimmed, x + 4, y + (height - 8) / 2, color, false);
   }

   private static void drawStatCell(GuiGraphics graphics, Font font, int x, int y, int width, int height, StatCell cell) {
      graphics.fill(x, y, x + width, y + height, 0x33000000);
      String label = I18n.get(cell.labelKey());
      graphics.drawString(font, label, x + 3, y + (height - 8) / 2, 0xAAAAAA, false);
      int valWidth = font.width(cell.value());
      graphics.drawString(font, cell.value(), x + width - valWidth - 3, y + (height - 8) / 2, cell.color(), false);
   }

   private static void drawSectionHeader(GuiGraphics graphics, Font font, int x, int y, String title, int color) {
      graphics.drawString(font, "▌ " + title, x, y, color, false);
   }

   private static void drawHorizontalSeparator(Matrix4f matrix, int x, int y, int width, float time, String form) {
      int colorA = sampleFormSpectrum(form, time * 0.2F);
      int colorB = sampleFormSpectrum(form, time * 0.2F + 2.0F);
      drawQuad(matrix, x, y, x + width, y, x + width, y + 1, x, y + 1, colorA, colorB, colorB, colorA);
   }

   private static void drawGlowText(GuiGraphics graphics, Font font, Component text, int x, int y, int color, int glowColor) {
      graphics.drawString(font, text, x - 1, y, withAlpha(glowColor, 0.35F), false);
      graphics.drawString(font, text, x + 1, y, withAlpha(glowColor, 0.35F), false);
      graphics.drawString(font, text, x, y - 1, withAlpha(glowColor, 0.35F), false);
      graphics.drawString(font, text, x, y + 1, withAlpha(glowColor, 0.35F), false);
      graphics.drawString(font, text, x, y, color, true);
   }

   private static void drawMovingDragonAccents(Matrix4f matrix, int x, int y, int width, int height, float time, String form) {
      for (int i = 0; i < 6; i++) {
         float phase = (time * 0.08F + i * 0.16F) % 1.0F;
         float sx = x - 28.0F + phase * (width + 56.0F);
         int colorA = withAlpha(sampleFormSpectrum(form, i * 0.6F + time * 0.2F), 0.15F);
         int colorB = withAlpha(0xFFFFFF, 0.06F);
         drawBeam(matrix, sx, y + height + 10.0F, sx + 50.0F, y - 12.0F, 1.0F, colorA, colorB);
      }
   }

   private static void renderWhiteOrbitBorder(Matrix4f matrix, int x, int y, int width, int height, float time, String form) {
      int glow = withAlpha(sampleFormSpectrum(form, time * 0.15F), 0.12F);
      int base = withAlpha(sampleFormSpectrum(form, time * 0.15F), 0.35F);
      drawBeam(matrix, x, y, x + width, y, 2.4F, glow, glow);
      drawBeam(matrix, x, y + height, x + width, y + height, 2.4F, glow, glow);
      drawBeam(matrix, x, y, x, y + height, 2.4F, glow, glow);
      drawBeam(matrix, x + width, y, x + width, y + height, 2.4F, glow, glow);
      drawBeam(matrix, x, y, x + width, y, 0.7F, base, base);
      drawBeam(matrix, x, y + height, x + width, y + height, 0.7F, base, base);
      drawBeam(matrix, x, y, x, y + height, 0.7F, base, base);
      drawBeam(matrix, x + width, y, x + width, y + height, 0.7F, base, base);
   }

   private static void renderGradientBorder(Matrix4f matrix, int x, int y, int width, int height, float time, String form) {
      drawSegmentedBorder(matrix, x, y, width, height, 0.8F, 1.2F, time * 0.1F, 0.85F, form);
   }

   private static void drawSegmentedBorder(Matrix4f matrix, int x, int y, int width, int height, float spread,
         float thickness, float drift, float alpha, String form) {
      int segments = 16;
      for (int i = 0; i < segments; i++) {
         float t1 = (float) i / segments;
         float t2 = (float) (i + 1) / segments;
         int c1 = withAlpha(sampleFormSpectrum(form, t1 * 6.0F + drift), alpha);
         int c2 = withAlpha(sampleFormSpectrum(form, t2 * 6.0F + drift), alpha);
         float sx = x + width * t1;
         float ex = x + width * t2;
         drawQuad(matrix, sx, y - spread, ex, y - spread, ex, y - spread + thickness, sx, y - spread + thickness, c1, c2, c2, c1);
         drawQuad(matrix, sx, y + height + spread, ex, y + height + spread, ex, y + height + spread - thickness, sx, y + height + spread - thickness, c2, c1, c1, c2);
      }
   }

   private static void drawBeam(Matrix4f matrix, float x1, float y1, float x2, float y2, float width, int colorA, int colorB) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float length = Mth.sqrt(dx * dx + dy * dy);
      if (length < 0.001F) {
         return;
      }

      float nx = -dy / length * width;
      float ny = dx / length * width;
      drawQuad(matrix, x1 - nx, y1 - ny, x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, colorA, colorA, colorB, colorB);
   }

   private static void drawCircle(Matrix4f matrix, float cx, float cy, float radius, float rotation, int segments, float thickness, int color) {
      Point2 previous = radialPoint(cx, cy, radius, rotation);
      for (int i = 1; i <= segments; i++) {
         float angle = rotation + i * Mth.PI * 2.0F / segments;
         Point2 current = radialPoint(cx, cy, radius, angle);
         drawBeam(matrix, previous.x(), previous.y(), current.x(), current.y(), thickness, color, color);
         previous = current;
      }
   }

   private static void drawPolygon(Matrix4f matrix, float cx, float cy, float radius, int points, float rotation, float thickness, int color) {
      Point2 previous = radialPoint(cx, cy, radius, rotation);
      for (int i = 1; i <= points; i++) {
         float angle = rotation + i * Mth.PI * 2.0F / points;
         Point2 current = radialPoint(cx, cy, radius, angle);
         drawBeam(matrix, previous.x(), previous.y(), current.x(), current.y(), thickness, color, color);
         previous = current;
      }
   }

   private static Point2 radialPoint(float cx, float cy, float radius, float angle) {
      return new Point2(cx + Mth.cos(angle) * radius, cy + Mth.sin(angle) * radius);
   }

   private static void drawQuad(Matrix4f matrix, float x1, float y1, float x2, float y2, int color) {
      drawQuad(matrix, x1, y1, x2, y1, x2, y2, x1, y2, color, color, color, color);
   }

   private static void drawQuad(Matrix4f matrix, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int color) {
      drawQuad(matrix, x1, y1, x2, y2, x3, y3, x4, y4, color, color, color, color);
   }

   private static void drawQuad(Matrix4f matrix, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int color1, int color2, int color3, int color4) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      BufferBuilder builder = Tesselator.getInstance().getBuilder();
      builder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      vertex(builder, matrix, x1, y1, color1);
      vertex(builder, matrix, x2, y2, color2);
      vertex(builder, matrix, x3, y3, color3);
      vertex(builder, matrix, x4, y4, color4);
      BufferUploader.drawWithShader(builder.end());
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   }

   private static void vertex(BufferBuilder builder, Matrix4f matrix, float x, float y, int color) {
      builder.vertex(matrix, x, y, 0.0F).color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF).endVertex();
   }

   private static int sampleFormSpectrum(String form, float position) {
      int[] spectrum = "final".equals(form) ? FINAL_SPECTRUM : "awakened".equals(form) ? AWAKENED_SPECTRUM : SEALED_SPECTRUM;
      int length = spectrum.length;
      float wrapped = position - (float) Math.floor(position / length) * length;
      if (wrapped >= length) {
         wrapped = 0.0F;
      }

      int index = Math.min(length - 1, Math.max(0, (int) Math.floor(wrapped)));
      int next = index == length - 1 ? 0 : index + 1;
      float blend = wrapped - index;
      blend = blend * blend * (3.0F - 2.0F * blend);
      return lerpColor(spectrum[index], spectrum[next], blend);
   }

   private static int lerpColor(int from, int to, float blend) {
      int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * blend);
      int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * blend);
      int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * blend);
      return r << 16 | g << 8 | b;
   }

   private static int withAlpha(int rgb, float alpha) {
      int a = Mth.clamp(Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F), 0, 255);
      return (rgb & 0xFFFFFF) | a << 24;
   }

   private static List<EnchantmentLine> getEnchantments(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      if (cachedEnchantmentLines != null && ItemStack.matches(cachedEnchantStack, stack) && java.util.Objects.equals(cachedEnchantTag, tag)) {
         return cachedEnchantmentLines;
      }

      List<EnchantmentLine> lines = new ArrayList<>();
      for (Entry<Enchantment, Integer> entry : sortedEnchantments(stack).entrySet()) {
         Enchantment enchantment = entry.getKey();
         int level = entry.getValue();
         ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
         String sortKey = id == null ? enchantment.getDescriptionId() : id.toString();
         lines.add(new EnchantmentLine(sortKey, enchantment.getFullname(level).getString()));
      }

      lines.sort(Comparator.comparing(EnchantmentLine::sortKey));
      cachedEnchantStack = stack.copy();
      cachedEnchantTag = tag != null ? tag.copy() : null;
      cachedEnchantmentLines = lines;
      return lines;
   }

   private static Map<Enchantment, Integer> sortedEnchantments(ItemStack stack) {
      return EnchantmentHelper.getEnchantments(stack);
   }

   private static BladeStats readBladeStats(ItemStack stack) {
      return stack.getCapability(ItemSlashBlade.BLADESTATE)
            .map(state -> new BladeStats(state.getProudSoulCount(), state.getKillCount(), state.getRefine()))
            .orElseGet(() -> readBladeStatsFromTag(stack));
   }

   private static BladeStats readBladeStatsFromTag(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      if (tag == null) {
         return new BladeStats(0, 0, 0);
      }

      CompoundTag bladeState = tag.getCompound("bladeState");
      return new BladeStats(bladeState.getInt("proudSoul"), bladeState.getInt("killCount"), bladeState.getInt("refine"));
   }

   private record DragonChip(String key, int color) {
   }

   private record StatCell(String labelKey, String value, int color) {
   }

   private record EnchantmentLine(String sortKey, String text) {
   }

   private record BladeStats(int proudSoul, int killCount, int refine) {
   }

   private record Point2(float x, float y) {
   }
}
