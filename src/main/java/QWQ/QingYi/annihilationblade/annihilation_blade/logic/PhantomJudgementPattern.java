package QWQ.QingYi.annihilationblade.annihilation_blade.logic;

import net.minecraft.world.phys.Vec3;

/**
 * 幻影审判的剑阵几何（服务端召唤剑与客户端光效共用，保证星图、星落与原生召唤剑严丝合缝）。
 */
public final class PhantomJudgementPattern {
   private static final double TAU = Math.PI * 2.0;

   private PhantomJudgementPattern() {
   }

   /** 搜索期环绕中心：施法者脚底 + 体高 0.58。 */
   public static Vec3 orbitCenter(Vec3 feet, float height) {
      return feet.add(0.0, height * 0.58, 0.0);
   }

   public static double orbitRadius(int age, double visualScale) {
      return (2.1 + Math.sin(age * 0.45) * 0.22) * visualScale;
   }

   public static Vec3 orbitPosition(Vec3 center, int index, int count, int age, double radius) {
      double angle = TAU * index / count + age * 0.34;
      return center.add(Math.cos(angle) * radius, 0.25 + Math.sin(age * 0.28 + index) * 0.65, Math.sin(angle) * radius);
   }

   public static Vec3 orbitTangent(int index, int count, int age) {
      double next = TAU * index / count + age * 0.34 + 0.42;
      return new Vec3(-Math.sin(next), 0.16 * Math.cos(age * 0.2 + index), Math.cos(next)).normalize();
   }

   /** 审判时剑阵中心：施法者脚底 + 体高 0.55。 */
   public static Vec3 judgementCenter(Vec3 feet, float height) {
      return feet.add(0.0, height * 0.55, 0.0);
   }

   /** 第 targetIndex 个目标的星桥起点（剑阵上的方位）。 */
   public static Vec3 source(Vec3 judgementCenter, int targetIndex, int swordCount, double visualScale) {
      double angle = TAU * (targetIndex % swordCount) / swordCount;
      return judgementCenter.add(Math.cos(angle) * 2.4 * visualScale, 0.8 * visualScale, Math.sin(angle) * 2.4 * visualScale);
   }

   /** 目标上空的裂点。 */
   public static Vec3 split(Vec3 targetCenter, float targetHeight, double visualScale) {
      return targetCenter.add(0.0, (targetHeight * 0.75 + 4.2) * visualScale, 0.0);
   }

   public static double rainRadius(float targetWidth, double visualScale) {
      return Math.max(1.35, targetWidth * 1.95) * visualScale;
   }

   public static Vec3 rainOffset(int sword, int rainSwords, int targetIndex, double radius) {
      double angle = TAU * sword / rainSwords + targetIndex * 0.43;
      double distance = radius * (0.35 + sword % 3 * 0.32);
      return new Vec3(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
   }

   public static Vec3 rainStart(Vec3 split, Vec3 offset, int sword) {
      return split.add(offset.scale(0.72)).add(0.0, sword % 2 * 0.45, 0.0);
   }

   public static Vec3 rainEnd(Vec3 targetFeet, Vec3 offset) {
      return targetFeet.add(offset).add(0.0, 0.08, 0.0);
   }
}
