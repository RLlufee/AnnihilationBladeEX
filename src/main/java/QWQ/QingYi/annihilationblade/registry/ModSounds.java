package QWQ.QingYi.annihilationblade.registry;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Annihilationblade.MODID);
   public static final RegistryObject<SoundEvent> LOLI_SUCCESS = SOUND_EVENTS.register(
      "loli_success", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Annihilationblade.MODID, "loli_success"))
   );

   public static void register(IEventBus eventBus) {
      SOUND_EVENTS.register(eventBus);
   }
}
