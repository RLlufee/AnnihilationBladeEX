package qwq.qingyi.annihilationblade.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.infinity_stellaris.entity.GammaThunderboltEntity;
import qwq.qingyi.annihilationblade.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;
import qwq.qingyi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity;

/**
 * 模组自定义实体类型注册表。
 */
public final class ModEntities {
    public static final EntityType<GammaThunderboltEntity> GAMMA_THUNDERBOLT = register(
            "gamma_thunderbolt",
            EntityType.Builder.<GammaThunderboltEntity>of(GammaThunderboltEntity::new, MobCategory.MISC)
                    .noSave()
                    .sized(0.0F, 0.0F)
                    .clientTrackingRange(16)
                    .updateInterval(Integer.MAX_VALUE)
    );

    public static final EntityType<ScaleGuardSwordEntity> SCALE_GUARD_SWORD = register(
            "scale_guard_sword",
            EntityType.Builder.<ScaleGuardSwordEntity>of(ScaleGuardSwordEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
    );

    public static final EntityType<NightfallDragonScreenShakeEntity> NIGHTFALL_SCREEN_SHAKE = register(
            "nightfall_screen_shake",
            EntityType.Builder.<NightfallDragonScreenShakeEntity>of(NightfallDragonScreenShakeEntity::new, MobCategory.MISC)
                    .noSave()
                    .sized(0.0F, 0.0F)
                    .clientTrackingRange(16)
                    .updateInterval(Integer.MAX_VALUE)
    );

    private ModEntities() {
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = AnnihilationBladeex.prefix(name);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(key));
    }

    public static void register() {
        // 触发静态初始化完成注册
    }
}
