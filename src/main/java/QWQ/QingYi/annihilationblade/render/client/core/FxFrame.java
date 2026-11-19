package QWQ.QingYi.annihilationblade.render.client.core;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * 单帧渲染上下文。所有特效几何都使用「相对相机」坐标，以避免远离原点时的浮点抖动。
 */
@OnlyIn(Dist.CLIENT)
public final class FxFrame {
   public final ClientLevel level;
   public final Vec3 camera;
   public final float partialTick;
   /** 客户端秒数（带 partialTick），用于着色器动画。 */
   public final float time;
   public final Matrix4f view;
   public final Matrix4f projection;
   public final Matrix4f inverseView;
   public final Matrix4f inverseProjection;
   public final Matrix4f viewProjection;
   /** 相机左/上方向（世界空间单位向量），用于公告板与条带朝向相机。 */
   public final Vector3f left;
   public final Vector3f up;
   public final int width;
   public final int height;

   FxFrame(ClientLevel level, Vec3 camera, float partialTick, float time, Matrix4f view, Matrix4f projection, Vector3f left, Vector3f up, int width, int height) {
      this.level = level;
      this.camera = camera;
      this.partialTick = partialTick;
      this.time = time;
      this.view = new Matrix4f(view);
      this.projection = new Matrix4f(projection);
      this.inverseView = new Matrix4f(view).invert();
      this.inverseProjection = new Matrix4f(projection).invert();
      this.viewProjection = new Matrix4f(projection).mul(view);
      this.left = new Vector3f(left);
      this.up = new Vector3f(up);
      this.width = width;
      this.height = height;
   }

   public Vector3f rel(Vec3 world) {
      return new Vector3f((float)(world.x - this.camera.x), (float)(world.y - this.camera.y), (float)(world.z - this.camera.z));
   }

   /** 世界坐标投影到屏幕 UV（0..1，左下为原点）；位于相机背后时返回 null。 */
   public float[] screenUv(Vec3 world) {
      Vector3f rel = this.rel(world);
      Vector4f clip = this.viewProjection.transform(new Vector4f(rel.x, rel.y, rel.z, 1.0F));
      if (clip.w <= 0.05F) {
         return null;
      }
      return new float[]{clip.x / clip.w * 0.5F + 0.5F, clip.y / clip.w * 0.5F + 0.5F};
   }
}
