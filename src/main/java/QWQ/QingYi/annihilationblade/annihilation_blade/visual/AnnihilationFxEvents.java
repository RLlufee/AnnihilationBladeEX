package QWQ.QingYi.annihilationblade.annihilation_blade.visual;

import QWQ.QingYi.annihilationblade.render.FxEvents;
import QWQ.QingYi.annihilationblade.render.FxSustained;
import QWQ.QingYi.annihilationblade.render.FxTypes;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 湮灭之刃特效事件的服务端发送入口：只描述「发生了什么」，表现全部交给客户端渲染器。
 * <p>
 * 所有带目标的事件都必须在 {@code TerminusLogic.execute} 之前调用，客户端才能解析到尚未移除的实体。
 */
public final class AnnihilationFxEvents {
   /** 断空事件种类（intParam 低 4 位）：开场断面门。 */
   public static final int DANKONG_OPEN = 0;
   /** 断空事件种类（intParam 低 4 位）：一次闪现斩击，高位为斩序号。 */
   public static final int DANKONG_STRIKE = 1;

   private AnnihilationFxEvents() {
   }

   /**
    * 断空开场（持续特效 START）。origin = 起点胸口，direction = 视线，radius = 搜索范围（用于补发范围），scale = 视觉倍率，
    * entityIds = 本次锁定的全部目标（客户端为每个目标挂上「死兆星」）。
    *
    * @return 持续特效句柄
    */
   public static int dankongOpen(ServerLevel level, Player player, Vec3 chest, double range, double visualScale, int duration, int[] targetIds) {
      return FxSustained.start(level, new FxEventPacket(
         FxTypes.DANKONG,
         chest,
         player.getLookAngle(),
         (float)range,
         (float)visualScale,
         DANKONG_OPEN,
         level.getRandom().nextLong(),
         player.getId(),
         targetIds
      ), duration);
   }

   /**
    * 断空单次闪现（持续特效 UPDATE），必须在本次斩击伤害之前调用。
    * origin = 落点胸口，direction = 落点 − 出发点，radius = 目标体高（兜底），scale = 视觉倍率。
    */
   public static void dankongStrike(int handle, ServerLevel level, Player player, Vec3 fromChest, Vec3 toChest, LivingEntity target, int index, double visualScale) {
      FxSustained.update(handle, new FxEventPacket(
         FxTypes.DANKONG,
         toChest,
         toChest.subtract(fromChest),
         target.getBbHeight(),
         (float)visualScale,
         DANKONG_STRIKE | (index << 4),
         level.getRandom().nextLong(),
         player.getId(),
         new int[]{target.getId()}
      ));
   }

   /**
    * 裂界：伤口在 center 撕开并把 targets 拖入，必须在处决这些目标之前调用。
    *
    * @param depth    当前连锁层数（从 1 开始）
    * @param maxDepth 最大连锁层数（客户端据此决定是否画出向下一层蔓延的伤痕）
    */
   public static void worldRift(ServerLevel level, Player attacker, Vec3 center, double radius, double visualScale, int depth, int maxDepth, List<LivingEntity> targets) {
      int count = Math.min(targets.size(), FxEventPacket.MAX_ENTITIES);
      int[] ids = new int[count];
      for (int i = 0; i < count; i++) {
         ids[i] = targets.get(i).getId();
      }
      FxEvents.send(level, new FxEventPacket(
         FxTypes.WORLD_RIFT,
         center,
         Vec3.ZERO,
         (float)(radius * visualScale),
         (float)visualScale,
         (depth & 0xF) | ((maxDepth & 0xF) << 4),
         level.getRandom().nextLong(),
         attacker.getId(),
         ids
      ));
   }

   /** 断空收刀（持续特效 STOP）：客户端播放「納刀」。 */
   public static void dankongFinish(int handle) {
      FxSustained.stop(handle);
   }

   /**
    * 终焉印记：普通命中触发终焉判定时，目标身上的金色光圈收束归零。
    * radius = 目标体宽，scale = 目标体高，entityIds = [目标]。
    */
   public static void terminusMark(ServerLevel level, Player attacker, LivingEntity target) {
      Vec3 center = target.getBoundingBox().getCenter();
      FxEvents.send(level, new FxEventPacket(
         FxTypes.TERMINUS_MARK,
         center,
         Vec3.ZERO,
         target.getBbWidth(),
         target.getBbHeight(),
         0,
         level.getRandom().nextLong(),
         attacker.getId(),
         new int[]{target.getId()}
      ));
   }
}
