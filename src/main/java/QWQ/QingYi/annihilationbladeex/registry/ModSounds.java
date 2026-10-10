package QWQ.QingYi.annihilationbladeex.registry;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, AnnihilationBladeEX.MODID);
   public static final DeferredHolder<SoundEvent, SoundEvent> LOLI_SUCCESS = SOUND_EVENTS.register(
      "loli_success",
      () -> SoundEvent.createVariableRangeEvent(AnnihilationBladeEX.prefix("loli_success"))
   );

   private ModSounds() {
   }

   public static void register(IEventBus eventBus) {
      SOUND_EVENTS.register(eventBus);
   }
}
