package QWQ.QingYi.annihilationblade.infinity_stellaris.logic;

import QWQ.QingYi.annihilationblade.common.ServerTickScheduler;
import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.infinity_stellaris.InfinityStellarisDefinitions;
import QWQ.QingYi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 真空衰变坍缩 (Vacuum Decay Collapse) 核心逻辑类
 * 
 * 本类演示了大型终极技能的完整实现机制，包含以下萌新值得学习的技术点：
 * 
 * 玩家视线射线追踪 (Raycasting / ClipContext)：根据玩家视角高精度计算远处目标点。
 * 无实体依赖的延迟 Tick 任务调度 (ServerTickScheduler)：利用递归 Lambda
 * 表达式实现多帧动画与持续结算。
 * 彻底抹杀机制与掉落物拦截：结合 PersistentData (NBT) 标记与 Forge
 * 事件监听器阻止物品与经验掉落。
 * 空间几何粒子渲染算法：包含立方体外框插值采样（spawnSquare）、四角柱体粒子阵（spawnCornerPillars）以及基于三角函数的浮动动画。
 * 
 */
@EventBusSubscriber(modid = "annihilationblade")
public final class VacuumDecayCollapseLogic {
   /** 用于在生物 PersistentData 中标记“彻底抹杀、禁止掉落物品/经验”的 NBT 键名 */
   private static final String NO_DROPS_TAG = "AnnihilationBladeAbsoluteAnnihilationNoDrops";

   /** 坍缩湮灭结界的持续总时间（单位：Tick，100 Ticks = 5 秒） */
   private static final int ZONE_DURATION_TICKS = 100;

   /** 结界水平方向半长（正方形边长为 64.0 * 2 = 128.0 格） */
   private static final double ZONE_HALF_SIZE = 64.0;

   /** 结界垂直方向高度（64.0 格） */
   private static final double ZONE_HEIGHT = 64.0;

   /** 射线瞄准的最大搜索距离（512 格） */
   private static final double CAST_RANGE = 512.0;

   /** 当射线未击中任何方块时，退而求其次使用的默认前向距离（128 格） */
   private static final double FALLBACK_RANGE = 128.0;

   /** 每个 Tick 在结界区域内随机生成的伽马雷电数量 */
   private static final int BOLTS_PER_TICK = 12;

   private VacuumDecayCollapseLogic() {
   }

   /**
    * 准备施法：检查并初始化玩家手持无尽星空刀的属性。
    *
    * @param player 施法玩家
    */
   public static void prepareCast(Player player) {
      ItemStack blade = InfinityStellarisItemSupport.heldInfinityStellaris(player);
      if (!blade.isEmpty()) {
         // 确保武器基础面板与全屏抹杀等特殊数值初始化
         InfinityStellarisDefinitions.ensureStats(blade, player.level());
      }
   }

   /**
    * 释放真空衰变坍缩技能入口。
    *
    * @param player 施法玩家
    */
   public static void unleash(Player player) {
      // 必须在服务端运行，且玩家当前手持无尽星空武器
      if (!(player.level() instanceof ServerLevel level)
            || !InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
         return;
      }

      // 1. 计算技能施放目标中心坐标（视线落点）
      Vec3 center = findCastCenter(level, player);

      // 2. 触发瞬间音效与伽马雷暴爆发效果
      GammaThunderburstLogic.trigger(player);

      // 3. 生成结界开启时的剧烈粒子与音效爆炸
      spawnZoneOpening(level, center);

      // 4. 开启结界 Tick 循环，传入结界数据上下文对象
      tickZone(new AnnihilationZone(level, player, center));
   }

   /**
    * Forge 实体掉落物事件监听器。
    * 当被标记为 {@link #NO_DROPS_TAG} 的生物死亡时，清空其掉落物列表，实现“蒸发/湮灭”效果。
    */
   @SubscribeEvent
   public static void onLivingDrops(LivingDropsEvent event) {
      if (event.getEntity().getPersistentData().getBoolean(NO_DROPS_TAG)) {
         event.getDrops().clear();
      }
   }

   /**
    * Forge 实体经验掉落事件监听器。
    * 当被标记为 {@link #NO_DROPS_TAG} 的生物死亡时，将掉落经验设为 0。
    */
   @SubscribeEvent
   public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
      if (event.getEntity().getPersistentData().getBoolean(NO_DROPS_TAG)) {
         event.setDroppedExperience(0);
      }
   }

   /**
    * 使用射线追踪（Raycast）获取玩家视角前方的施法中心坐标。
    *
    * @param level  服务端世界
    * @param player 施法玩家
    * @return 施法目标点三维坐标 Vec3
    */
   private static Vec3 findCastCenter(ServerLevel level, Player player) {
      Vec3 start = player.getEyePosition(); // 玩家眼睛起点坐标
      Vec3 look = player.getLookAngle(); // 视线归一化方向向量
      Vec3 end = start.add(look.scale(CAST_RANGE)); // 碰撞检测终点

      // 构造方块碰撞物理检测上下文 (不忽略固体方块，忽略液体)
      BlockHitResult hit = level
            .clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

      // 如果击中了方块，取该方块几何中心；否则取前方 FALLBACK_RANGE 处坐标
      if (hit.getType() == HitResult.Type.BLOCK) {
         BlockPos pos = hit.getBlockPos();
         return Vec3.atCenterOf(pos);
      }

      return start.add(look.scale(FALLBACK_RANGE));
   }

   /**
    * 结界的主 Tick 循环函数。通过 {@link ServerTickScheduler} 实现无实体依赖的延迟递归调用。
    *
    * @param zone 当前结界运行上下文
    */
   private static void tickZone(AnnihilationZone zone) {
      // 玩家死亡则立即终止结界
      if (!zone.player.isAlive()) {
         return;
      }

      // 1. 扫描范围内目标并执行熵灭绝对抹杀
      scanAndExecute(zone);

      // 2. 渲染结界边界正方形框与四周立柱粒子
      spawnZoneFrame(zone.level, zone.center, zone.age);

      // 3. 在结界内随机轰炸伽马落雷
      GammaThunderburstLogic.spawnRandomBoltsInSquare(zone.level, zone.center, ZONE_HALF_SIZE, BOLTS_PER_TICK,
            zone.player.getRandom());

      zone.age++;

      // 4. 递归调度下一个 Tick（1 Tick 后再次调用 tickZone）
      if (zone.age < ZONE_DURATION_TICKS) {
         ServerTickScheduler.schedule(1, () -> tickZone(zone));
      }
   }

   /**
    * 扫描结界 AABB 范围内的所有活体生物，并对其施加无视防御与死亡判定的绝对抹杀。
    *
    * @param zone 当前结界运行上下文
    */
   private static void scanAndExecute(AnnihilationZone zone) {
      // 构造 centered 在 zone.center 处的轴对齐包围盒 (AABB)
      AABB killBox = AABB.ofSize(zone.center, ZONE_HALF_SIZE * 2.0, ZONE_HEIGHT, ZONE_HALF_SIZE * 2.0);

      // 搜索范围内所有可攻击的活体实体
      List<LivingEntity> targets = zone.level.getEntitiesOfClass(LivingEntity.class, killBox,
            entity -> SlashBladeTargeting.canAttack(zone.player, entity));

      for (LivingEntity target : targets) {
         // 标记为“彻底抹杀”，触发上述事件监听清除掉落物
         target.getPersistentData().putBoolean(NO_DROPS_TAG, true);

         // 执行终极熵灭溶解（将生命值设为 0、绕过不死图腾与无敌帧）
         EntropyDissolutionLogic.executeFinal(target, zone.player);
      }
   }

   /**
    * 爆发初期的强效视觉与音效冲击。
    */
   private static void spawnZoneOpening(ServerLevel level, Vec3 center) {
      // 播放多重音效交叠（末影龙、复活锚、威瑟出生音效），营造宏大仪式感
      level.playSound(null, center.x, center.y, center.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 6.0F,
            0.35F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
            SoundSource.PLAYERS, 5.0F, 0.35F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 5.0F,
            0.65F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 4.0F, 0.45F);

      // 发送大量大范围粒子爆炸
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 12, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 1600, ZONE_HALF_SIZE * 0.65,
            ZONE_HEIGHT * 0.35, ZONE_HALF_SIZE * 0.65, 1.1);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 700, ZONE_HALF_SIZE * 0.45,
            ZONE_HEIGHT * 0.25, ZONE_HALF_SIZE * 0.45, 0.28);
      level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 0.8, center.z, 8, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 500, ZONE_HALF_SIZE * 0.45,
            ZONE_HEIGHT * 0.25, ZONE_HALF_SIZE * 0.45, 0.18);
      level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 360, ZONE_HALF_SIZE * 0.5,
            ZONE_HEIGHT * 0.2, ZONE_HALF_SIZE * 0.5, 0.16);
   }

   /**
    * 渲染每一帧结界框架粒子（浮动正方形边框与四角传送门柱）。
    *
    * @param level  服务端世界
    * @param center 结界中心点
    * @param age    结界当前持续的 Tick 步数
    */
   private static void spawnZoneFrame(ServerLevel level, Vec3 center, int age) {
      // 每 20 Ticks (1秒) 播放一次律动充能音效，音调随时间微升
      if (age % 20 == 0) {
         level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS,
               2.0F, 0.55F + (age % 40) * 0.01F);
      }

      // 生成水平正方形动态边框
      spawnSquare(level, center, ZONE_HALF_SIZE, age);
      // 生成四个顶点的垂直柱体
      spawnCornerPillars(level, center, ZONE_HALF_SIZE, ZONE_HEIGHT, age);

      // 中心扩散电火花与反向传送门粒子
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 80, 2.0, 1.2, 2.0, 0.45);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + Math.sin(age * 0.25) * 2.0, center.z, 36,
            3.0, 1.4, 3.0, 0.18);
      if (age % 5 == 0) {
         level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y + 1.0, center.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   /**
    * 利用插值算法在正方形的四条边上均匀平铺生成粒子点，带有正弦函数（Math.sin）波动的上下浮动特效。
    *
    * @param level    服务端世界
    * @param center   中心点
    * @param halfSize 半边长
    * @param age      时间步数
    */
   private static void spawnSquare(ServerLevel level, Vec3 center, double halfSize, int age) {
      // Y轴高度随着正弦波做上下小幅度浮动
      double y = center.y + 0.12 + Math.sin(age * 0.35) * 0.35;
      int samples = 32; // 每条边采样 32 个粒子点

      for (int i = 0; i <= samples; i++) {
         // 从 -halfSize 到 +halfSize 线性插值计算偏移量
         double offset = -halfSize + halfSize * 2.0 * i / samples;

         // 4 条边上的对应坐标点
         spawnBoundaryPoint(level, center.x + offset, y, center.z - halfSize); // 北边
         spawnBoundaryPoint(level, center.x + offset, y, center.z + halfSize); // 南边
         spawnBoundaryPoint(level, center.x - halfSize, y, center.z + offset); // 西边
         spawnBoundaryPoint(level, center.x + halfSize, y, center.z + offset); // 东边
      }
   }

   /**
    * 在指定边界坐标点处生成高亮度末影棒与电火花粒子。
    */
   private static void spawnBoundaryPoint(ServerLevel level, double x, double y, double z) {
      level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.03, 0.03, 0.03, 0.0);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
   }

   /**
    * 渲染正方形结界的 4 个角上的垂直立体粒子柱。
    *
    * @param level    服务端世界
    * @param center   中心点
    * @param halfSize 半边长
    * @param height   柱体总高度
    * @param age      时间步数
    */
   private static void spawnCornerPillars(ServerLevel level, Vec3 center, double halfSize, double height, int age) {
      double[] xs = new double[] { center.x - halfSize, center.x + halfSize };
      double[] zs = new double[] { center.z - halfSize, center.z + halfSize };

      // 遍历四个角 (x, z)
      for (double x : xs) {
         for (double z : zs) {
            // 沿 Y 轴向上分为 8 段采样粒子
            for (int step = 0; step <= 8; step++) {
               double y = center.y - height * 0.5 + height * step / 8.0;
               level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 2, 0.08, 0.08, 0.08, 0.1);
               // 交替律动线条
               if ((step + age) % 2 == 0) {
                  level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.04, 0.04, 0.04, 0.0);
               }
            }
         }
      }
   }

   /**
    * 内部静态类：封装真空湮灭结界在运行期间的状态数据上下文（世界、玩家、坐标中心与已运行时间）。
    */
   private static final class AnnihilationZone {
      private final ServerLevel level;
      private final Player player;
      private final Vec3 center;
      private int age;

      private AnnihilationZone(ServerLevel level, Player player, Vec3 center) {
         this.level = level;
         this.player = player;
         this.center = center;
      }
   }
}
