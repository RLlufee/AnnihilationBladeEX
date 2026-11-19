package QWQ.QingYi.annihilationblade.render;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.network.ModNetwork;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import QWQ.QingYi.annihilationblade.render.network.FxSustainPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;

/**
 * 持续特效的服务端权威管理（结界、领域、光环等持续数秒的效果）。
 * <p>
 * 服务端持有每个持续特效的句柄、事件数据与已运行 tick：
 * <ul>
 *   <li>开始时发送给范围内玩家；之后每 {@value #SYNC_INTERVAL} tick 检查一次，把 START（带已运行时间）补发给新进入范围的玩家。</li>
 *   <li>可随时 {@link #update} 位置/半径等参数，或 {@link #stop} 提前结束。</li>
 *   <li>到期后服务端自行移除记录；客户端按 duration 自行结束，无需额外通知。</li>
 * </ul>
 * 设计参考了 UsefulMagic（GPL-3.0，CooStack）「服务端渲染实体」的同步思路，代码为自行实现。
 */
@EventBusSubscriber(modid = Annihilationblade.MODID)
public final class FxSustained {
   private static final int SYNC_INTERVAL = 10;
   private static final double VIEW_MARGIN = 192.0;
   private static final Int2ObjectOpenHashMap<Entry> ACTIVE = new Int2ObjectOpenHashMap<>();
   private static int nextHandle = 1;

   private FxSustained() {
   }

   /**
    * 开始一个持续特效。
    *
    * @param duration 持续 tick（客户端会额外播放自己的收尾动画）
    * @return 句柄，用于后续 update / stop
    */
   public static int start(ServerLevel level, FxEventPacket event, int duration) {
      int handle = nextHandle++;
      if (nextHandle == Integer.MAX_VALUE) {
         nextHandle = 1;
      }
      Entry entry = new Entry(handle, level, event, Math.max(1, duration));
      ACTIVE.put(handle, entry);
      entry.syncViewers();
      return handle;
   }

   public static void update(int handle, FxEventPacket event) {
      Entry entry = ACTIVE.get(handle);
      if (entry == null) {
         return;
      }
      entry.event = event;
      FxSustainPacket packet = FxSustainPacket.update(handle, event);
      for (ServerPlayer player : entry.onlineViewers()) {
         ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
      }
   }

   public static void stop(int handle) {
      Entry entry = ACTIVE.remove(handle);
      if (entry == null) {
         return;
      }
      FxSustainPacket packet = FxSustainPacket.stop(handle);
      for (ServerPlayer player : entry.onlineViewers()) {
         ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
      }
   }

   public static boolean isActive(int handle) {
      return ACTIVE.containsKey(handle);
   }

   @SubscribeEvent
   public static void onServerTick(TickEvent.ServerTickEvent event) {
      if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
         return;
      }
      Iterator<Entry> iterator = ACTIVE.values().iterator();
      while (iterator.hasNext()) {
         Entry entry = iterator.next();
         entry.age++;
         if (entry.age >= entry.duration) {
            iterator.remove();
         } else if (entry.age % SYNC_INTERVAL == 0) {
            entry.syncViewers();
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
      UUID id = event.getEntity().getUUID();
      for (Entry entry : ACTIVE.values()) {
         entry.viewers.remove(id);
      }
   }

   @SubscribeEvent
   public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
      UUID id = event.getEntity().getUUID();
      for (Entry entry : ACTIVE.values()) {
         entry.viewers.remove(id);
      }
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent event) {
      ACTIVE.clear();
   }

   private static final class Entry {
      final int handle;
      final ServerLevel level;
      final int duration;
      final Set<UUID> viewers = new HashSet<>();
      FxEventPacket event;
      int age;

      Entry(int handle, ServerLevel level, FxEventPacket event, int duration) {
         this.handle = handle;
         this.level = level;
         this.event = event;
         this.duration = duration;
      }

      double viewRange() {
         return Math.max(this.event.radius(), this.event.scale()) + VIEW_MARGIN;
      }

      /** 给范围内尚未收到 START 的玩家补发（带已运行时间）。 */
      void syncViewers() {
         Vec3 origin = this.event.origin();
         double range = this.viewRange();
         double rangeSqr = range * range;
         for (ServerPlayer player : this.level.players()) {
            if (this.viewers.contains(player.getUUID())) {
               continue;
            }
            double dx = player.getX() - origin.x;
            double dz = player.getZ() - origin.z;
            if (dx * dx + dz * dz <= rangeSqr) {
               this.viewers.add(player.getUUID());
               ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), FxSustainPacket.start(this.handle, this.age, this.duration, this.event));
            }
         }
      }

      Iterable<ServerPlayer> onlineViewers() {
         Set<ServerPlayer> players = new HashSet<>();
         for (UUID id : this.viewers) {
            ServerPlayer player = this.level.getServer().getPlayerList().getPlayer(id);
            if (player != null && player.level() == this.level) {
               players.add(player);
            }
         }
         return players;
      }
   }
}
