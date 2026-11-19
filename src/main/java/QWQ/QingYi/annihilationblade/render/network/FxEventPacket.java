package QWQ.QingYi.annihilationblade.render.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * 服务端 → 客户端的通用特效事件包。
 * <p>
 * 设计目标：服务端只描述「发生了什么」（特效类型、位置、朝向、尺寸、随机种子、相关实体），
 * 由客户端专属渲染器负责全部视觉表现。一次技能只发一个包，替代以往逐点 sendParticles 的数百个粒子包。
 * <p>
 * 注意：本类会在专用服务端加载，禁止引用任何客户端类；客户端逻辑通过 {@link DistExecutor} 间接调用。
 */
public record FxEventPacket(
   int type,
   Vec3 origin,
   Vec3 direction,
   float radius,
   float scale,
   int intParam,
   long seed,
   int casterId,
   int[] entityIds
) {
   /** 单包最多携带的关联实体数，防止异常数据撑爆缓冲区。 */
   public static final int MAX_ENTITIES = 256;

   public static void encode(FxEventPacket packet, FriendlyByteBuf buffer) {
      buffer.writeVarInt(packet.type);
      writeVec(buffer, packet.origin);
      writeVec(buffer, packet.direction);
      buffer.writeFloat(packet.radius);
      buffer.writeFloat(packet.scale);
      buffer.writeVarInt(packet.intParam);
      buffer.writeLong(packet.seed);
      buffer.writeVarInt(packet.casterId);
      int count = Math.min(MAX_ENTITIES, packet.entityIds == null ? 0 : packet.entityIds.length);
      buffer.writeVarInt(count);
      for (int i = 0; i < count; i++) {
         buffer.writeVarInt(packet.entityIds[i]);
      }
   }

   public static FxEventPacket decode(FriendlyByteBuf buffer) {
      int type = buffer.readVarInt();
      Vec3 origin = readVec(buffer);
      Vec3 direction = readVec(buffer);
      float radius = buffer.readFloat();
      float scale = buffer.readFloat();
      int intParam = buffer.readVarInt();
      long seed = buffer.readLong();
      int casterId = buffer.readVarInt();
      int count = Math.min(MAX_ENTITIES, Math.max(0, buffer.readVarInt()));
      int[] ids = new int[count];
      for (int i = 0; i < count; i++) {
         ids[i] = buffer.readVarInt();
      }
      return new FxEventPacket(type, origin, direction, radius, scale, intParam, seed, casterId, ids);
   }

   public static void handle(FxEventPacket packet, Supplier<Context> context) {
      Context ctx = context.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FxClientBridge.dispatch(packet)));
      ctx.setPacketHandled(true);
   }

   private static void writeVec(FriendlyByteBuf buffer, Vec3 vec) {
      buffer.writeDouble(vec.x);
      buffer.writeDouble(vec.y);
      buffer.writeDouble(vec.z);
   }

   private static Vec3 readVec(FriendlyByteBuf buffer) {
      return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
   }
}
