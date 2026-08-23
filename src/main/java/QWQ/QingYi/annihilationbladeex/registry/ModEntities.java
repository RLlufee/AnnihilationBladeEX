package QWQ.QingYi.annihilationbladeex.registry;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.entity.GammaThunderboltEntity;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.entity.ScaleGuardSwordEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, AnnihilationBladeEX.MODID);

   public static final DeferredHolder<EntityType<?>, EntityType<GammaThunderboltEntity>> GAMMA_THUNDERBOLT = ENTITY_TYPES.register(
      "gamma_thunderbolt",
      () -> EntityType.Builder.<GammaThunderboltEntity>of(GammaThunderboltEntity::new, MobCategory.MISC)
         .noSave()
         .sized(0.0F, 0.0F)
         .clientTrackingRange(16)
         .updateInterval(Integer.MAX_VALUE)
         .build("gamma_thunderbolt")
   );

   public static final DeferredHolder<EntityType<?>, EntityType<ScaleGuardSwordEntity>> SCALE_GUARD_SWORD = ENTITY_TYPES.register(
      "scale_guard_sword",
      () -> EntityType.Builder.<ScaleGuardSwordEntity>of(ScaleGuardSwordEntity::new, MobCategory.MISC)
         .sized(0.5F, 0.5F)
         .clientTrackingRange(16)
         .updateInterval(1)
         .build("scale_guard_sword")
   );

   public static final DeferredHolder<EntityType<?>, EntityType<NightfallDragonScreenShakeEntity>> NIGHTFALL_SCREEN_SHAKE = ENTITY_TYPES.register(
      "nightfall_screen_shake",
      () -> EntityType.Builder.<NightfallDragonScreenShakeEntity>of(NightfallDragonScreenShakeEntity::new, MobCategory.MISC)
         .noSave()
         .sized(0.0F, 0.0F)
         .clientTrackingRange(16)
         .updateInterval(Integer.MAX_VALUE)
         .build("nightfall_screen_shake")
   );

   private ModEntities() {
   }
}
