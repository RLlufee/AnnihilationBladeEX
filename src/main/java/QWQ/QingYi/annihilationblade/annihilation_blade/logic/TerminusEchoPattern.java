package QWQ.QingYi.annihilationblade.annihilation_blade.logic;

import net.minecraft.world.phys.Vec3;

/**
 * 归墟回响每一波的几何规则（服务端判定与客户端渲染共用，保证光效与判定带严丝合缝）。
 * <p>
 * 第 0 波位于正中；之后各波左右交替偏移并逐波抬高，射程与宽度逐波增加。
 */
public final class TerminusEchoPattern {
   private TerminusEchoPattern() {
   }

   /** 横向偏移（格，沿 right 方向）：0, -2.65, +3.5, -4.35, +5.2 …… */
   public static double side(int wave) {
      return wave == 0 ? 0.0 : (wave % 2 == 0 ? 1.0 : -1.0) * (1.8 + wave * 0.85);
   }

   /** 竖直抬升（格）。 */
   public static double lift(int wave) {
      return wave * 0.28;
   }

   public static double range(double baseRange, int wave) {
      return baseRange + wave * 2.5;
   }

   public static double width(double baseWidth, int wave) {
      return baseWidth + wave * 0.35;
   }

   /** 该波的起点：序列起点 + 横向偏移 + 竖直抬升。 */
   public static Vec3 start(Vec3 sequenceStart, Vec3 right, int wave) {
      return sequenceStart.add(right.scale(side(wave))).add(0.0, lift(wave), 0.0);
   }
}
