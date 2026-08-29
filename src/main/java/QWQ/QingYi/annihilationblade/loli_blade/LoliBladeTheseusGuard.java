package QWQ.QingYi.annihilationblade.loli_blade;

import QWQ.QingYi.annihilationblade.config.ModConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 服务端每 tick 校验受保护玩家实体是否仍“在世界中”。
 * 硬移除后 onPlayerTick 不再触发、复活失败的终极解法
 * 也不依赖 {@code LivingDeathEvent}。
 */
@Mod.EventBusSubscriber(modid = "annihilationblade", value = Dist.DEDICATED_SERVER)
public final class LoliBladeTheseusGuard {
   /** 连续缺席多少 tick 后才尝试重建，避开维度切换造成的短暂 removed 状态。 */
   private static final int RECONSTRUCT_DELAY = 3;
   private static final Map<UUID, Integer> ABSENT_TICKS = new HashMap<>();

   private LoliBladeTheseusGuard() {
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void onServerTick(TickEvent.ServerTickEvent event) {
      if (event.phase != TickEvent.Phase.END || !ModConfig.COMMON.loliBlade.ultimateTheseus.getValue()) {
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

         if (player.isRemoved() || !player.isAddedToWorld()) {
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
      if (!player.isAddedToWorld() && player.level() instanceof ServerLevel level) {
         try {
            level.addFreshEntity(player);
         } catch (Throwable ignored) {
            // 玩家实体由 PlayerList/连接层管理，addFreshEntity 可能重复加入；
            // 失败则交由连接层恢复，不抛异常以免污染服务端 tick。
         }
      }
   }

   private static final java.lang.reflect.Method UNSET_REMOVED_METHOD;

   static {
      java.lang.reflect.Method m = null;
      try {
         m = Entity.class.getDeclaredMethod("unsetRemoved");
         m.setAccessible(true);
      } catch (Throwable ignored) {
      }
      UNSET_REMOVED_METHOD = m;
   }

   /** 使用静态缓存的方法引用调用 Entity.unsetRemoved()；消除运行时反射查找开销。 */
   private static void unsetRemoved(Entity entity) {
      if (UNSET_REMOVED_METHOD != null) {
         try {
            UNSET_REMOVED_METHOD.invoke(entity);
         } catch (Throwable ignored) {
         }
      }
   }
}
