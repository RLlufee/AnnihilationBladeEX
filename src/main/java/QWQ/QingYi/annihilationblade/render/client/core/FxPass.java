package QWQ.QingYi.annihilationblade.render.client.core;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 全屏通道（B 档）。
 * <ul>
 *   <li>{@link Kind#SCENE}：读取当前场景颜色并输出新的场景颜色（空间碎裂折射、黑洞透镜、冲击波扭曲）。</li>
 *   <li>{@link Kind#VOLUME}：光线步进生成的体积发光，以加法混合累积到体积发光层，随后参与泛光。</li>
 * </ul>
 * 着色器约定：Sampler0 = 场景颜色（仅 SCENE），Sampler1 = 场景深度（R 通道为原始深度）。
 */
@OnlyIn(Dist.CLIENT)
public record FxPass(Kind kind, Supplier<ShaderInstance> shader, Consumer<ShaderInstance> uniforms) {
   public enum Kind {
      SCENE,
      VOLUME
   }
}
