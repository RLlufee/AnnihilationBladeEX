package QWQ.QingYi.annihilationblade.registry;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.infinity_stellaris.entity.GammaThunderboltEntity;
import QWQ.QingYi.annihilationblade.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;
import QWQ.QingYi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Annihilationblade.MODID);

   public static final RegistryObject<EntityType<GammaThunderboltEntity>> GAMMA_THUNDERBOLT = ENTITY_TYPES.register(
      "gamma_thunderbolt",
      () -> EntityType.Builder.<GammaThunderboltEntity>of(GammaThunderboltEntity::new, MobCategory.MISC)
         .noSave()
         .sized(0.0F, 0.0F)
         .clientTrackingRange(16)
         .updateInterval(Integer.MAX_VALUE)
         .build("gamma_thunderbolt")
   );
   public static final RegistryObject<EntityType<ScaleGuardSwordEntity>> SCALE_GUARD_SWORD = ENTITY_TYPES.register(
      "scale_guard_sword",
      () -> EntityType.Builder.<ScaleGuardSwordEntity>of(ScaleGuardSwordEntity::new, MobCategory.MISC)
         .noSave()
         .sized(0.1F, 0.1F)
         .clientTrackingRange(96)
         .updateInterval(1)
         .setCustomClientFactory(ScaleGuardSwordEntity::createInstance)
         .build("scale_guard_sword")
   );
   public static final RegistryObject<EntityType<NightfallDragonScreenShakeEntity>> NIGHTFALL_SCREEN_SHAKE = ENTITY_TYPES.register(
      "nightfall_screen_shake",
      () -> EntityType.Builder.<NightfallDragonScreenShakeEntity>of(NightfallDragonScreenShakeEntity::new, MobCategory.MISC)
         .noSave()
         .sized(0.0F, 0.0F)
         .clientTrackingRange(32)
         .updateInterval(1)
         .setCustomClientFactory(NightfallDragonScreenShakeEntity::createInstance)
         .build("nightfall_screen_shake")
   );

   private ModEntities() {
   }
}
