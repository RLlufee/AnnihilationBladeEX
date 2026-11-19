package QWQ.QingYi.annihilationblade.render;

import QWQ.QingYi.annihilationblade.network.ModNetwork;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * 服务端发送特效事件的统一入口。
 */
public final class FxEvents {
   /** 最小 / 最大广播半径（格）。大型技能按自身半径放大广播范围，但不超过任何人可能看到的距离。 */
   private static final double MIN_BROADCAST_RADIUS = 128.0;
   private static final double MAX_BROADCAST_RADIUS = 512.0;

   private FxEvents() {
   }

   public static void send(ServerLevel level, FxEventPacket packet) {
      Vec3 origin = packet.origin();
      double radius = Math.min(MAX_BROADCAST_RADIUS, Math.max(MIN_BROADCAST_RADIUS, packet.radius() * 3.0 + 64.0));
      ModNetwork.CHANNEL.send(
         PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(origin.x, origin.y, origin.z, radius, level.dimension())),
         packet
      );
   }
}
