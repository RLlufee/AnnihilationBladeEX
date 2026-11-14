package qwq.qingyi.annihilationblade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qwq.qingyi.annihilationblade.event.AnnihilationFabricEvents;

/**
 * {@link Player#actuallyHurt(ServerLevel, DamageSource, float)} 不调用 {@code super.actuallyHurt}，
 * 因此需同步注入 {@code Player} 以保证玩家作为受击方也能触发 {@code LivingDamagePre} 钩子。
 */
@Mixin(Player.class)
public abstract class PlayerDamageMixin {

   @WrapOperation(
      method = "actuallyHurt",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
      )
   )
   private float annihilationblade$onPlayerDamagePre(
      Player instance,
      DamageSource source,
      float amount,
      Operation<Float> original,
      @Share("annihilationbladeCancelPre") LocalBooleanRef canceledRef
   ) {
      float modified = AnnihilationFabricEvents.onLivingDamagePre(instance, source, amount);
      if (modified == Float.NEGATIVE_INFINITY) {
         canceledRef.set(true);
         return 0.0F;
      }
      return original.call(instance, source, modified);
   }

   @Inject(
      method = "actuallyHurt",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
      ),
      cancellable = true
   )
   private void annihilationblade$cancelAfterPre(
      ServerLevel level,
      DamageSource source,
      float amount,
      CallbackInfo ci,
      @Share("annihilationbladeCancelPre") LocalBooleanRef canceledRef
   ) {
      if (canceledRef.get()) {
         ci.cancel();
      }
   }
}
