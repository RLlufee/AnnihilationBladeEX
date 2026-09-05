package QWQ.QingYi.annihilationblade.nightfall_dragon.client;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.event.client.RenderOverrideEvent;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 魔龙夜陨 - 拔刀剑 3D 模型与光效渲染事件重写类
 * 
 * 本类展示了如何钩入（Hook）拔刀剑 (SlashBlade) 的模型渲染管线：
 * 
 * 拔刀剑渲染覆写事件 (RenderOverrideEvent)：拦截 3D 刀身与刀鞘模型的渲染节点 (Target
 * Group)，注入自定义贴图与发光材质。
 * 防重入渲染锁 (ThreadLocal RENDERING_EXTRA_LAYER)：在内部再次调用
 * {@code BladeRenderState.renderOverridedLuminous()} 时，通过 ThreadLocal
 * 标志位打破递归死循环。
 * PoseStack 三角函数律动悬浮动画：利用正弦波 {@code Mth.sin()} 和余弦波
 * {@code Mth.cos()} 控制刀鞘在三维空间中呼吸缩放与微幅晃动。
 * UV 流动帧动画算法 (FLOW_FRAMES)：根据系统真实毫秒时间戳 {@code Util.getMillis()}
 * 计算当前帧号，配合拔刀剑的 {@code renderChargeEffect()} 实现流光特效。
 * 矩阵旋转光环 (Axis.ZP.rotationDegrees)：在 2D 物品或 3D
 * 刀柄处叠加顺时针旋转的极光光环矩阵。
 * 
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Annihilationblade.MODID, value = Dist.CLIENT)
public final class NightfallDragonRenderEvents {
   /** UV 充能特效的总流光帧数（32 帧） */
   private static final int FLOW_FRAMES = 32;

   /** 物品环形光环渲染节点 target 名称 */
   private static final String ITEM_HALO_TARGET = "item_dragon_halo_luminous";

   /** 线程局部变量（ThreadLocal）：渲染额外图层时的递归防御锁 */
   private static final ThreadLocal<Boolean> RENDERING_EXTRA_LAYER = ThreadLocal.withInitial(() -> false);

   private NightfallDragonRenderEvents() {
   }

   /**
    * 拔刀剑渲染覆写事件监听。
    */
   @SubscribeEvent
   public static void onSlashBladeRenderOverride(RenderOverrideEvent event) {
      if (!NightfallDragonDefinitions.isNightfallDragon(event.getStack())) {
         return;
      }

      String target = event.getTarget();
      if (target == null) {
         return;
      }

      // 如果当前线程正处于额外图层的渲染逻辑中，直接返回，防止死循环递归
      if (RENDERING_EXTRA_LAYER.get()) {
         return;
      }

      long time = Util.getMillis();
      String lowerTarget = target.toLowerCase();

      try {
         // 上锁
         RENDERING_EXTRA_LAYER.set(true);

         // 1. 刀鞘浮动与发光重绘
         if (lowerTarget.contains("sheath") || lowerTarget.contains("scabbard")) {
            renderEnhancedSheathMotionAndGlow(event, lowerTarget, time);
         }
         // 2. 刀身发光部位增强
         else if (lowerTarget.contains("luminous")) {
            renderEnhancedBladeLuminous(event, time);
         }

         // // 3. 物品视角下的交错旋转光环
         // if (lowerTarget.startsWith("item_")) {
         // renderRotatingItemHalo(event, time);
         // }
      } finally {
         // 解锁
         RENDERING_EXTRA_LAYER.set(false);
      }
   }

   /**
    * 渲染刀鞘的动态呼吸缩放、微幅晃动与多重发光流光图层。
    */
   private static void renderEnhancedSheathMotionAndGlow(RenderOverrideEvent event, String target, long time) {
      PoseStack poseStack = event.getPoseStack();
      poseStack.pushPose();

      try {
         // 正弦波计算呼吸缩放 (1.0 +- 0.018) 与 Y 轴浮动 (0.0 +- 0.012)
         float pulseScale = 1.0F + Mth.sin((float) (time * 0.004D)) * 0.018F;
         float floatY = Mth.sin((float) (time * 0.003D)) * 0.012F;
         float subtleRotZ = Mth.cos((float) (time * 0.0025D)) * 0.6F;
         float subtleRotX = Mth.sin((float) (time * 0.0035D)) * 0.4F;

         // 应用矩阵平移、缩放与四元数/角度旋转
         poseStack.translate(0.0D, floatY, 0.0D);
         poseStack.scale(pulseScale, pulseScale, pulseScale);
         poseStack.mulPose(Axis.ZP.rotationDegrees(subtleRotZ));
         poseStack.mulPose(Axis.XP.rotationDegrees(subtleRotX));

         // 计算紫红颜色渐变 (r: 176->255, g: 38->77, b: 255)
         float colorSine = (Mth.sin((float) (time * 0.0035D)) + 1.0F) * 0.5F; // 0.0 ~ 1.0
         int r = (int) Mth.lerp(colorSine, 176.0F, 255.0F); // 0xB0 -> 0xFF
         int g = (int) Mth.lerp(colorSine, 38.0F, 77.0F); // 0x26 -> 0x4D
         int b = 255;
         int alpha = 230;
         int primaryColor = (alpha << 24) | (r << 16) | (g << 8) | b;

         if (target.contains("luminous") || target.contains("effect")) {
            // 1. 设置渲染颜色并绘制发光层
            BladeRenderState.setCol(primaryColor, true);
            BladeRenderState.renderOverridedLuminous(
                  event.getStack(),
                  event.getModel(),
                  event.getTarget(),
                  event.getTexture(),
                  poseStack,
                  event.getBuffer(),
                  BladeRenderState.MAX_LIGHT); // 使用最大亮度 15 (MAX_LIGHT)

            // 2. 绘制金黄色流光帧动画
            int frame = (int) ((time / 40L) % FLOW_FRAMES);
            int flowColor = 0xFFCC3AFF;
            BladeRenderState.setCol(flowColor, true);
            BladeRenderState.renderChargeEffect(
                  event.getStack(),
                  frame,
                  event.getModel(),
                  event.getTarget(),
                  event.getTexture(),
                  poseStack,
                  event.getBuffer(),
                  BladeRenderState.MAX_LIGHT);

            // 3. 绘制反向反光层
            int reverseColor = 0xAA6A00FF;
            BladeRenderState.setCol(reverseColor, true);
            BladeRenderState.renderOverridedReverseLuminous(
                  event.getStack(),
                  event.getModel(),
                  event.getTarget(),
                  event.getTexture(),
                  poseStack,
                  event.getBuffer(),
                  BladeRenderState.MAX_LIGHT);
         } else {
            // 刀鞘基础模型的渐变流光
            int subFrame = (int) ((time / 60L) % FLOW_FRAMES);
            BladeRenderState.setCol(0x80B026FF, true);
            BladeRenderState.renderChargeEffect(
                  event.getStack(),
                  subFrame,
                  event.getModel(),
                  event.getTarget(),
                  event.getTexture(),
                  poseStack,
                  event.getBuffer(),
                  BladeRenderState.MAX_LIGHT);
         }
      } finally {
         poseStack.popPose();
      }
   }

   /**
    * 刀身发光部位增强（帧率色调插值与充能动画叠加）。
    */
   private static void renderEnhancedBladeLuminous(RenderOverrideEvent event, long time) {
      int frame = (int) ((time / 50L) % FLOW_FRAMES);
      float pulse = (Mth.sin((float) (time * 0.005D)) + 1.0F) * 0.5F;
      int r = (int) Mth.lerp(pulse, 200.0F, 255.0F);
      int g = (int) Mth.lerp(pulse, 50.0F, 120.0F);
      int b = 255;
      int glowColor = (240 << 24) | (r << 16) | (g << 8) | b;

      BladeRenderState.setCol(glowColor, true);
      BladeRenderState.renderChargeEffect(
            event.getStack(),
            frame,
            event.getModel(),
            event.getTarget(),
            event.getTexture(),
            event.getPoseStack(),
            event.getBuffer(),
            BladeRenderState.MAX_LIGHT);
   }

   // /** 旋转光环太容易冲突，我ban掉了
   // * 物品模式下交错旋转的光环矩阵渲染。
   // */
   // private static void renderRotatingItemHalo(RenderOverrideEvent event, long
   // time) {
   // float rotation = (time % 12000L) * 0.035F; // 顺时针持续旋转角度
   // float haloPulse = 1.0F + Mth.sin((float) (time * 0.006D)) * 0.05F; // 正弦脉冲

   // PoseStack poseStack = event.getPoseStack();
   // poseStack.pushPose();

   // try {
   // // 沿 Z 轴旋转与脉冲缩放
   // poseStack.scale(haloPulse, haloPulse, haloPulse);
   // poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));

   // BladeRenderState.setCol(0xF4E84DFF, true);
   // BladeRenderState.renderOverridedLuminous(
   // event.getStack(),
   // event.getModel(),
   // ITEM_HALO_TARGET,
   // NightfallDragonDefinitions.HALO_TEXTURE,
   // poseStack,
   // event.getBuffer(),
   // BladeRenderState.MAX_LIGHT);
   // } finally {
   // poseStack.popPose();
   // }
   // }
}
