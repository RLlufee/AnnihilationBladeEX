package QWQ.QingYi.annihilationblade.nightfall_dragon.logic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * 「神陨·宇宙夜陨降临」的时间轴与星雨落点规则（服务端伤害与客户端渲染共用，保证流星精确砸在伤害结算点上）。
 * <p>
 * 落点抖动使用由特效种子创建的 {@link RandomSource}，服务端与客户端按「波次 → 每波剑序」的相同顺序取数，
 * 因此只需同步一个 long 种子即可在客户端完整复现所有落点。本类为双端通用代码，禁止引用客户端类。
 */
public final class CosmicDescentPattern {
   /** 第一波星雨落地的 tick。 */
   public static final int FIRST_WAVE_TICK = 6;
   /** 每波间隔 tick。 */
   public static final int WAVE_INTERVAL = 2;
   /** 暗星坍缩（终极爆发）的 tick。 */
   public static final int COLLAPSE_TICK = 38;
   /** 坍缩时落雷的数量与三重环半径。 */
   public static final int COLLAPSE_BOLTS = 24;

   private CosmicDescentPattern() {
   }

   public static int waveTick(int wave) {
      return FIRST_WAVE_TICK + wave * WAVE_INTERVAL;
   }

   /** 时间轴总长（服务端最后一个事件的 tick）。 */
   public static int duration(int waves) {
      return Math.max(COLLAPSE_TICK, waveTick(Math.max(0, waves - 1)));
   }

   /** 第 wave 波第 sword 把剑的落点；每次调用消耗 rng 的一个 double。 */
   public static Vec3 impact(Vec3 focal, int wave, int sword, int swordsPerWave, RandomSource rng) {
      double angle = wave * 0.85 + sword * (Math.PI * 2.0 / Math.max(1, swordsPerWave)) + rng.nextDouble() * 0.5;
      double offsetRadius = wave % 3 == 0 ? 1.5 : 7.5 + (wave % 5) * 3.6;
      return focal.add(Math.cos(angle) * offsetRadius, 0.0, Math.sin(angle) * offsetRadius);
   }

   /** 坍缩落雷位置（三重环 12 / 24 / 36 格）。 */
   public static Vec3 collapseBolt(Vec3 focal, int index) {
      double angle = index * (Math.PI * 2.0 / COLLAPSE_BOLTS);
      double r = 12.0 + (index % 3) * 12.0;
      return focal.add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
   }

   /** 把波数与每波剑数打包进特效包的 intParam。 */
   public static int pack(int waves, int swordsPerWave) {
      return (waves & 0xFF) | (swordsPerWave & 0xFF) << 8;
   }

   public static int unpackWaves(int packed) {
      return packed & 0xFF;
   }

   public static int unpackSwords(int packed) {
      return packed >> 8 & 0xFF;
   }
}
