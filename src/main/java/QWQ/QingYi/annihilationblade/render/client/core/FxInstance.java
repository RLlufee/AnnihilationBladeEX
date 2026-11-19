package QWQ.QingYi.annihilationblade.render.client.core;

import QWQ.QingYi.annihilationblade.render.client.geometry.FxGeometry;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 客户端特效实例。生命周期：{@link FxManager#add} → 每 tick {@link #tick} → 每帧渲染回调 → {@link #finished()} 后移除。
 * <p>
 * 实例本身始终存在（即使渲染器被关闭），以保证音效等时间轴事件照常触发；
 * 渲染器不可用时改为调用 {@link #tickFallback}，用少量原版粒子做降级表现。
 */
@OnlyIn(Dist.CLIENT)
public abstract class FxInstance {
   protected int age;
   protected int lifetime;
   private int handle = -1;

   protected FxInstance(int lifetime) {
      this.lifetime = Math.max(1, lifetime);
   }

   // ───────────── 持续特效（FxSustainPacket）钩子 ─────────────

   /** 由调度器调用：绑定服务端句柄，并跳到服务端已运行的时间点（中途进入范围的玩家）。 */
   public final void bindSustained(int handle, int elapsed, int duration) {
      this.handle = handle;
      this.age = Math.max(0, elapsed);
      this.onSustainedStart(duration);
   }

   public final int handle() {
      return this.handle;
   }

   /** 子类据服务端给定的持续时间设置自身 lifetime（通常为 duration + 收尾动画长度）。 */
   protected void onSustainedStart(int duration) {
      this.lifetime = Math.max(this.lifetime, duration);
   }

   /** 服务端更新了位置/半径等参数。 */
   public void onSustainedUpdate(net.minecraft.client.multiplayer.ClientLevel level, QWQ.QingYi.annihilationblade.render.network.FxEventPacket event) {
   }

   /** 常驻类特效（光环等）续命：只要条件持续成立就不断调用，条件消失后自然走完剩余寿命淡出。 */
   public void extendLife(int ticks) {
      this.lifetime = Math.max(this.lifetime, this.age + Math.max(1, ticks));
   }

   /** 剩余寿命（tick），常驻特效用于淡出。 */
   public int remainingTicks() {
      return Math.max(0, this.lifetime - this.age);
   }

   /** 服务端提前结束；默认立即结束，子类可改为播放收尾动画。 */
   public void onSustainedStop() {
      this.lifetime = Math.min(this.lifetime, this.age + 1);
   }

   /** 世界坐标焦点，用于距离剔除。 */
   public abstract Vec3 focus();

   /** 超过该距离（格）时不再渲染。 */
   public double cullDistance() {
      return 192.0;
   }

   /** 每 tick 调用（客户端逻辑，例如播放时间轴音效、更新火花）。 */
   public void tick(ClientLevel level) {
   }

   /** 渲染器不可用时每 tick 调用，用原版粒子降级表现。 */
   public void tickFallback(ClientLevel level) {
   }

   /** 绘制网格类特效（写入 HDR 特效层）。 */
   public void renderGeometry(FxGeometry geometry, FxFrame frame) {
   }

   /** 提交 B 档全屏通道。 */
   public void collectPasses(FxFrame frame, List<FxPass> out) {
   }

   /** 写入屏幕级效果（闪白、色差、径向模糊等）。 */
   public void screen(FxFrame frame, FxScreenState screen) {
   }

   /** 屏幕震动强度（0..1），由管理器换算为镜头抖动。 */
   public float shake(FxFrame frame) {
      return 0.0F;
   }

   public void onRemoved() {
   }

   final void advance(ClientLevel level, boolean rendererActive) {
      this.tick(level);
      if (!rendererActive) {
         this.tickFallback(level);
      }
      this.age++;
   }

   public boolean finished() {
      return this.age >= this.lifetime;
   }

   /** 带 partialTick 的年龄（tick）。 */
   public float age(float partialTick) {
      return Math.min(this.age + partialTick, this.lifetime);
   }

   public int ageTicks() {
      return this.age;
   }
}
