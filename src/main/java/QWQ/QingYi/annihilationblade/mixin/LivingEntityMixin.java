package QWQ.QingYi.annihilationblade.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 占位 Mixin：当前不注入逻辑，仅用于确保 annihilationblade.mixins.json 有实际可加载条目。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
}
