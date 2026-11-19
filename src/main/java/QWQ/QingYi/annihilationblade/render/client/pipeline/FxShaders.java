package QWQ.QingYi.annihilationblade.render.client.pipeline;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * 渲染器全部着色器均注册为原版 {@link ShaderInstance}（core shader）。
 * <p>
 * 这样做的原因：原版对 GL 程序、纹理单元、VAO、混合状态都有缓存，若绕过它直接调用 GL，
 * 很容易让原版缓存与真实状态不一致，导致后续方块/实体渲染错乱。走 ShaderInstance + BufferUploader
 * 可以让所有状态变更都经过原版缓存。
 * <p>
 * 注意：json 中禁止声明 "blend" 字段——ShaderInstance 会在每次 apply 时强制套用并改写全局混合状态缓存，
 * 混合模式统一由 {@link FxPipeline} 用 RenderSystem 显式设置。
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Annihilationblade.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public final class FxShaders {
   @Nullable public static ShaderInstance world;
   @Nullable public static ShaderInstance depthCopy;
   @Nullable public static ShaderInstance bloomDown;
   @Nullable public static ShaderInstance bloomUp;
   @Nullable public static ShaderInstance composite;
   @Nullable public static ShaderInstance fractureScene;
   @Nullable public static ShaderInstance fractureVolume;
   @Nullable public static ShaderInstance vacuumScene;
   @Nullable public static ShaderInstance vacuumVolume;
   @Nullable public static ShaderInstance descentScene;
   @Nullable public static ShaderInstance descentVolume;

   private static int failures;

   private FxShaders() {
   }

   @SubscribeEvent
   public static void onRegisterShaders(RegisterShadersEvent event) {
      failures = 0;
      register(event, "fx_world", DefaultVertexFormat.POSITION_COLOR_TEX, shader -> world = shader);
      register(event, "fx_depth_copy", DefaultVertexFormat.POSITION, shader -> depthCopy = shader);
      register(event, "fx_bloom_down", DefaultVertexFormat.POSITION, shader -> bloomDown = shader);
      register(event, "fx_bloom_up", DefaultVertexFormat.POSITION, shader -> bloomUp = shader);
      register(event, "fx_composite", DefaultVertexFormat.POSITION, shader -> composite = shader);
      register(event, "fx_fracture_scene", DefaultVertexFormat.POSITION, shader -> fractureScene = shader);
      register(event, "fx_fracture_volume", DefaultVertexFormat.POSITION, shader -> fractureVolume = shader);
      register(event, "fx_vacuum_scene", DefaultVertexFormat.POSITION, shader -> vacuumScene = shader);
      register(event, "fx_vacuum_volume", DefaultVertexFormat.POSITION, shader -> vacuumVolume = shader);
      register(event, "fx_descent_scene", DefaultVertexFormat.POSITION, shader -> descentScene = shader);
      register(event, "fx_descent_volume", DefaultVertexFormat.POSITION, shader -> descentVolume = shader);
      if (failures == 0) {
         Annihilationblade.LOGGER.info("[AnnihilationBlade FX] All renderer shaders registered.");
      }
      FxPipeline.INSTANCE.onShadersReloaded();
   }

   private static void register(RegisterShadersEvent event, String name, VertexFormat format, Consumer<ShaderInstance> sink) {
      try {
         event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Annihilationblade.MODID, name), format), sink);
      } catch (IOException | RuntimeException exception) {
         failures++;
         sink.accept(null);
         Annihilationblade.LOGGER.error("[AnnihilationBlade FX] Failed to load shader {}: {}", name, exception.toString());
      }
   }

   /** 渲染器运行所需的核心着色器是否齐全。 */
   public static boolean coreReady() {
      return world != null && depthCopy != null && composite != null;
   }
}
