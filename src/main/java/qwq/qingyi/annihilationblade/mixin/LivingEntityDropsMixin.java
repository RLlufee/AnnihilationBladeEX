package qwq.qingyi.annihilationblade.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qwq.qingyi.annihilationblade.event.AnnihilationFabricEvents;

/**
 * 拦截带有真空衰变无掉落标记的实体战利品掉落（{@link LivingEntity#dropAllDeathLoot}）。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDropsMixin {

   @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$cancelNoDropsLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
      LivingEntity self = (LivingEntity) (Object) this;
      if (AnnihilationFabricEvents.shouldCancelDrops(self)) {
         ci.cancel();
      }
   }
}
