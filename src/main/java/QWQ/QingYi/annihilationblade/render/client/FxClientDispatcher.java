package QWQ.QingYi.annihilationblade.render.client;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.annihilation_blade.client.fx.DankongFx;
import QWQ.QingYi.annihilationblade.annihilation_blade.client.fx.SpatialFractureFx;
import QWQ.QingYi.annihilationblade.annihilation_blade.client.fx.TerminusMarkPool;
import QWQ.QingYi.annihilationblade.annihilation_blade.client.fx.WorldRiftFx;
import QWQ.QingYi.annihilationblade.blood_prison.client.fx.BloodBurstFx;
import QWQ.QingYi.annihilationblade.blood_prison.client.fx.BloodDashFx;
import QWQ.QingYi.annihilationblade.blood_prison.client.fx.BloodDomainFx;
import QWQ.QingYi.annihilationblade.infinity_stellaris.client.fx.VacuumDecayFx;
import QWQ.QingYi.annihilationblade.loli_blade.client.fx.LoliJudgementFx;
import QWQ.QingYi.annihilationblade.nightfall_dragon.client.fx.CosmicDescentFx;
import QWQ.QingYi.annihilationblade.nightfall_dragon.client.fx.JudgementCutFx;
import QWQ.QingYi.annihilationblade.nightfall_dragon.client.fx.JudgementDomainFx;
import QWQ.QingYi.annihilationblade.nightfall_dragon.client.fx.ScaleGuardFx;
import QWQ.QingYi.annihilationblade.render.network.FxSustainPacket;
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
      FACTORIES.put(FxTypes.VACUUM_DECAY, VacuumDecayFx::new);
      FACTORIES.put(FxTypes.COSMIC_DESCENT, CosmicDescentFx::new);
      FACTORIES.put(FxTypes.BLOOD_DOMAIN, BloodDomainFx::new);
      FACTORIES.put(FxTypes.BLOOD_DASH, BloodDashFx::new);
      FACTORIES.put(FxTypes.BLOOD_BURST, BloodBurstFx::new);
      FACTORIES.put(FxTypes.JUDGEMENT_DOMAIN, JudgementDomainFx::new);
      FACTORIES.put(FxTypes.JUDGEMENT_CUT, JudgementCutFx::new);
      FACTORIES.put(FxTypes.SCALE_GUARD, ScaleGuardFx::new);
      FACTORIES.put(FxTypes.LOLI_JUDGEMENT, LoliJudgementFx::new);
      // 池化特效：工厂把事件并入已有实例并返回 null，不新建实例
      FACTORIES.put(FxTypes.TERMINUS_MARK, TerminusMarkPool::accept);
      FACTORIES.put(FxTypes.DANKONG, DankongFx::new);
      FACTORIES.put(FxTypes.WORLD_RIFT, WorldRiftFx::new);
   }

   private FxClientDispatcher() {
   }

   public static void handle(FxEventPacket packet) {
      FxInstance instance = create(packet);
      if (instance != null) {
         FxManager.add(instance);
      }
   }

   public static void handleSustain(FxSustainPacket packet) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return;
      }
      switch (packet.action()) {
         case FxSustainPacket.START -> {
            if (packet.event() == null) {
               return;
            }
            FxInstance instance = create(packet.event());
            if (instance != null) {
               instance.bindSustained(packet.handle(), packet.elapsed(), packet.duration());
               FxManager.add(instance);
            }
         }
         case FxSustainPacket.UPDATE -> {
            FxInstance instance = FxManager.sustained(packet.handle());
            if (instance != null && packet.event() != null) {
               instance.onSustainedUpdate(level, packet.event());
            }
         }
         case FxSustainPacket.STOP -> {
            FxInstance instance = FxManager.sustained(packet.handle());
            if (instance != null) {
               instance.onSustainedStop();
            }
         }
         default -> Annihilationblade.LOGGER.warn("[AnnihilationBlade FX] Unknown sustain action {}", packet.action());
      }
   }

   private static FxInstance create(FxEventPacket packet) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      }
      BiFunction<ClientLevel, FxEventPacket, FxInstance> factory = FACTORIES.get(packet.type());
      if (factory == null) {
         Annihilationblade.LOGGER.warn("[AnnihilationBlade FX] Unknown effect type {}", packet.type());
         return null;
      }
      return factory.apply(level, packet);
   }
}
