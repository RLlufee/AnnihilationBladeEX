package QWQ.QingYi.annihilationblade.nightfall_dragon.logic;

import QWQ.QingYi.annihilationblade.common.ServerTickScheduler;
import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationblade.infinity_stellaris.logic.GammaThunderburstLogic;
import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import java.util.ArrayList;
import java.util.List;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * <h1>永夜魔龙 - 次元斩 / 裁决切 (Judgement Cut) 核心逻辑类</h1>
 * <p>
 * 本类展示了类似《鬼泣》维吉尔万剑归宗/连续次元斩的技术实现：
 * <ul>
 *   <li><b>目标锚点收集与均匀圆盘极坐标采样</b>：优先追踪范围内真实敌对生物；若目标不足，使用 {@code radius * Math.sqrt(random)} 算法在圆形区域内均匀随机散布切痕点。</li>
 *   <li><b>地面与空间碰撞校验</b>：递归检测下方坚固方块 (isFaceSturdy) 与上方空气空间 (hasClearCutSpace)，确保次元斩不会生成在墙体内。</li>
 *   <li><b>拔刀剑 API 集成 (EntityJudgementCut)</b>：实例化 SlashBlade 的次元斩实体，自定义其色彩、尺寸缩放 (Rank/Scale) 与暴击判定。</li>
 *   <li><b>时间序列延迟调度</b>：利用 {@link ServerTickScheduler} 按照固定时间间隔 (INTERVAL_TICKS) 逐个触发 100 次次元斩连击。</li>
 * </ul>
 */
public final class NightfallDragonJudgementCutLogic {
   /** 随机尝试寻找有效平面的次数上限 */
   private static final int RANDOM_POSITION_ATTEMPTS = 16;
   private static final int TOTAL_RANDOM_POSITION_ATTEMPTS = 320;   

   private static double getRadius() {
      return ModConfig.COMMON.nightfallDragon.judgementCutRange.getValue();
   }

   private static int getTotalCuts() {
      return ModConfig.COMMON.nightfallDragon.judgementCutTotalCuts.getValue();
   }

   private static int getIntervalTicks() {
      return ModConfig.COMMON.nightfallDragon.judgementCutIntervalTicks.getValue();
   }

   private static double getDamage() {
      return ModConfig.COMMON.nightfallDragon.judgementCutDamage.getValue();
   }

   private static float getScale() {
      return ModConfig.COMMON.nightfallDragon.judgementCutScale.getValue().floatValue();
   }   

   private NightfallDragonJudgementCutLogic() {
   }

   /**
    * 蓄力/准备释放阶段：播放空间震颤音效与反向传送门汇聚粒子。
    */
   public static void prepareCast(Player player) {
      if (player instanceof ServerPlayer serverPlayer && canUseSealedArt(serverPlayer)) {
         ServerLevel level = serverPlayer.serverLevel();
         Vec3 center = serverPlayer.position().add(0.0, serverPlayer.getBbHeight() * 0.5, 0.0);
         level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 18, 1.2, 0.45, 1.2, 0.08);
         level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.75F);
      }
   }

   /**
    * 释放连环次元斩。
    */
   public static void unleash(Player player) {
      if (!(player instanceof ServerPlayer serverPlayer) || !canUseSealedArt(serverPlayer)) {
         return;
      }

      ServerLevel level = serverPlayer.serverLevel();
      // 1. 收集 100 个切痕锚点 (CutAnchor)
      List<CutAnchor> anchors = collectAnchors(level, serverPlayer);
      Vec3 center = serverPlayer.position().add(0.0, serverPlayer.getBbHeight() * 0.5, 0.0);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 1.3F, 0.55F);

      // 2. 将 100 次次元斩以 index * getIntervalTicks() 的时间差注册到定时任务队列中
      for (int i = 0; i < anchors.size(); i++) {
         int index = i;
         ServerTickScheduler.schedule(index * getIntervalTicks(), () -> spawnScheduledCut(serverPlayer, anchors.get(index), index));
      }
   }

   /**
    * 验证玩家是否可以释放封印形态奥义。
    */
   private static boolean canUseSealedArt(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      return NightfallDragonItemSupport.isNightfallDragon(stack) && NightfallDragonDefinitions.FORM_SEALED.equals(NightfallDragonDefinitions.getForm(stack));
   }

   /**
    * 收集所有次元斩的生成坐标锚点。优先锁定活体敌人，不足时在周身圆盘范围内随机采样。
    */
   private static List<CutAnchor> collectAnchors(ServerLevel level, ServerPlayer player) {
      int totalCuts = getTotalCuts();
      double radius = getRadius();
      List<CutAnchor> anchors = new ArrayList<>(totalCuts);
      Vec3 center = player.position();
      
      // 第一阶段：搜索 50 格半径内的敌人，在敌人身体处放置锚点
      List<LivingEntity> targets = SpecialEffectSupport.radialTargets(level, player, center, radius);
      for (LivingEntity target : targets) {
         anchors.add(CutAnchor.target(target.getId(), target.position()));
         if (anchors.size() >= totalCuts) {
            return anchors;
         }
      }

      // 第二阶段：如果敌人不足，使用极坐标随机产生地面有效坐标点补齐
      RandomSource random = player.getRandom();
      int attempts = 0;
      while (anchors.size() < totalCuts && attempts++ < TOTAL_RANDOM_POSITION_ATTEMPTS) {
         Vec3 position = randomPosition(level, player, random);
         if (position != null) {
            anchors.add(CutAnchor.fixed(position));
         }
      }

      // 第三阶段：后备保底落点
      Vec3 fallback = findNearbyFloor(level, player.getX(), player.getY(), player.getZ());
      while (anchors.size() < totalCuts && fallback != null) {
         anchors.add(CutAnchor.fixed(fallback));
      }

      return anchors;
   }

   /**
    * 极坐标圆盘均匀分布采样算法：
    * 极坐标中，径向距离必须使用 r = getRadius() * sqrt(random) 才能保证点在二维圆盘内分布密度均匀。
    */
   private static Vec3 randomPosition(ServerLevel level, ServerPlayer player, RandomSource random) {
      double radius = getRadius();
      for (int attempt = 0; attempt < RANDOM_POSITION_ATTEMPTS; attempt++) {
         double distance = radius * Math.sqrt(random.nextDouble()); // 开平方保证均匀密度
         double angle = random.nextDouble() * Math.PI * 2.0;         // 0 ~ 2π 随机角度
         double x = player.getX() + Math.cos(angle) * distance;
         double z = player.getZ() + Math.sin(angle) * distance;
         Vec3 position = findNearbyFloor(level, x, player.getY(), z);
         if (position != null) {
            return position;
         }
      }

      return null;
   }

   /**
    * 寻找 (x, baseY, z) 附近的合法脚下地面，确保次元斩切痕紧贴方块表层。
    */
   private static Vec3 findNearbyFloor(ServerLevel level, double x, double baseY, double z) {
      BlockPos origin = BlockPos.containing(x, baseY, z);
      // 先向下搜索 10 格
      for (int dy = 0; dy >= -10; dy--) {
         BlockPos floor = origin.offset(0, dy, 0);
         if (hasClearCutSpace(level, floor)) {
            return new Vec3(x, floor.getY() + 1.0, z);
         }
      }

      // 向上搜索 8 格
      for (int dy = 1; dy <= 8; dy++) {
         BlockPos floor = origin.offset(0, dy, 0);
         if (hasClearCutSpace(level, floor)) {
            return new Vec3(x, floor.getY() + 1.0, z);
         }
      }

      return null;
   }

   /**
    * 检查指定方块是否为坚固顶面 (isFaceSturdy)，且上方 3 格内没有固体方块遮挡。
    */
   private static boolean hasClearCutSpace(ServerLevel level, BlockPos floor) {
      BlockState floorState = level.getBlockState(floor);
      if (!floorState.getFluidState().isEmpty() || !floorState.isFaceSturdy(level, floor, Direction.UP)) {
         return false;
      }

      for (int height = 1; height <= 3; height++) {
         BlockPos space = floor.above(height);
         BlockState state = level.getBlockState(space);
         if (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, space).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   /**
    * 定时任务回调：在到达指定 Tick 时间时生成单个次元斩实体。
    */
   private static void spawnScheduledCut(ServerPlayer player, CutAnchor anchor, int index) {
      if (!player.isAlive() || !canUseSealedArt(player)) {
         return;
      }

      ServerLevel level = player.serverLevel();
      Vec3 position = resolveAnchor(level, player, anchor);
      if (position == null) {
         return;
      }

      spawnJudgementCut(level, player, position, index);
   }

   /**
    * 解析动态锚点：如果锁定的敌人依然存活，取其最新的位置，否则取固定的静态坐标。
    */
   private static Vec3 resolveAnchor(ServerLevel level, ServerPlayer player, CutAnchor anchor) {
      if (anchor.targetId >= 0 && level.getEntity(anchor.targetId) instanceof LivingEntity target && SlashBladeTargeting.canAttack(player, target)) {
         return target.position();
      }

      return anchor.position;
   }

   /**
    * 实例化并生成拔刀剑的 {@link EntityJudgementCut} 实体。
    */
   private static void spawnJudgementCut(ServerLevel level, ServerPlayer player, Vec3 position, int index) {
      // 创建拔刀剑注册的次元斩实体
      EntityJudgementCut cut = new EntityJudgementCut(RegistryEvents.JudgementCut, level);
      cut.setOwner(player);
      cut.setShooter(player);
      cut.setColor(NightfallDragonDefinitions.SEALED_SUMMONED_SWORD_COLOR);
      cut.setDamage(getDamage());
      cut.setLifetime(20);
      cut.setRank(getScale()); // 放大尺寸
      cut.setIsCritical(index % 3 == 0); // 每 3 次连斩触发一次强力暴击音效与震屏
      cut.setNoGravity(true);
      cut.setPos(position.x, position.y + 0.15, position.z);

      // 读取武器本身的刀色设定
      player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> cut.setColor(state.getColorCode()));
      
      // 生成实体加入世界
      level.addFreshEntity(cut);

      // 附带紫红色伽马闪电特效
      GammaThunderburstLogic.spawnBolt(level, position, 0xB026FF);

      // 粒子与声效支持
      double pScale = Math.max(0.5, getScale());
      int pCount1 = (int) Math.round(5 * pScale);
      int pCount2 = (int) Math.round(8 * pScale);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, position.x, position.y + 0.2, position.z, pCount1, 0.45 * pScale, 0.08 * pScale, 0.45 * pScale, 0.05);
      level.sendParticles(ParticleTypes.ENCHANT, position.x, position.y + 0.25, position.z, pCount2, 0.55 * pScale, 0.1 * pScale, 0.55 * pScale, 0.04);
      level.playSound(null, position.x, position.y, position.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.45F, 0.85F + index % 4 * 0.08F);
   }

   /**
    * 静态 Record：记录次元斩落点锚点（包含目标实体 ID 或固定三维 Vec3 坐标）。
    */
   private record CutAnchor(int targetId, Vec3 position) {
      private static CutAnchor target(int targetId, Vec3 position) {
         return new CutAnchor(targetId, position);
      }

      private static CutAnchor fixed(Vec3 position) {
         return new CutAnchor(-1, position);
      }
   }
}

