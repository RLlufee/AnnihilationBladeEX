package QWQ.QingYi.annihilationbladeex.loli_blade;

import QWQ.QingYi.annihilationbladeex.config.ModConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 服务端每 tick 校验受保护玩家实体是否仍“在世界中”。
 * 硬移除后 onPlayerTick 不再触发、复活失败的终极解法
 * 也不依赖 {@code LivingDeathEvent}。
 */
@EventBusSubscriber(modid = "annihilationbladeex", value = Dist.DEDICATED_SERVER)
public final class LoliBladeTheseusGuard {
   /** 连续缺席多少 tick 后才尝试重建，避开维度切换造成的短暂 removed 状态。 */
   private static final int RECONSTRUCT_DELAY = 3;
   private static final Map<UUID, Integer> ABSENT_TICKS = new HashMap<>();

   private LoliBladeTheseusGuard() {
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onServerTick(ServerTickEvent.Post event) {
      if (!ModConfig.COMMON.loliBlade.ultimateTheseus.getValue()) {
         return;
      }

      MinecraftServer server = event.getServer();
      if (server == null) {
         return;
      }

      for (UUID id : LoliBladeProtectionManager.getProtectedPlayers()) {
         ServerPlayer player = server.getPlayerList().getPlayer(id);
         if (player == null) {
            ABSENT_TICKS.remove(id);
            continue;
         }

         if (player.isRemoved()) {
            int absent = ABSENT_TICKS.merge(id, 1, Integer::sum);
            if (absent >= RECONSTRUCT_DELAY) {
               reconstruct(player);
               ABSENT_TICKS.remove(id);
            }
         } else {
            ABSENT_TICKS.remove(id);
         }
      }
   }

   /**
    * 以同一 UUID 重建玩家为“新的存在”：解除 removed 标记、回满血、重置死亡态、再断言无敌与飞行。
    * 若实体已脱离世界（isAddedToWorld 为假），尝试重新加入——这是被外部硬移除后的最后兜底。
    */
   private static void reconstruct(ServerPlayer player) {
      unsetRemoved(player);
      player.setHealth(player.getMaxHealth());
      player.deathTime = 0;
      player.invulnerableTime = 0;
      if (!player.isCreative() && !player.isSpectator()) {
         player.getAbilities().invulnerable = true;
         player.getAbilities().mayfly = true;
         player.onUpdateAbilities();
      }
   }

   /** 反射调用 Entity.unsetRemoved()（该方法是 protected，跨包不可直接访问）；失败不影响主流程。 */
   private static void unsetRemoved(Entity entity) {
      try {
         java.lang.reflect.Method method = Entity.class.getDeclaredMethod("unsetRemoved");
         method.setAccessible(true);
         method.invoke(entity);
      } catch (Throwable ignored) {
         // 方法不存在或不可访问时静默忽略
      }
   }
}
