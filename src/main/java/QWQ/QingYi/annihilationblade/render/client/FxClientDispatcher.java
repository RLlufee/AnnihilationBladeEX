package QWQ.QingYi.annihilationblade.render.client;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.annihilation_blade.client.fx.SpatialFractureFx;
import QWQ.QingYi.annihilationblade.render.FxTypes;
import QWQ.QingYi.annihilationblade.render.client.core.FxInstance;
import QWQ.QingYi.annihilationblade.render.client.core.FxManager;
import QWQ.QingYi.annihilationblade.render.network.FxEventPacket;
import java.util.function.BiFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 特效事件 → 客户端特效实例的工厂表。新增特效时在 {@link FxTypes} 追加编号并在此注册工厂。
 */
@OnlyIn(Dist.CLIENT)
public final class FxClientDispatcher {
   private static final Int2ObjectOpenHashMap<BiFunction<ClientLevel, FxEventPacket, FxInstance>> FACTORIES = new Int2ObjectOpenHashMap<>();

   static {
      FACTORIES.put(FxTypes.SPATIAL_FRACTURE, SpatialFractureFx::new);
   }

   private FxClientDispatcher() {
   }

   public static void handle(FxEventPacket packet) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return;
      }
      BiFunction<ClientLevel, FxEventPacket, FxInstance> factory = FACTORIES.get(packet.type());
      if (factory == null) {
         Annihilationblade.LOGGER.warn("[AnnihilationBlade FX] Unknown effect type {}", packet.type());
         return;
      }
      FxManager.add(factory.apply(level, packet));
   }
}
