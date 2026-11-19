package QWQ.QingYi.annihilationblade.infinity_stellaris.logic;

import QWQ.QingYi.annihilationblade.common.ServerTickScheduler;
import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.infinity_stellaris.InfinityStellarisDefinitions;
import QWQ.QingYi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;
import QWQ.QingYi.annihilationblade.render.FxSustained;
import QWQ.QingYi.annihilationblade.render.FxTypes;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.List;
import net.minecraft.core.BlockPos;
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
 * 持续特效同步（FxSustained）：结界视觉由客户端专属渲染器绘制，服务端只同步结界参数与生命周期。
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

   /** 每个 Tick 在结界区域内随机落下的伽马雷电数量（纯视觉，由客户端绘制） */
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

      // 3. 结界开启音效 + 启动客户端持续特效（取代原先数千个粒子与每 tick 生成的雷电实体）
      playZoneOpening(level, center);
      int fxHandle = startZoneFx(level, player, center);

      // 4. 开启结界 Tick 循环，传入结界数据上下文对象
      tickZone(new AnnihilationZone(level, player, center, fxHandle));
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
      // 玩家死亡则立即终止结界，并通知客户端提前进入崩解收尾
      if (!zone.player.isAlive()) {
         FxSustained.stop(zone.fxHandle);
         return;
      }

      // 1. 扫描范围内目标并执行熵灭绝对抹杀
      scanAndExecute(zone);

      // 2. 结界律动音效（帷幕、光柱、伽马雷暴等视觉由客户端 VacuumDecayFx 绘制）
      playZonePulse(zone.level, zone.center, zone.age);

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
    * 结界开启音效。视觉（真空泡成核、帷幕、奇点、伽马雷暴）全部由客户端专属渲染器 VacuumDecayFx 绘制。
    */
   private static void playZoneOpening(ServerLevel level, Vec3 center) {
      // 播放多重音效交叠（末影龙、复活锚、威瑟出生音效），营造宏大仪式感
      level.playSound(null, center.x, center.y, center.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 6.0F,
            0.35F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
            SoundSource.PLAYERS, 5.0F, 0.35F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 5.0F,
            0.65F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 4.0F, 0.45F);
   }

   /**
    * 结界律动音效：每 20 Ticks (1秒) 播放一次充能音效，音调随时间微升。
    */
   private static void playZonePulse(ServerLevel level, Vec3 center, int age) {
      if (age % 20 == 0) {
         level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS,
               2.0F, 0.55F + (age % 40) * 0.01F);
      }
   }

   /**
    * 启动持续特效：服务端只同步结界中心、尺寸与雷暴密度，中途进入范围的玩家会自动补发并跳过已播放部分。
    */
   private static int startZoneFx(ServerLevel level, Player player, Vec3 center) {
      return FxSustained.start(level, new FxEventPacket(
            FxTypes.VACUUM_DECAY,
            center,
            player.getLookAngle(),
            (float) ZONE_HALF_SIZE,
            (float) ZONE_HEIGHT,
            BOLTS_PER_TICK,
            player.getRandom().nextLong(),
            player.getId(),
            new int[0]), ZONE_DURATION_TICKS);
   }
   /**
    * 内部静态类：封装真空湮灭结界在运行期间的状态数据上下文（世界、玩家、坐标中心与已运行时间）。
    */
   private static final class AnnihilationZone {
      private final ServerLevel level;
      private final Player player;
      private final Vec3 center;
      private final int fxHandle;
      private int age;

      private AnnihilationZone(ServerLevel level, Player player, Vec3 center, int fxHandle) {
         this.level = level;
         this.player = player;
         this.center = center;
         this.fxHandle = fxHandle;
      }
   }
}
