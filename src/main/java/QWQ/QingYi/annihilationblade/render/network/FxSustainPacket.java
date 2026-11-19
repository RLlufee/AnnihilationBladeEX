package QWQ.QingYi.annihilationblade.render.network;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * 持续特效通道（服务端 → 客户端）。
 * <ul>
 *   <li>{@link #START}：创建句柄为 handle 的持续特效；elapsed 为已经过的 tick（中途进入范围的玩家会跳过已播放部分）。</li>
 *   <li>{@link #UPDATE}：用新的 event 数据（位置/半径/参数）更新已有特效。</li>
 *   <li>{@link #STOP}：服务端提前结束（例如施法者死亡），客户端进入收尾淡出。</li>
 * </ul>
 * 与 {@link FxEventPacket} 相同，本类会在专用服务端加载，禁止引用客户端类。
 */
public record FxSustainPacket(int action, int handle, int elapsed, int duration, @Nullable FxEventPacket event) {
   public static final int START = 0;
   public static final int UPDATE = 1;
   public static final int STOP = 2;

   public static FxSustainPacket start(int handle, int elapsed, int duration, FxEventPacket event) {
      return new FxSustainPacket(START, handle, elapsed, duration, event);
   }

   public static FxSustainPacket update(int handle, FxEventPacket event) {
      return new FxSustainPacket(UPDATE, handle, 0, 0, event);
   }

   public static FxSustainPacket stop(int handle) {
      return new FxSustainPacket(STOP, handle, 0, 0, null);
   }

   public static void encode(FxSustainPacket packet, FriendlyByteBuf buffer) {
      buffer.writeVarInt(packet.action);
      buffer.writeVarInt(packet.handle);
      buffer.writeVarInt(packet.elapsed);
      buffer.writeVarInt(packet.duration);
      buffer.writeBoolean(packet.event != null);
      if (packet.event != null) {
         FxEventPacket.encode(packet.event, buffer);
      }
   }

   public static FxSustainPacket decode(FriendlyByteBuf buffer) {
      int action = buffer.readVarInt();
      int handle = buffer.readVarInt();
      int elapsed = buffer.readVarInt();
      int duration = buffer.readVarInt();
      FxEventPacket event = buffer.readBoolean() ? FxEventPacket.decode(buffer) : null;
      return new FxSustainPacket(action, handle, elapsed, duration, event);
   }

   public static void handle(FxSustainPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FxClientBridge.dispatchSustain(packet)));
      ctx.setPacketHandled(true);
   }
}
