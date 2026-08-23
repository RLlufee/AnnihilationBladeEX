package QWQ.QingYi.annihilationbladeex.nightfall_dragon.client;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AnnihilationBladeEX.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public final class NightfallDragonClientEvents {
   private NightfallDragonClientEvents() {
   }

   @SubscribeEvent
   public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModEntities.SCALE_GUARD_SWORD.get(), ScaleGuardControllerRenderer::new);
      event.registerEntityRenderer(ModEntities.NIGHTFALL_SCREEN_SHAKE.get(), NightfallDragonScreenShakeRenderer::new);
   }
}
