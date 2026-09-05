package QWQ.QingYi.annihilationblade.loli_blade;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 受保护玩家名册：登记当前“持有萝莉之刃且开启防御”的玩家 UUID。
 * <p>
 * 由 {@link LoliBladeDefenseLogic#onPlayerTick} 每 tick 增量维护，
 * 供方法级 Mixin（{@code LoliBladeLivingEntityMixin} / {@code LoliBladeEntityMixin}）
 * 与 {@link LoliBladeTheseusGuard} 在字节码层 / 服务端 tick 层快速判定，
 * 避免在高频拦截路径上反复扫描玩家背包。
 */
public final class LoliBladeProtectionManager {
   private static final Set<UUID> PROTECTED = new HashSet<>();

   private LoliBladeProtectionManager() {
   }

   /** 登记一名受保护玩家。 */
   public static void markProtected(UUID id) {
      if (id != null) {
         PROTECTED.add(id);
      }
   }

   /** 取消一名玩家的受保护状态。 */
   public static void unmark(UUID id) {
      if (id != null) {
         PROTECTED.remove(id);
      }
   }

   /** 该玩家当前是否处于受保护状态。 */
   public static boolean isProtected(UUID id) {
      return id != null && PROTECTED.contains(id);
   }

   /** 返回当前所有受保护玩家 UUID 的只读副本，便于遍历时安全改动名册。 */
   public static Set<UUID> getProtectedPlayers() {
      return new HashSet<>(PROTECTED);
   }
}
