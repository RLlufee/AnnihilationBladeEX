package QWQ.QingYi.annihilationblade.render.client.core;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.render.client.pipeline.FxPipeline;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Matrix4f;

/**
 * 客户端特效调度中心：持有活跃实例、驱动 tick、挂接渲染阶段事件、换算镜头震动。
 * <p>
 * 渲染时序（均为 Forge 原生事件，不需要额外 Mixin）：
 * <ul>
 *   <li>AFTER_SKY：记录本帧视图/投影矩阵。</li>
 *   <li>AFTER_WEATHER：复制场景深度（早于 Fabulous 透明度后处理改写主深度）。</li>
 *   <li>AFTER_LEVEL：在 LevelRenderer.renderLevel 返回之后执行完整管线（光影包此时已完成合成）。</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Annihilationblade.MODID, value = Dist.CLIENT)
public final class FxManager {
   private static final int MAX_ACTIVE = 32;
   private static final List<FxInstance> ACTIVE = new ArrayList<>();
   private static final FxScreenState SCREEN = new FxScreenState();
   private static final Matrix4f VIEW = new Matrix4f();
   private static final Matrix4f PROJECTION = new Matrix4f();
   private static boolean matricesCaptured;
   private static float shake;

   private FxManager() {
   }

   public static void add(FxInstance instance) {
      while (ACTIVE.size() >= MAX_ACTIVE) {
         ACTIVE.remove(0).onRemoved();
      }
      ACTIVE.add(instance);
   }

   public static void clear() {
      for (FxInstance instance : ACTIVE) {
         instance.onRemoved();
      }
      ACTIVE.clear();
      shake = 0.0F;
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.END) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      if (level == null) {
         if (!ACTIVE.isEmpty()) {
            clear();
         }
         return;
      }
      if (minecraft.isPaused() || ACTIVE.isEmpty()) {
         return;
      }
      boolean rendererActive = FxPipeline.INSTANCE.active();
      Iterator<FxInstance> iterator = ACTIVE.iterator();
      while (iterator.hasNext()) {
         FxInstance instance = iterator.next();
         try {
            instance.advance(level, rendererActive);
         } catch (RuntimeException exception) {
            Annihilationblade.LOGGER.error("[AnnihilationBlade FX] Effect tick failed, removing {}", instance.getClass().getSimpleName(), exception);
            iterator.remove();
            continue;
         }
         if (instance.finished()) {
            iterator.remove();
            instance.onRemoved();
         }
      }
   }

   @SubscribeEvent
   public static void onRenderStage(RenderLevelStageEvent event) {
      RenderLevelStageEvent.Stage stage = event.getStage();
      if (stage == RenderLevelStageEvent.Stage.AFTER_SKY || stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
         if (stage == RenderLevelStageEvent.Stage.AFTER_SKY || !matricesCaptured) {
            if (FxCompat.shadowPass()) {
               return;
            }
            VIEW.set(event.getPoseStack().last().pose());
            PROJECTION.set(event.getProjectionMatrix());
            matricesCaptured = true;
            FxPipeline.INSTANCE.beginFrame();
         }
      } else if (stage == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
         if (!ACTIVE.isEmpty() && matricesCaptured && !FxCompat.shadowPass() && FxPipeline.INSTANCE.active()) {
            FxPipeline.INSTANCE.captureDepth();
         }
      } else if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
         try {
            renderFrame(event.getCamera(), event.getPartialTick());
         } finally {
            matricesCaptured = false;
         }
      }
   }

   private static void renderFrame(Camera camera, float partialTick) {
      shake = 0.0F;
      if (ACTIVE.isEmpty() || !matricesCaptured || !FxPipeline.INSTANCE.active()) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      if (level == null) {
         return;
      }
      Vec3 cameraPos = camera.getPosition();
      float time = ((level.getGameTime() % 72000L) + partialTick) / 20.0F;
      FxFrame frame = new FxFrame(
         level, cameraPos, partialTick, time, VIEW, PROJECTION, camera.getLeftVector(), camera.getUpVector(),
         minecraft.getMainRenderTarget().width, minecraft.getMainRenderTarget().height
      );
      List<FxInstance> visible = new ArrayList<>();
      SCREEN.reset();
      float strongestShake = 0.0F;
      for (FxInstance instance : ACTIVE) {
         if (instance.focus().distanceTo(cameraPos) <= instance.cullDistance()) {
            visible.add(instance);
            instance.screen(frame, SCREEN);
            strongestShake = Math.max(strongestShake, instance.shake(frame));
         }
      }
      shake = strongestShake;
      if (!visible.isEmpty()) {
         FxPipeline.INSTANCE.render(frame, visible, SCREEN);
      }
   }

   /** 镜头震动：在相机角度计算阶段叠加多频正弦抖动，强度按平方衰减更自然。 */
   @SubscribeEvent
   public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
      float amount = shake * shake * (float)(double)ModConfig.CLIENT.clientRenderer.screenShake.getValue();
      if (amount <= 1.0E-4F) {
         return;
      }
      double t = (System.nanoTime() % 100_000_000_000L) / 1.0E9;
      float yaw = (float)(Math.sin(t * 37.0) * 0.6 + Math.sin(t * 61.0) * 0.4) * 2.4F * amount;
      float pitch = (float)(Math.sin(t * 43.0 + 1.3) * 0.6 + Math.sin(t * 71.0) * 0.4) * 2.0F * amount;
      float roll = (float)Math.sin(t * 29.0 + 0.7) * 2.6F * amount;
      event.setYaw(event.getYaw() + yaw);
      event.setPitch(event.getPitch() + pitch);
      event.setRoll(event.getRoll() + roll);
   }

   @SubscribeEvent
   public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      clear();
      FxPipeline.INSTANCE.release();
   }
}
