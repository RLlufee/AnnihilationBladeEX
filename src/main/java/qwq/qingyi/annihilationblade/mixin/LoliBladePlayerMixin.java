package qwq.qingyi.annihilationblade.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeProtectionManager;

/**
 * {@link Player} 覆盖了 {@code hurtServer / actuallyHurt / die} 且 {@code actuallyHurt} 不走父类实现，
 * 需同步拦截以保障受保护玩家的方法级无敌。
 */
@Mixin(value = Player.class, priority = Integer.MAX_VALUE)
public abstract class LoliBladePlayerMixin {

   @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliPlayerHurtServer(
      ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir
   ) {
      if (annihilationblade$isProtected()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliPlayerActuallyHurt(
      ServerLevel level, DamageSource source, float amount, CallbackInfo ci
   ) {
      if (annihilationblade$isProtected()) {
         ci.cancel();
      }
   }

   @Inject(method = "die", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliPlayerDie(DamageSource source, CallbackInfo ci) {
      if (annihilationblade$isProtected()) {
         ci.cancel();
      }
   }

   private boolean annihilationblade$isProtected() {
      Player self = (Player) (Object) this;
      return ModConfig.COMMON.loliBlade.ultimateInvincible.getValue()
         && LoliBladeProtectionManager.isProtected(self.getUUID());
   }
}
